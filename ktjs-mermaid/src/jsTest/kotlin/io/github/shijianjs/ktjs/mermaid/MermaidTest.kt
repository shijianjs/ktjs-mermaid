package io.github.shijianjs.ktjs.mermaid

import io.github.shijianjs.ktjs.mermaid.config.MermaidConfig
import io.github.shijianjs.ktjs.mermaid.config.MermaidConfigLogLevels
import io.github.shijianjs.ktjs.mermaid.config.MermaidConfigSecurityLevels
import io.github.shijianjs.ktjs.mermaid.config.MermaidConfigThemes
import js.objects.unsafeJso
import kotlin.js.Promise
import kotlin.js.unsafeCast
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import web.dom.ElementId
import web.dom.document
import web.html.HTMLElement
import web.html.HtmlSource
import web.html.asStringOrNull

/**
 * Exercises the entry point against the real npm package: every case below renders, parses or
 * inspects a diagram, so a declaration that does not match the runtime shows up here.
 *
 * Upstream sources: `mermaid/src/mermaid.ts` for the entry point, `mermaid/src/types.ts` for
 * [RunOptions] / [RenderResult] / [ParseResult].
 */
class MermaidTest {
    @Test
    fun initializeIsReflectedInTheConfiguration() {
        mermaid.initialize(
            unsafeJso<MermaidConfig> {
                startOnLoad = false
                theme = MermaidConfigThemes.dark
                securityLevel = MermaidConfigSecurityLevels.loose
                logLevel = MermaidConfigLogLevels.fatal
            },
        )
        val config = mermaid.mermaidAPI.getConfig()
        assertEquals("dark", config.theme.unsafeCast<String>())
        assertEquals("loose", config.securityLevel.unsafeCast<String>())
        assertEquals("fatal", config.logLevel.unsafeCast<String>())
        assertEquals(false, config.startOnLoad)
    }

    @Test
    fun renderReturnsSvgAndTheDiagramType(): Promise<Unit> {
        initializeQuietly()
        // The container must not carry the id the diagram is rendered under: mermaid looks the
        // svg up by that id and would find the container instead (getBBox is not a function there).
        val host = newHost("ktjs-mermaid-render-host")
        return mermaid.render("ktjs-mermaid-render", "graph TD; A-->B", host).then { result ->
            assertEquals("flowchart-v2", result.diagramType)
            assertContains(result.svg, "<svg")
            Unit
        }
    }

    @Test
    fun renderBindsTheDiagramAfterItIsInserted(): Promise<Unit> {
        initializeQuietly()
        return mermaid.render("ktjs-mermaid-bind", "graph TD; A-->B").then { result ->
            val bindFunctions = assertNotNull(result.bindFunctions, "mermaid returns a bind function")
            val host = newHost("ktjs-mermaid-bind-host")
            host.innerHTML = HtmlSource(result.svg)
            bindFunctions.invoke(host)
            assertContains(host.innerHTML.asStringOrNull().orEmpty(), "<svg")
            Unit
        }
    }

    @Test
    fun parseReportsTheDetectedTypeAndConfig(): Promise<Unit> {
        initializeQuietly()
        return mermaid.parse("sequenceDiagram\nAlice->>Bob: hi").then { result ->
            val parsed = result.unsafeCast<ParseResult>()
            assertEquals("sequence", parsed.diagramType)
            assertNotNull(parsed.config, "parse returns the configuration in effect")
            Unit
        }
    }

    @Test
    fun parseOrNullReturnsNullForInvalidText(): Promise<Unit> {
        initializeQuietly()
        return mermaid.parseOrNull("graph TD; A-->[", unsafeJso { suppressErrors = true }).then { result ->
            assertNull(result, "invalid text with suppressErrors yields null")
            Unit
        }
    }

    @Test
    fun detectTypeMatchesTheDetectorsUpstreamShips() {
        assertEquals("flowchart-v2", mermaid.detectType("graph TD; A-->B"))
        assertEquals("sequence", mermaid.detectType("sequenceDiagram\nAlice->>Bob: hi"))
    }

