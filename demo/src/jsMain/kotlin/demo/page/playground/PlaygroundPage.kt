package demo.page.playground

import io.github.shijianjs.ktjs.demo.page.DemoPage
import io.github.shijianjs.ktjs.dom.webcomponent.defineIfMissing
import web.components.CustomElementReference
import web.dom.TagName
import io.github.shijianjs.ktjs.demo.i18n.Binder
import io.github.shijianjs.ktjs.demo.i18n.Lang
import io.github.shijianjs.ktjs.demo.i18n.Langs
import io.github.shijianjs.ktjs.demo.i18n.Translations
import io.github.shijianjs.ktjs.demo.i18n.texts
import io.github.shijianjs.ktjs.demo.page.row
import io.github.shijianjs.ktjs.demo.page.sectionBlock
import io.github.shijianjs.ktjs.demo.style.Palette
import io.github.shijianjs.ktjs.dom.element.inlineStyle
import io.github.shijianjs.ktjs.dom.element.invoke
import io.github.shijianjs.ktjs.dom.element.unaryPlus
import io.github.shijianjs.ktjs.mermaid.mermaid
import io.github.shijianjs.ktjs.mermaid.parseOrNull
import io.github.shijianjs.ktjs.mermaid.config.MermaidConfig
import io.github.shijianjs.ktjs.mermaid.config.MermaidConfigTheme
import io.github.shijianjs.ktjs.vaadin.button.ButtonElement
import io.github.shijianjs.ktjs.vaadin.button.createButton
import js.objects.unsafeJso
import web.animations.requestAnimationFrame
import web.cssom.Border
import web.cssom.FontFamily
import web.cssom.FontSize
import web.cssom.LineStyle
import web.cssom.Overflow
import web.cssom.Padding
import web.cssom.Position
import web.cssom.pct
import web.cssom.px
import web.dom.clickEvent
import web.dom.document
import web.events.EventHandler
import web.events.addHandler
import web.html.HTMLSelectElement
import web.html.HTMLTextAreaElement
import web.html.HTMLElement
import web.html.HtmlSource
import web.html.HtmlTagName

/** The diagrams the playground offers: the source each preset writes into the editor. */
private val PRESET_SOURCES: List<String> = listOf(
    """
        graph TD
            A[call mermaid.render] --> B{does it parse?}
            B -->|yes| C[insert the svg]
            B -->|no| D[show the error]
            C --> E[bindFunctions]
    """.trimIndent(),
    """
        sequenceDiagram
            participant K as Kotlin
            participant M as mermaid
            K->>M: render(id, text)
            M-->>K: RenderResult(svg, bindFunctions)
    """.trimIndent(),
    """
        classDiagram
            class Mermaid {
                +initialize(config)
                +render(id, text) Promise
                +parse(text) Promise
            }
            class RenderResult {
                +svg
                +diagramType
            }
            Mermaid --> RenderResult
    """.trimIndent(),
    """
        stateDiagram-v2
            [*] --> Idle
            Idle --> Parsing: press Render
            Parsing --> Rendered: the syntax passes
            Parsing --> Broken: the syntax fails
            Rendered --> [*]
    """.trimIndent(),
    """
        gantt
            title wrapper schedule
            dateFormat YYYY-MM-DD
            section Declarations
            generate the config types :done, a1, 2026-11-10, 3d
            hand-write the entry points :active, a2, after a1, 2d
            section Verification
            browser tests :a3, after a2, 2d
    """.trimIndent(),
    """
        pie title diagram types in use
            "flowchart" : 40
            "sequence" : 30
            "other" : 30
    """.trimIndent(),
)

/** The themes the selector offers; `mermaid` validates the value itself. */
private val THEMES: List<String> = listOf("default", "dark", "forest", "neutral", "base", "neo")

