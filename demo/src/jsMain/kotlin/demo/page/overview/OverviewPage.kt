package demo.page.overview

import io.github.shijianjs.ktjs.demo.page.DemoPage
import demo.API_URL
import demo.DOCS_URL
import demo.SOURCE_URL
import io.github.shijianjs.ktjs.demo.i18n.Binder
import io.github.shijianjs.ktjs.demo.i18n.Lang
import io.github.shijianjs.ktjs.demo.i18n.Langs
import io.github.shijianjs.ktjs.demo.i18n.Translations
import io.github.shijianjs.ktjs.demo.i18n.texts
import io.github.shijianjs.ktjs.demo.page.Blocks.anchor
import io.github.shijianjs.ktjs.demo.page.Blocks.codeText
import io.github.shijianjs.ktjs.demo.page.Blocks.paragraph
import io.github.shijianjs.ktjs.demo.page.Blocks.row
import io.github.shijianjs.ktjs.demo.page.SectionBlock
import io.github.shijianjs.ktjs.dom.element.invoke
import io.github.shijianjs.ktjs.dom.element.unaryPlus
import io.github.shijianjs.ktjs.dom.webcomponent.CustomElementReferenceAdapter
import web.html.HtmlTagName

private val overviewEnglish: OverviewTexts = OverviewTexts(
    title = "Overview",
    summary = "A Kotlin/JS wrapper for mermaid: typed configuration, a promise-based render, " +
        "and the diagram source as plain text.",
    docsLink = "Documentation",
    sourceLink = "Source repository",
    apiLink = "API reference",
    purposeHeading = "What it wraps",
    body = "mermaid renders diagrams from text. In Kotlin the configuration object would be a " +
        "`JsAny` literal and every promise would be `dynamic`; this wrapper declares both, so the " +
        "render result has a type, a syntax error is a value (`parseOrNull`), and a misspelled " +
        "configuration key fails at compile time.",
    bullets = listOf(
        "MermaidConfig / MermaidConfigTheme — the option object handed to `initialize`.",
        "render(id, text, element) — returns `RenderResult` with `svg` and `bindFunctions`.",
        "parse / parseOrNull — the syntax check, without throwing on a broken diagram.",
    ),
    coordinatesHeading = "Coordinates",
    coordinatesNote = "The demo next to this site is built with the library itself, so every page " +
        "doubles as an integration test.",
    nextHeading = "Reading order",
    nextBody = "The overview, then the playground: pick a preset, edit the source, render it, and " +
        "switch the theme.",
)

private val overviewChinese: OverviewTexts = OverviewTexts(
    title = "概览",
    summary = "mermaid 的 Kotlin/JS 封装：类型化的配置、基于 Promise 的渲染，图源就是纯文本。",
    docsLink = "使用文档",
    sourceLink = "源码仓库",
    apiLink = "API 文档",
    purposeHeading = "它封装了什么",
    body = "mermaid 从文本渲染图形。在 Kotlin 里配置对象只能是 `JsAny` 字面量、每个 Promise 都是 " +
        "`dynamic`；这个封装把两者都声明出来：渲染结果有类型，语法错误是一个值（`parseOrNull`），" +
        "配置键拼错在编译期就报错。",
    bullets = listOf(
        "MermaidConfig / MermaidConfigTheme —— 交给 `initialize` 的选项对象。",
        "render(id, text, element) —— 返回带 `svg` 与 `bindFunctions` 的 `RenderResult`。",
        "parse / parseOrNull —— 语法检查，图坏了也不抛异常。",
    ),
    coordinatesHeading = "坐标",
    coordinatesNote = "这个站点旁边的 demo 本身就用这个库搭建，每页同时充当它的集成测试。",
    nextHeading = "建议顺序",
    nextBody = "先看概览，再看演练场：选一个预设、改图源、渲染、换主题。",
)

private val overviewTexts: Translations<OverviewTexts> = texts(
    Langs.EN to overviewEnglish,
    Langs.ZH_HANS to overviewChinese,
)

/**
 * Landing page: what the library wraps, how it is consumed, where the reference lives.
 *
 * The sections are plain blocks, not components: they have nothing to swap at run time, so a component
 * per section would only add indirection.
 */
internal class OverviewPage : DemoPage() {

    private val texts = Binder(overviewTexts)

    init {
        addLink(texts.bind(anchor("", DOCS_URL, external = true), { it.docsLink }))
        addLink(texts.bind(anchor("", API_URL, external = true), { it.apiLink }))
        addLink(texts.bind(anchor("", SOURCE_URL, external = true), { it.sourceLink }))

        val purpose = SectionBlock.NAME { show() }
        texts.bind(purpose.heading, { it.purposeHeading })
        val bullets = HtmlTagName.ul {
            List(3) { index -> texts.bind(+HtmlTagName.li()) { it.bullets.getOrNull(index).orEmpty() } }
        }
        purpose.body.append(texts.bind(paragraph(), { it.body }), bullets)

        val coordinates = SectionBlock.NAME { show() }
        texts.bind(coordinates.heading, { it.coordinatesHeading })
        texts.bind(coordinates.hint, { it.coordinatesNote })
        coordinates.body.append(
            row(codeText("implementation(\"io.github.shijianjs:ktjs-mermaid:12.3.0-kt.1\")")),
            row(texts.bind(anchor("", SOURCE_URL, external = true), { it.sourceLink })),
        )

        val next = SectionBlock.NAME { show() }
        texts.bind(next.heading, { it.nextHeading })
        next.body.append(texts.bind(paragraph(), { it.nextBody }))

        body.append(purpose.root, coordinates.root, next.root)
    }

    override fun applyTranslation(lang: Lang) {
        val current = overviewTexts[lang]
        setTitle(current.title)
        setSummary(current.summary)
        texts.apply(lang)
    }

    companion object : CustomElementReferenceAdapter<OverviewPage>(OverviewPage::class.js, "mermaid-overview-page")
}
