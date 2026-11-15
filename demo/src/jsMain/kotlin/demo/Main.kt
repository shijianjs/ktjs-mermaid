package demo

import io.github.shijianjs.ktjs.mermaid.mermaid
import io.github.shijianjs.ktjs.mermaid.parseOrNull
import io.github.shijianjs.ktjs.mermaid.config.MermaidConfig
import io.github.shijianjs.ktjs.mermaid.config.MermaidConfigTheme
import js.objects.unsafeJso
import web.cssom.ClassName
import web.dom.document
import web.events.EventHandler
import web.html.HtmlSource
import web.html.HtmlTagName
import web.html.HTMLElement

private const val PROJECT_NAME = "ktjs-mermaid"

/** Diagram sources the preset buttons install, keyed by the label shown on the button. */
private val PRESETS = listOf(
    "流程图" to """
        graph TD
            A[调用 mermaid.render] --> B{解析通过?}
            B -->|是| C[插入 svg]
            B -->|否| D[显示错误]
            C --> E[bindFunctions]
    """.trimIndent(),
    "时序图" to """
        sequenceDiagram
            participant K as Kotlin
            participant M as mermaid
            K->>M: render(id, text)
            M-->>K: RenderResult(svg, bindFunctions)
    """.trimIndent(),
    "类图" to """
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
    "状态图" to """
        stateDiagram-v2
            [*] --> 待渲染
            待渲染 --> 解析中: 点击渲染
            解析中 --> 已渲染: 语法通过
            解析中 --> 语法错误: 解析失败
            已渲染 --> [*]
    """.trimIndent(),
    "甘特图" to """
        gantt
            title wrapper 开发计划
            dateFormat YYYY-MM-DD
            section 声明
            生成配置类型 :done, a1, 2026-11-10, 3d
            手写入口声明 :active, a2, after a1, 2d
            section 验证
            浏览器测试 :a3, after a2, 2d
    """.trimIndent(),
    "饼图" to """
        pie title 图型使用占比
            "流程图" : 40
            "时序图" : 30
            "其他" : 30
    """.trimIndent(),
)

/** Themes offered by the selector; `mermaid` validates the value itself. */
private val THEMES = listOf("default", "dark", "forest", "neutral", "base", "neo")

/**
 * Entry point of the demo page.
 *
 * The page is one editor plus one canvas: a preset fills the editor, the render button hands the
 * text to [mermaid], and the result is inserted into the canvas — the same three steps a consumer
 * performs. Nodes are built with `document.createElement` and mutated in place; see the UI
 * conventions in AGENTS.md.
 */
fun main() {
    val output = document.createElement(HtmlTagName.div).apply { className = ClassName("canvas") }
    val status = document.createElement(HtmlTagName.p).apply {
        className = ClassName("status")
        textContent = "尚未渲染"
    }
    val source = document.createElement(HtmlTagName.textarea).apply {
        className = ClassName("editor")
        value = PRESETS.first().second
    }

    var renders = 0

    /** Renders [source] into [output]. Ids are unique per call: mermaid keys the svg by them. */
    fun renderNow() {
        status.textContent = "解析中…"
        val text = source.value
        mermaid.parseOrNull(text, unsafeJso { suppressErrors = true }).then(
            { parsed ->
                if (parsed == null) {
                    status.textContent = "语法错误：mermaid 无法解析这段文本"
                } else {
                    renders += 1
                    mermaid.render("ktjs-mermaid-demo-$renders", text, output).then(
                        { result ->
                            output.innerHTML = HtmlSource(result.svg)
                            result.bindFunctions?.invoke(output)
                            status.textContent = "已渲染：${result.diagramType}"
                        },
                        { status.textContent = "渲染失败：见控制台" },
                    )
                }
            },
            { status.textContent = "无法识别的图型，请检查文本" },
        )
    }

    fun applyTheme(theme: String) {
        mermaid.initialize(
            unsafeJso<MermaidConfig> {
                startOnLoad = false
                this.theme = MermaidConfigTheme(theme)
            },
        )
    }

    val themeSelect = document.createElement(HtmlTagName.select).apply {
        className = ClassName("theme")
        THEMES.forEach { theme ->
            append(
                document.createElement(HtmlTagName.option).apply {
                    value = theme
                    textContent = theme
                },
            )
        }
    }
    themeSelect.onchange = EventHandler {
        applyTheme(themeSelect.value)
        renderNow()
    }

    val presets = document.createElement(HtmlTagName.div).apply { className = ClassName("presets") }
    PRESETS.forEach { (label, text) ->
        presets.append(
            document.createElement(HtmlTagName.button).apply {
                className = ClassName("preset")
                textContent = label
                onclick = EventHandler {
                    source.value = text
                    renderNow()
                }
            },
        )
    }

    val renderButton = document.createElement(HtmlTagName.button).apply {
        className = ClassName("render")
        textContent = "渲染"
        onclick = EventHandler { renderNow() }
    }

    val toolbar = document.createElement(HtmlTagName.div).apply {
        className = ClassName("toolbar")
        append(presets, themeSelect, renderButton)
    }

    val root = document.createElement(HtmlTagName.div).apply { className = ClassName("page") }
    root.append(
        buildHeader(),
        buildHint(),
        toolbar,
        source,
        status,
        output,
    )
    document.body.append(root)

    applyTheme(themeSelect.value)
    renderNow()
}

private fun buildHeader(): HTMLElement {
    val logo = document.createElement(HtmlTagName.img).apply {
        className = ClassName("logo")
        setAttribute("src", "img/ktjs-mermaid-logo.png")
        setAttribute("alt", "$PROJECT_NAME logo")
    }
    val heading = document.createElement(HtmlTagName.h1).apply {
        textContent = "$PROJECT_NAME demo"
    }
    return document.createElement(HtmlTagName.header).apply {
        className = ClassName("header")
        append(logo, heading)
    }
}

private fun buildHint(): HTMLElement = document.createElement(HtmlTagName.p).apply {
    className = ClassName("hint")
    textContent = "页面上的图都由 ktjs-mermaid 调用 mermaid@12 渲染：改源码或换主题后点「渲染」。"
}
