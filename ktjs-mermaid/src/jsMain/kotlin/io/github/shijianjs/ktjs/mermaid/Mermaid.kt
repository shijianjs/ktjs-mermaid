package io.github.shijianjs.ktjs.mermaid

import io.github.shijianjs.ktjs.mermaid.config.MermaidConfig
import kotlin.js.JsAny
import kotlin.js.Promise
import web.dom.Element

/**
 * The mermaid singleton: the module's default export.
 *
 * ```kotlin
 * mermaid.initialize(unsafeJso<MermaidConfig> { startOnLoad = false })
 * val result = mermaid.render("graph1", "graph TD; A-->B").await()
 * ```
 *
 * Upstream: `interface Mermaid` plus `const mermaid: Mermaid` in `mermaid/src/mermaid.ts`.
 */
external interface Mermaid {
    /**
     * When `true`, mermaid renders every `.mermaid` element as soon as the document is loaded.
     *
     * Only meaningful before the module runs; a wrapper that renders explicitly keeps this `false`.
     */
    var startOnLoad: Boolean

    /**
     * The handler called when a diagram fails to parse.
     *
     * Upstream: `parseError?: ParseErrorFunction`.
     */
    var parseError: ParseErrorFunction?

    /**
     * The deprecated, internal API object.
     *
     * Upstream marks it `@internal` and `@deprecated`: use [parse], [render], [initialize] and [run]
     * instead. It stays declared because upstream exposes it.
     */
    val mermaidAPI: MermaidAPI

    /**
     * Validates a diagram without rendering it.
     *
     * Resolves to [ParseResult] when the text is valid. With `options.suppressErrors = true` it
     * resolves to `false` for invalid text instead of throwing, which is why the declared result is
     * the opaque [ParseResultOrFalse]; [parseOrNull] is the nullable shortcut.
     *
     * `UnknownDiagramError` is thrown when no diagram type matches the text.
     */
    fun parse(text: String, options: ParseOptions? = definedExternally): Promise<ParseResultOrFalse>

    /**
     * Renders a diagram to SVG markup.
     *
     * Calls are queued and run one after another.
     *
     * @param id the id the rendered SVG element gets; it has to be unique per document.
     * @param text the diagram definition.
     * @param container where mermaid may draw while measuring; it inserts a hidden container into
     *   the body when this is omitted, and removes it once rendering is done.
     * @return the SVG markup, the detected diagram type and the function that binds the diagram's
     *   event listeners after insertion.
     */
    fun render(id: String, text: String, container: Element? = definedExternally): Promise<RenderResult>

    /**
     * Renders every element matching `nodes`.
     *
     * @deprecated Upstream deprecates it in favour of [initialize] plus [run].
     */
    fun init(
        config: MermaidConfig? = definedExternally,
        nodes: InitNodes? = definedExternally,
        callback: ((String) -> Unit)? = definedExternally,
    ): Promise<Unit>

    /**
     * Finds the diagrams already in the document and renders them.
     *
     * Elements that were rendered before are marked with `data-processed` and skipped, so calling
     * this again after adding markup picks up only the new diagrams.
     */
    fun run(options: RunOptions? = definedExternally): Promise<Unit>

    /**
     * Registers layout algorithms, e.g. the ELK one.
     *
     * The loaders run lazily: a diagram that names the algorithm triggers its loader.
     */
    fun registerLayoutLoaders(loaders: Array<LayoutLoaderDefinition>)

    /**
     * Registers diagram types contributed from outside mermaid.
     *
     * Detectors run in registration order, so register more specific ones first.
     */
    fun registerExternalDiagrams(
        diagrams: Array<ExternalDiagramDefinition>,
        options: RegisterExternalDiagramsOptions? = definedExternally,
    ): Promise<Unit>

    /**
     * Applies the configuration that every later parse and render starts from.
     *
     * Call it before the first render; the wrapper's `startOnLoad` stays `false`, so nothing is
     * rendered implicitly.
     */
    fun initialize(config: MermaidConfig)

    /**
     * Renders the diagrams of a page that finished loading, using whatever configuration the page
     * declared.
     *
     * Upstream calls it from a `load` listener when `startOnLoad` is set.
     */
    fun contentLoaded()

    /**
     * Installs a parse-error handler.
     *
     * Provided for environments where [parseError] cannot be assigned directly — a Kotlin/JS
     * consumer can assign it, so this is mostly the upstream escape hatch.
     */
    fun setParseErrorHandler(handler: (err: JsAny?, hash: JsAny?) -> Unit)

    /**
     * Detects which diagram type [text] belongs to.
     *
     * @return the diagram key, e.g. `"flowchart-v2"`, or `""` when nothing matches.
     * Upstream throws `UnknownDiagramError` for unrecognized text; the declared `String`
     *   return is what upstream's signature promises.
     */
    fun detectType(text: String, config: MermaidConfig? = definedExternally): String

    /**
     * Registers icon packs, so diagrams can reference them as `prefix:name`.
     *
     * A loader is either already in memory ([SyncIconLoader]) or fetched on first use
     * ([AsyncIconLoader]).
     */
    fun registerIconPacks(loaders: Array<IconLoader>)

    /**
     * The ids of every registered diagram type.
     */
    fun getRegisteredDiagramsMetadata(): Array<RegisteredDiagramMetadata>
}

