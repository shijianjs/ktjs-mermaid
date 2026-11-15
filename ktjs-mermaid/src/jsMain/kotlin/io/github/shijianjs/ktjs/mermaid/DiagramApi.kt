package io.github.shijianjs.ktjs.mermaid

import io.github.shijianjs.ktjs.mermaid.config.BaseDiagramConfig
import io.github.shijianjs.ktjs.mermaid.config.MermaidConfig
import kotlin.js.JsAny
import kotlin.js.Promise
import kotlin.js.unsafeCast
import web.dom.Element

/**
 * Frontmatter metadata of a diagram.
 *
 * Upstream: `DiagramMetadata` in `mermaid/src/diagram-api/types.ts`.
 */
external interface DiagramMetadata {
    /** `title` from the YAML frontmatter. */
    var title: String?

    /** The configuration carried by the frontmatter or a directive. */
    var config: MermaidConfig?
}

/**
 * The preprocessed text a diagram parser works on.
 *
 * Upstream: `DiagramCode` in `mermaid/src/diagram-api/types.ts`.
 */
external interface DiagramCode {
    /** The original source text, untouched. */
    var raw: String

    /**
     * The fully processed text: line endings normalised, frontmatter and directives removed,
     * comments stripped.
     */
    var cleaned: String

    /** Like [cleaned], but with the `%%` comments still in place. */
    var withComments: String?

    /** The number of lines the YAML frontmatter occupied, `0` when there was none. */
    var frontmatterLineOffset: Double?
}

/**
 * The data store of a diagram: everything the parser fills in and the renderer reads back.
 *
 * Upstream: `DiagramDB` in `mermaid/src/diagram-api/types.ts`. Every member is optional there, so
 * a diagram only implements the ones it needs.
 */
external interface DiagramDB {
    /** The diagram-specific configuration. */
    var getConfig: (() -> BaseDiagramConfig?)?

    /** Drops every piece of state collected while parsing. */
    var clear: (() -> Unit)?

    /** Sets the diagram title. */
    var setDiagramTitle: ((String) -> Unit)?

    /** The diagram title. */
    var getDiagramTitle: (() -> String)?

    /** Sets the accessibility title. */
    var setAccTitle: ((String) -> Unit)?

    /** The accessibility title. */
    var getAccTitle: (() -> String)?

    /** Sets the accessibility description. */
    var setAccDescription: ((String) -> Unit)?

    /** The accessibility description. */
    var getAccDescription: (() -> String)?

    /** The direction the diagram is laid out in, when the diagram has one. */
    var getDirection: (() -> DiagramOrientation?)?

    /** Sets the direction. */
    var setDirection: ((DiagramOrientation) -> Unit)?

    /** Switches between the compact and the regular display mode. */
    var setDisplayMode: ((String) -> Unit)?

    /** Sets the id of the SVG element the diagram renders into. */
    var setDiagramId: ((String) -> Unit)?

    /** Binds the diagram's event listeners once its markup is in the document. */
    var bindFunctions: ((Element) -> Unit)?

    /** Sets the error message the diagram displays in place of a graph. */
    var setErrorMessage: ((String) -> Unit)?

    /** The error message set through [setErrorMessage]. */
    var getErrorMessage: (() -> String?)?

    /**
     * Opts the diagram into source-faithful parsing: the parser then sees [DiagramCode.withComments]
     * so its positions line up with what the author wrote.
     */
    val preserveCommentsWhenParsing: Boolean?

    /** Receives the number of frontmatter lines the parser never saw. */
    var setFrontmatterLineOffset: ((Double) -> Unit)?
}

/**
 * A CSS class a diagram text can declare with `classDef`.
 *
 * Upstream: `DiagramStyleClassDef` in `mermaid/src/diagram-api/types.ts`.
 */
external interface DiagramStyleClassDef {
    /** The class name the styles apply to. */
    var id: String

    /** CSS declarations without the trailing semicolon, e.g. `fill: red`. */
    var styles: Array<String>?

    /** CSS declarations that apply to the `<tspan>` elements carrying the class. */
    var textStyles: Array<String>?
}

/**
 * Draws a parsed diagram into the page.
 *
 * Upstream: `DrawDefinition` in `mermaid/src/diagram-api/types.ts`.
 *
 * The arguments are, in order: the original diagram text, the id the rendered element gets, the
 * mermaid version (so a diagram can record what rendered it) and the parsed diagram — db, parser
 * and renderer included.
 */
typealias DrawDefinition = (text: String, id: String, version: String, diagramObject: Diagram) -> JsAny?

/**
 * The parser half of a diagram definition.
 *
 * Upstream: `ParserDefinition` in `mermaid/src/diagram-api/types.ts`.
 */