/** Every string this page shows, in one language. */
private class PlaygroundTexts(
    val title: String,
    val summary: String,
    val editorHeading: String,
    val editorHint: String,
    val presetLabels: List<String>,
    val themeLabel: String,
    val render: String,
    val idle: String,
    val parsing: String,
    val syntaxError: String,
    val unknownDiagram: String,
    val renderFailed: String,
    val renderedPrefix: String,
)

private val playgroundEnglish: PlaygroundTexts = PlaygroundTexts(
    title = "Playground",
    summary = "One editor and one canvas: a preset fills the source, Render hands it to mermaid and " +
        "the result lands on the canvas — the same three steps a consumer performs.",
    editorHeading = "Source and result",
    editorHint = "Pick a preset, edit the text, then press Render. Switching the theme re-renders " +
        "the same source.",
    presetLabels = listOf("Flowchart", "Sequence", "Class", "State", "Gantt", "Pie"),
    themeLabel = "Theme",
    render = "Render",
    idle = "Not rendered yet",
    parsing = "Parsing…",
    syntaxError = "Syntax error: mermaid cannot parse this text",
    unknownDiagram = "Unknown diagram type — check the first line",
    renderFailed = "Render failed — see the console",
    renderedPrefix = "Rendered: ",
)

private val playgroundChinese: PlaygroundTexts = PlaygroundTexts(
    title = "演练场",
    summary = "一个编辑器加一块画布：预设填入图源，点「渲染」交给 mermaid，结果画在画布上——" +
        "就是消费方的三步。",
    editorHeading = "图源与结果",
    editorHint = "选一个预设、改文本、再点「渲染」。换主题会用同一段图源重新渲染。",
    presetLabels = listOf("流程图", "时序图", "类图", "状态图", "甘特图", "饼图"),
    themeLabel = "主题",
    render = "渲染",
    idle = "尚未渲染",
    parsing = "解析中…",
    syntaxError = "语法错误：mermaid 无法解析这段文本",
    unknownDiagram = "无法识别的图型，请检查第一行",
    renderFailed = "渲染失败，见控制台",
    renderedPrefix = "已渲染：",
)

private val playgroundTexts: Translations<PlaygroundTexts> = texts(
    Langs.EN to playgroundEnglish,
    Langs.ZH_HANS to playgroundChinese,
)

/**
 * The playground: an editor, a preset row, a theme selector and a canvas.
 *
 * Nodes are fields and the renderer is called in place; a language switch only rewrites the labels,
 * so the diagram already on the canvas survives it.
 */
internal class PlaygroundPage : DemoPage() {

    private val texts = Binder(playgroundTexts)

    /** The strings the status line reads; kept in step with the language switch. */
    private var current: PlaygroundTexts = playgroundEnglish

    private var renders = 0

    /**
     * The element mermaid measures in. It has to live in the light DOM: mermaid looks its temporary
     * container up with `document.getElementById`, which does not see into a shadow root, and the
     * demo page sits inside the shell`s — rendering against the canvas fails with
     * `Cannot read properties of null (reading `getAttribute`)`. Off-screen, so nothing flashes.
     */
    private val staging: HTMLElement = HtmlTagName.div {
        inlineStyle.apply {
            position = Position.absolute
            left = (-10000).px
            top = 0.px
        }
    }

    private val canvas: HTMLElement = HtmlTagName.div {
        setAttribute("data-canvas", "")
        inlineStyle.apply {
            minHeight = 220.px
            padding = Padding(16.px, 16.px)
            border = Border(1.px, LineStyle.solid, Palette.border)
            borderRadius = 12.px
            overflow = Overflow.hidden
        }
    }

    /** The status line, marked for the end-to-end suite: its text is the renderer's state. */
    private val status: HTMLElement = HtmlTagName.p { setAttribute("data-status", "") }

