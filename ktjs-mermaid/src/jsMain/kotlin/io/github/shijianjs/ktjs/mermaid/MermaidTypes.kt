package io.github.shijianjs.ktjs.mermaid

import io.github.shijianjs.ktjs.mermaid.config.MermaidConfig
import js.array.ArrayLike
import kotlin.js.JsAny
import kotlin.js.Promise
import kotlin.js.unsafeCast
import web.dom.Element
import web.dom.NodeList
import web.html.HTMLElement

/**
 * Handler installed through [Mermaid.setParseErrorHandler] (or assigned to [Mermaid.parseError]).
 *
 * Upstream declares `(err: string | DetailedError | unknown, hash?: any) => void`. The `unknown`
 * branch makes the error untypeable, so the first parameter is [JsAny]: narrow it with
 * [isDetailedError] and [detailedError] when the caller wants the structured form, or read a
 * `String` for the plain one.
 */
typealias ParseErrorFunction = (JsAny?, JsAny?) -> Unit

/**
 * Options of [Mermaid.run].
 *
 * Upstream: `RunOptions` in `mermaid/src/mermaid.ts`.
 */
external interface RunOptions {
    /**
     * The query selector used to find the elements to render. Default: `".mermaid"`.
     */
    var querySelector: String?

    /**
     * The nodes to render. When set, `querySelector` is ignored.
     */
    var nodes: ArrayLike<HTMLElement>?

    /**
     * Called after each diagram is rendered, with the diagram's id.
     *
     * Upstream types the return value as `unknown` and ignores it, so the Kotlin side takes a
     * `Unit` callback: a Kotlin lambda returns `undefined`, which is a valid `unknown`.
     */
    var postRenderCallback: ((String) -> Unit)?

    /**
     * When `true`, errors are logged to the console instead of thrown. Default: `false`.
     */
    var suppressErrors: Boolean?
}

/**
 * What [Mermaid.render] returns.
 *
 * Upstream: `RenderResult` in `mermaid/src/types.ts`.
 */
external interface RenderResult {
    /**
     * The SVG markup of the rendered diagram, ready to be inserted into the DOM.
     */
    val svg: String

    /**
     * The diagram type that was detected, e.g. `"flowchart-v2"`, `"sequence"`.
     */
    val diagramType: String

    /**
     * Binds the event listeners mermaid needs after the SVG has been inserted into the document.
     *
     * Call it on the element the SVG was inserted into:
     *
     * ```kotlin
     * val result = mermaid.render("graph1", "graph TD; A-->B").await()
     * element.innerHTML = result.svg
     * result.bindFunctions?.invoke(element)
     * ```
     */
    val bindFunctions: ((Element) -> Unit)?
}

/**
 * Options of [Mermaid.parse].
 *
 * Upstream: `ParseOptions` in `mermaid/src/types.ts`.
 */
external interface ParseOptions {
    /**
     * When `true`, a syntax error returns `false` instead of throwing and instead of calling the
     * parse-error handler.
     */
    var suppressErrors: Boolean?
}

/**
 * The result of a successful [Mermaid.parse].
 *
 * Upstream: `ParseResult` in `mermaid/src/types.ts`.
 */
external interface ParseResult {
    /**
     * The detected diagram type, e.g. `"flowchart-v2"`.
     */
    val diagramType: String

    /**
     * The configuration mermaid ended up with, including YAML frontmatter and directives found in
     * the diagram text.
     */
    val config: MermaidConfig
}

/**
 * `ParseResult | false`: what [Mermaid.parse] resolves to.
 *
 * Upstream returns `false` only when `options.suppressErrors` is `true`; otherwise it throws. Kotlin
 * cannot express a union of an object and a literal, so the result is opaque and callers either
 * cast it or use the [Mermaid.parseOrNull] extension, which turns `false` into `null`.
 */
external interface ParseResultOrFalse

/** Wraps the successful half of [ParseResultOrFalse]. */
@Suppress("NOTHING_TO_INLINE")
inline fun ParseResultOrFalse(value: ParseResult): ParseResultOrFalse =
    value.unsafeCast<ParseResultOrFalse>()