    @Test
    fun runRendersTheElementsTheSelectorFinds(): Promise<Unit> {
        initializeQuietly()
        val first = newHost("ktjs-mermaid-run-1", className = "mermaid")
        val second = newHost("ktjs-mermaid-run-2", className = "mermaid")
        first.textContent = "graph LR; X-->Y"
        second.textContent = "graph LR; Y-->Z"
        return mermaid.run(unsafeJso { querySelector = ".mermaid" }).then {
            assertContains(first.innerHTML.asStringOrNull().orEmpty(), "<svg")
            assertContains(second.innerHTML.asStringOrNull().orEmpty(), "<svg")
            Unit
        }
    }

    @Test
    fun registeredDiagramsIncludeTheBuiltInTypes() {
        val ids = mermaid.getRegisteredDiagramsMetadata().map { it.id }
        assertContains(ids, "flowchart-v2")
        assertContains(ids, "sequence")
    }

    @Test
    fun registerIconPacksAcceptsAnInMemoryPack() {
        mermaid.registerIconPacks(
            arrayOf(
                IconLoader(
                    unsafeJso<SyncIconLoader> {
                        name = "ktjs-test"
                        icons = unsafeJso<IconifyJSON> {
                            prefix = "ktjs-test"
                            icons = unsafeJso { }
                            width = 24.0
                            height = 24.0
                        }
                    },
                ),
            ),
        )
        // A registered pack is looked up by diagrams while they render; the entry point itself
        // reports success by not throwing, which the absence of a failure here covers.
        assertTrue(true)
    }

    @Test
    fun registerExternalDiagramsRegistersItsDetector(): Promise<Unit> {
        initializeQuietly()
        return mermaid.registerExternalDiagrams(
            arrayOf(
                unsafeJso<ExternalDiagramDefinition> {
                    id = "ktjs-mermaid-test-diagram"
                    detector = { text, _ -> text.trimStart().startsWith("ktjsMermaidTest") }
                    loader = {
                        Promise { _, _ -> } // never resolved: detection does not load the diagram
                    }
                },
            ),
        ).then {
            assertEquals("ktjs-mermaid-test-diagram", mermaid.detectType("ktjsMermaidTest diagram"))
            Unit
        }
    }

    @Test
    fun registerLayoutLoadersAcceptsALoader() {
        mermaid.registerLayoutLoaders(
            arrayOf(
                unsafeJso<LayoutLoaderDefinition> {
                    name = "ktjs-mermaid-test-layout"
                    loader = {
                        Promise { resolveLayout, _ ->
                            resolveLayout(
                                unsafeJso<LayoutAlgorithm> {
                                    render = { _, _, _, _ -> Promise { done, _ -> done(Unit) } }
                                },
                            )
                        }
                    }
                },
            ),
        )
        assertTrue(true)
    }

    @Test
    fun getDiagramFromTextParsesWithoutTheDom(): Promise<Unit> {
        initializeQuietly()
        return mermaid.mermaidAPI.getDiagramFromText("graph TD; A-->B").then { diagram ->
            assertEquals("flowchart-v2", diagram.type)
            assertEquals("flowchart-v2", diagram.getType())
            assertNotNull(diagram.db)
            Unit
        }
    }

    /** Applies a known configuration so earlier state from another case cannot leak in. */
    private fun initializeQuietly() {
        mermaid.initialize(
            unsafeJso<MermaidConfig> {
                startOnLoad = false
                logLevel = MermaidConfigLogLevels.fatal
                securityLevel = MermaidConfigSecurityLevels.loose
            },
        )
    }
}

/** Creates a detached host element with [id], so rendering has a container to measure in. */
internal fun newHost(id: String, className: String? = null): HTMLElement {
    val host = document.createElement("div")
    host.id = ElementId(id)
    if (className != null) {
        host.setAttribute("class", className)
    }
    document.body.appendChild(host)
    return host
}
