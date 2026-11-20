package demo.page.playground

import io.github.shijianjs.ktjs.dom.element.invoke
import io.github.shijianjs.ktjs.dom.element.unaryPlus

/** Every string this page shows, in one language. */
internal class PlaygroundTexts(
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