/** Wraps the `false` half of [ParseResultOrFalse]. */
@Suppress("NOTHING_TO_INLINE")
inline fun ParseResultOrFalse(value: Boolean): ParseResultOrFalse =
    value.unsafeCast<ParseResultOrFalse>()

/**
 * The structured form of a parse error, as passed to [Mermaid.parseError].
 *
 * Upstream: `DetailedError` in `mermaid/src/utils.ts`.
 */
external interface DetailedError {
    /** The error message. */
    var str: String

    /** The token hash the parser was at, as `any` upstream. */
    var hash: JsAny?

    /** The underlying exception, if there was one. */
    var error: JsAny?

    /** Same message as [str], when the error came from an exception. */
    var message: String?
}

/**
 * True when [error] is a [DetailedError] rather than a plain string.
 *
 * Mirrors upstream `isDetailedError` (`'str' in error`), which `mermaid/src/utils.ts` uses to tell
 * the two shapes the parse-error handler can receive apart.
 */
fun isDetailedError(error: JsAny?): Boolean = error != null && hasStrProperty(error)

/**
 * Narrows [error] to [DetailedError] after [isDetailedError] returned `true`.
 */
@Suppress("NOTHING_TO_INLINE")
inline fun detailedError(error: JsAny): DetailedError = error.unsafeCast<DetailedError>()

/**
 * Thrown by [Mermaid.render] and [Mermaid.parse] when the diagram type is not registered.
 *
 * Upstream: `class UnknownDiagramError extends Error` in `mermaid/src/errors.ts`.
 */
external interface UnknownDiagramError {
    /** Always `"UnknownDiagramError"`. */
    val name: String

    /** The message, e.g. `"No diagram type detected matching given configuration for text: …"`. */
    val message: String
}

/**
 * Options of [Mermaid.registerExternalDiagrams].
 */
external interface RegisterExternalDiagramsOptions {
    /**
     * When `true` (the default), the diagram loader runs only for text its detector matched.
     */
    var lazyLoad: Boolean?
}

/**
 * What [Mermaid.getRegisteredDiagramsMetadata] returns: the id of every registered diagram.
 *
 * Upstream is `Pick<ExternalDiagramDefinition, "id">`.
 */
external interface RegisteredDiagramMetadata {
    /** The diagram type key, e.g. `"flowchart-v2"`. */
    val id: String
}

/**
 * `string | HTMLElement | NodeListOf<HTMLElement>`: the `nodes` argument of [Mermaid.init].
 */
external interface InitNodes

/** Wraps the CSS-selector form of [InitNodes]. */
@Suppress("NOTHING_TO_INLINE")
inline fun InitNodes(value: String): InitNodes = value.unsafeCast<InitNodes>()

/** Wraps the single-element form of [InitNodes]. */
@Suppress("NOTHING_TO_INLINE")
inline fun InitNodes(value: HTMLElement): InitNodes = value.unsafeCast<InitNodes>()

/** Wraps the node-list form of [InitNodes]. */
@Suppress("NOTHING_TO_INLINE")
inline fun InitNodes(value: NodeList<HTMLElement>): InitNodes = value.unsafeCast<InitNodes>()

/**
 * Resolves [text] or `null` instead of failing, mirroring `parse(text, {suppressErrors: true})`.
 *
 * A wrapper convenience: upstream already offers this through [ParseOptions], but the union it
 * returns is not usable as a Kotlin type, so the common case gets a nullable result.
 */
fun Mermaid.parseOrNull(text: String, options: ParseOptions? = null): Promise<ParseResult?> =
    parse(text, options).then { result ->
        // `false` is the only non-object the union can hold; `== false` compiles to `=== false`.
        if (result.unsafeCast<Boolean?>() == false) null else result.unsafeCast<ParseResult>()
    }

/** True when `value` has an own `str` property, which is what upstream tests for. */
private val hasStrProperty: (JsAny) -> Boolean = js(
    """
    function (value) {
      return 'str' in Object(value);
    }
    """,
)