external interface ParserDefinition {
    /** Parses `text` into the diagram's [DiagramDB]. */
    var parse: ((String) -> JsAny?)?

    /** The generated parser, when the diagram uses one. */
    var parser: JsAny?
}

/**
 * The renderer half of a diagram definition.
 *
 * Upstream: `DiagramRenderer` in `mermaid/src/diagram-api/types.ts`.
 */
external interface DiagramRenderer {
    /** Draws the parsed diagram. */
    var draw: DrawDefinition

    /**
     * Collects the `classDef` styles the text declares, keyed by class name.
     *
     * Upstream type is a JavaScript `Map`; see [StyleClasses] for why the Kotlin side cannot name
     * it directly.
     */
    var getClasses: ((String, DiagramDbPick) -> StyleClasses)?
}

/**
 * `Pick<DiagramDefinition, "db">`: the second argument of [DiagramRenderer.getClasses].
 */
external interface DiagramDbPick {
    /** The data store of the diagram being rendered. */
    val db: DiagramDB
}

/**
 * A JavaScript `Map<string, DiagramStyleClassDef>`.
 *
 * kotlin-wrappers' `JsMap` bounds its type parameters by `JsAny?`, which a Kotlin `String` key does
 * not satisfy, so the map reaches Kotlin as an opaque handle: pass it back to mermaid, or read
 * entries through [styleClass].
 */
external interface StyleClasses

/** Reads the class definition stored under [id] in a [StyleClasses] map. */
fun StyleClasses.styleClass(id: String): DiagramStyleClassDef? =
    getMapValue(this, id)?.unsafeCast<DiagramStyleClassDef>()

/** The full definition of a built-in or external diagram type. */
external interface DiagramDefinition {
    /** The data store the parser fills and the renderer reads. */
    var db: DiagramDB

    /** Draws the diagram. */
    var renderer: DiagramRenderer

    /** Parses the diagram text. */
    var parser: ParserDefinition

    /** The diagram's stylesheet, as `any` upstream. */
    var styles: JsAny?

    /** Runs once, before the first parse, with the configuration in effect. */
    var init: ((MermaidConfig) -> Unit)?

    /** Receives the helper functions mermaid injects into every diagram. */
    var injectUtils: JsAny?
}

/**
 * A diagram type contributed from outside mermaid.
 *
 * Upstream: `ExternalDiagramDefinition` in `mermaid/src/diagram-api/types.ts`.
 */
external interface ExternalDiagramDefinition {
    /** The key the diagram is registered under, e.g. `"flowchart-v2"`. */
    var id: String

    /** Decides whether a given text belongs to this diagram. */
    var detector: DiagramDetector

    /** Loads the diagram definition; runs lazily unless `lazyLoad` is `false`. */
    var loader: DiagramLoader
}

/** Decides whether `text` is this diagram's syntax. */
typealias DiagramDetector = (text: String, config: MermaidConfig?) -> Boolean

/** Loads a diagram definition on demand. */
typealias DiagramLoader = () -> Promise<LoadedDiagram>

/** What [DiagramLoader] resolves to. */
external interface LoadedDiagram {
    /** The diagram type key. */
    val id: String

    /** The diagram definition. */
    val diagram: DiagramDefinition
}

/**
 * The direction a diagram is laid out in.
 *
 * Upstream: `DiagramOrientation` in `mermaid/src/diagrams/git/gitGraphTypes.ts`.
 */
sealed external interface DiagramOrientation

/** Wraps one of `"LR"`, `"TB"`, `"BT"`. */
@Suppress("NOTHING_TO_INLINE")
inline fun DiagramOrientation(value: String): DiagramOrientation =
    value.unsafeCast<DiagramOrientation>()

/** The values [DiagramOrientation] accepts, as the upstream literals. */
object DiagramOrientations {
    /** `"LR"`: left to right. */
    inline val lr: DiagramOrientation get() = "LR".unsafeCast<DiagramOrientation>()

    /** `"TB"`: top to bottom. */
    inline val tb: DiagramOrientation get() = "TB".unsafeCast<DiagramOrientation>()

    /** `"BT"`: bottom to top. */
    inline val bt: DiagramOrientation get() = "BT".unsafeCast<DiagramOrientation>()
}

/**
 * A d3 selection of an SVG element.
 *
 * kotlin-wrappers does not model d3, so the type stays opaque: it is only handed back to mermaid
 * (e.g. from a custom layout's `render`), never built in Kotlin.
 */
external interface SVG

/** A d3 selection of an `<g>` element; see [SVG]. */
external interface SVGGroup

/** A d3 selection of an `<iframe>`; see [SVG]. */
external interface HTML

private val getMapValue: (JsAny, String) -> JsAny? = js(
    """
    function (map, key) {
      return map.get(key);
    }
    """,
)