    private val editor: HTMLTextAreaElement = HtmlTagName.textarea {
        setAttribute("data-editor", "")
        value = PRESET_SOURCES.first()
        inlineStyle.apply {
            width = 100.pct
            minHeight = 160.px
            padding = Padding(12.px, 12.px)
            fontFamily = FontFamily.monospace
            fontSize = 13.px
            border = Border(1.px, LineStyle.solid, Palette.border)
            borderRadius = 8.px
        }
    }

    private val themeSelect: HTMLSelectElement = HtmlTagName.select {
        THEMES.forEach { theme ->
            append(HtmlTagName.option { value = theme; textContent = theme })
        }
    }

    private val renderButton: ButtonElement = createButton()

    private val presetButtons: List<ButtonElement> = PRESET_SOURCES.mapIndexed { index, source ->
        createButton().also { button ->
            texts.bind(button, { it.presetLabels.getOrNull(index).orEmpty() })
            button.clickEvent.addHandler {
                editor.value = source
                renderNow()
            }
        }
    }


    init {
        texts.bind(renderButton, { it.render })
        status.textContent = current.idle

        val section = sectionBlock()
        texts.bind(section.title, { it.editorHeading })
        texts.bind(section.hint, { it.editorHint })
        val themeLabel = texts.bind(HtmlTagName.label(), { it.themeLabel + ":" })
        section.body.append(
            row(*presetButtons.toTypedArray()),
            row(themeLabel, themeSelect, renderButton),
            editor,
            status,
            canvas,
        )

        body.append(section.root)

        renderButton.clickEvent.addHandler { renderNow() }
        // A native `<select>`: its value is exactly the theme name mermaid takes.
        themeSelect.onchange = EventHandler {
            applyTheme(themeSelect.value)
            renderNow()
        }

        // The staging element has to be in the light DOM before the first render; see [staging].
        document.body.append(staging)
        applyTheme(themeSelect.value)
        // `mermaid.render` measures through the element, so the first render waits until the router
        // has mounted the page: the initialiser runs before the node is in the document.
        renderOnceMounted()
    }

    /** Renders the initial diagram as soon as the canvas is in the document. */
    private fun renderOnceMounted() {
        if (canvas.isConnected) {
            renderNow()
            return
        }
        requestAnimationFrame { _ -> renderOnceMounted() }
    }

    /** Renders the editor's text into [canvas]; ids are unique per call, mermaid keys the svg by them. */
    private fun renderNow() {
        status.textContent = current.parsing
        val text = editor.value
        mermaid.parseOrNull(text, unsafeJso { suppressErrors = true }).then(
            { parsed ->
                if (parsed == null) {
                    status.textContent = current.syntaxError
                } else {
                    renders += 1
                    mermaid.render("ktjs-mermaid-demo-$renders", text, staging).then(
                        { result ->
                            canvas.innerHTML = HtmlSource(result.svg)
                            result.bindFunctions?.invoke(canvas)
                            status.textContent = current.renderedPrefix + result.diagramType
                            Unit
                        },
                        {
                            status.textContent = current.renderFailed
                            Unit
                        },
                    )
                }
                Unit
            },
            {
                status.textContent = current.unknownDiagram
                Unit
            },
        )
    }

    private fun applyTheme(theme: String) {
        mermaid.initialize(
            unsafeJso<MermaidConfig> {
                startOnLoad = false
                this.theme = MermaidConfigTheme(theme)
            },
        )
    }

    override fun applyTranslation(lang: Lang) {
        current = playgroundTexts[lang]
        setTitle(current.title)
        setSummary(current.summary)
        texts.apply(lang)
    }

    companion object : CustomElementReference<PlaygroundPage>(PlaygroundPage::class.js) {
        val NAME: TagName<PlaygroundPage> = TagName("mermaid-playground-page")

        fun define() {
            defineIfMissing(NAME, this)
        }
    }
}

/** Creates a defined `<mermaid-playground-page>`. */
internal fun newPlaygroundPage(): PlaygroundPage {
    PlaygroundPage.define()
    return PlaygroundPage.NAME()
}