/**
 * The `mermaid` singleton, imported from the package's default export.
 *
 * Reads as the upstream constant: `mermaid.render(...)`, `mermaid.initialize(...)`.
 */
val mermaid: Mermaid
    get() = mermaidDefault
/**
 * The deprecated API object behind [Mermaid.mermaidAPI].
 *
 * Upstream marks the whole object `@internal`; only the members that are not superseded by the
 * top-level functions are typed here, and [Mermaid] itself is the supported entry point.
 */
external interface MermaidAPI {
    /** Renders a diagram; same function as [Mermaid.render]. */
    fun render(id: String, text: String, svgContainingElement: Element? = definedExternally): Promise<RenderResult>

    /** Validates a diagram without rendering it; same function as [Mermaid.parse]. */
    fun parse(text: String, options: ParseOptions? = definedExternally): Promise<ParseResultOrFalse>

    /** Applies a configuration; same function as [Mermaid.initialize]. */
    fun initialize(config: MermaidConfig)

    /**
     * Upstream marks this deprecated: it does nothing and is overwritten by the next [render] or
     * [parse].
     */
    fun setConfig(config: MermaidConfig): MermaidConfig

    /** The configuration currently in effect. */
    fun getConfig(): MermaidConfig

    /** The configuration that `reset` falls back to. */
    fun getSiteConfig(): MermaidConfig

    /** Replaces the site configuration and returns the merged result. */
    fun updateSiteConfig(config: MermaidConfig): MermaidConfig

    /** Resets the current configuration to the site configuration. */
    fun reset()

    /** Resets the site configuration too. */
    fun globalReset()

    /** mermaid's default configuration, the base every `initialize` merges into. */
    val defaultConfig: MermaidConfig

    /**
     * Parses the text into a diagram object.
     *
     * The DOM-free half of what [Mermaid.render] does, for callers that want to inspect or drive a
     * diagram themselves.
     */
    fun getDiagramFromText(text: String, metadata: DiagramMetadata? = definedExternally): Promise<Diagram>
}

/**
 * A parsed diagram.
 *
 * Upstream: `class Diagram` in `mermaid/src/Diagram.ts`, reachable through
 * [MermaidAPI.getDiagramFromText]. Its static `fromText` factory is not exposed: the package entry
 * does not export the class, so the instance route is the one a consumer can take.
 */
external class Diagram {
    /** The diagram type key, e.g. `"flowchart-v2"`. */
    val type: String

    /** The text the diagram was parsed from. */
    val text: String

    /** The data store the parser filled in. */
    val db: DiagramDB

    /** The parser that produced [db]. */
    val parser: ParserDefinition

    /** The renderer that draws [db]. */
    val renderer: DiagramRenderer

    /** Draws the diagram into a new element with [id]. */
    fun render(id: String, version: String): Promise<Unit>

    /** The parser of this diagram. */
    fun getParser(): ParserDefinition

    /** The diagram type key; same as [type]. */
    fun getType(): String
}
