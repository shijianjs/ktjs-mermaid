package io.github.shijianjs.ktjs.mermaid

import kotlin.js.JsAny
import kotlin.js.Promise

/**
 * The layout options of [Mermaid.registerLayoutLoaders].
 *
 * Upstream: `RenderOptions` in `mermaid/src/rendering-util/render.ts`.
 */
external interface RenderOptions {
    /** The layout algorithm to use, e.g. `"dagre"`, `"elk"`, `"cose-bilkent"`. */
    var algorithm: String?
}

/**
 * A layout algorithm contributed from outside mermaid, e.g. a WebCol or ELK adapter.
 *
 * Upstream: `LayoutAlgorithm` in `mermaid/src/rendering-util/render.ts`.
 */
external interface LayoutAlgorithm {
    /**
     * Lays out the graph described by `layoutData` and draws it into `svg`.
     *
     * Declared as a property rather than a method so a Kotlin caller can write the object literal
     * (`unsafeJso<LayoutAlgorithm> { render = { ... } }`) that upstream expects from a loader.
     *
     * The four arguments are, in order: the nodes, edges and clusters mermaid collected while
     * parsing; the d3 selection of the `<svg>` to draw into; mermaid's internal drawing helpers
     * (see [InternalHelpers]); the options of the render call.
     */
    var render: (LayoutData, SVG, InternalHelpers, RenderOptions?) -> Promise<Unit>
}

/** Loads a [LayoutAlgorithm] on demand. */
typealias LayoutLoader = () -> Promise<LayoutAlgorithm>

/**
 * A layout algorithm mermaid can be told about.
 *
 * Upstream: `LayoutLoaderDefinition` in `mermaid/src/rendering-util/render.ts`.
 */
external interface LayoutLoaderDefinition {
    /** The name the algorithm is selected by, e.g. `"elk"`. */
    var name: String

    /** Loads the algorithm. */
    var loader: LayoutLoader

    /** The algorithm name this loader provides, when it differs from [name]. */
    var algorithm: String?
}

/**
 * The graph a custom layout lays out.
 *
 * Upstream: `LayoutData` in `mermaid/src/rendering-util/types.ts`, a structural interface of
 * several hundred lines that describes nodes, edges, clusters and the layout options in effect.
 * It is opaque here: a custom layout receives it from mermaid and passes it straight back, while
 * the wrapper keeps signatures honest instead of mirroring a structure nobody builds by hand.
 */
external interface LayoutData

/**
 * The drawing helpers mermaid injects into a custom layout.
 *
 * Upstream: `InternalHelpers` (`typeof internalHelpers` in `mermaid/src/internals.ts`), which
 * upstream itself marks deprecated and internal ("definitions will change without notice"). It
 * reaches Kotlin as an opaque handle: pass it through to the helpers mermaid provides.
 */
external interface InternalHelpers

/** A single helper of [InternalHelpers], reached by name; see the upstream `internalHelpers` shape. */
fun InternalHelpers.helper(name: String): JsAny? = readHelper(this, name)

private val readHelper: (JsAny, String) -> JsAny? = js(
    """
    function (helpers, name) {
      return helpers[name];
    }
    """,
)
