package io.github.shijianjs.ktjs.mermaid

import io.github.shijianjs.ktjs.mermaid.config.CSSFontSize
import io.github.shijianjs.ktjs.mermaid.config.FlowchartDiagramConfigCurves
import io.github.shijianjs.ktjs.mermaid.config.MermaidConfig
import io.github.shijianjs.ktjs.mermaid.config.MermaidConfigElkLineHops
import io.github.shijianjs.ktjs.mermaid.config.MermaidConfigElkPresets
import io.github.shijianjs.ktjs.mermaid.config.MermaidConfigLogLevel
import io.github.shijianjs.ktjs.mermaid.config.MermaidConfigLogLevels
import io.github.shijianjs.ktjs.mermaid.config.MermaidConfigLook
import io.github.shijianjs.ktjs.mermaid.config.MermaidConfigLooks
import io.github.shijianjs.ktjs.mermaid.config.MermaidConfigSecurityLevels
import io.github.shijianjs.ktjs.mermaid.config.MermaidConfigThemes
import io.github.shijianjs.ktjs.mermaid.config.SankeyLabelStyles
import io.github.shijianjs.ktjs.mermaid.config.StringOrDouble
import js.objects.unsafeJso
import kotlin.js.unsafeCast
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Checks the generated `MermaidConfig` declarations against the literals upstream declares and
 * against what mermaid accepts at runtime. The literals come from
 * `node_modules/mermaid/dist/config.type.d.ts`, the same file `scripts/generate-config.py` reads.
 */
class ConfigTypesTest {
    @Test
    fun literalConstantsCarryTheUpstreamStrings(): Unit {
        assertEquals("default", MermaidConfigThemes.default.unsafeCast<String>())
        assertEquals("neo-dark", MermaidConfigThemes.neoDark.unsafeCast<String>())
        assertEquals("null", MermaidConfigThemes.`null`.unsafeCast<String>())
        assertEquals("classic", MermaidConfigLooks.classic.unsafeCast<String>())
        assertEquals("handDrawn", MermaidConfigLooks.handDrawn.unsafeCast<String>())
        assertEquals("fatal", MermaidConfigLogLevels.fatal.unsafeCast<String>())
        assertEquals("trace", MermaidConfigLogLevels.trace.unsafeCast<String>())
        assertEquals("strict", MermaidConfigSecurityLevels.strict.unsafeCast<String>())
        assertEquals("sandbox", MermaidConfigSecurityLevels.sandbox.unsafeCast<String>())
        assertEquals("legacy", SankeyLabelStyles.legacy.unsafeCast<String>())
        assertEquals("basis", FlowchartDiagramConfigCurves.basis.unsafeCast<String>())
        assertEquals("modelOrder", MermaidConfigElkPresets.modelOrder.unsafeCast<String>())
    }

    @Test
    fun unionsWithNumbersAcceptBothBranches(): Unit {
        // `logLevel` upstream is `'trace' | 0 | 'debug' | 1 | … | 'fatal' | 5`.
        assertEquals("info", MermaidConfigLogLevel("info").unsafeCast<String>())
        assertEquals(2.0, MermaidConfigLogLevel(2.0).unsafeCast<Double>())
        // `lineHops` upstream is `boolean | ('arc' | 'gap')`: a three-way union.
        assertEquals(false, MermaidConfigElkLineHops(false).unsafeCast<Boolean>())
        assertEquals("arc", MermaidConfigElkLineHops("arc").unsafeCast<String>())
    }

    @Test
    fun stringOrDoubleCarriesCssLengths(): Unit {
        assertEquals("12px", StringOrDouble("12px").unsafeCast<String>())
        assertEquals(12.0, StringOrDouble(12.0).unsafeCast<Double>())
    }

    @Test
    fun nestedConfigIsAppliedByInitialize() {
        mermaid.initialize(
            unsafeJso<MermaidConfig> {
                startOnLoad = false
                theme = MermaidConfigThemes.forest
                look = MermaidConfigLook("handDrawn")
                themeCSS = ".node { fill: #ffe; }"
                flowchart = unsafeJso {
                    htmlLabels = false
                    curve = FlowchartDiagramConfigCurves.linear
                    nodeSpacing = 30.0
                }
            },
        )
        val config = mermaid.mermaidAPI.getConfig()
        assertEquals("forest", config.theme.unsafeCast<String>())
        assertEquals("handDrawn", config.look.unsafeCast<String>())
        assertEquals(".node { fill: #ffe; }", config.themeCSS)
        val flowchart = config.flowchart
        assertEquals(false, flowchart?.htmlLabels)
        assertEquals(30.0, flowchart?.nodeSpacing)
        assertEquals("linear", flowchart?.curve.unsafeCast<String>())
    }

    @Test
    fun defaultConfigExposesTheTypedTree(): Unit {
        val defaults = mermaid.mermaidAPI.defaultConfig
        // The generated interfaces are structural, so the default object reads back as the same
        // tree mermaid ships: a missing declaration would surface as a null member here.
        assertEquals(
            FlowchartDiagramConfigCurves.basis.unsafeCast<String>(),
            defaults.flowchart?.curve.unsafeCast<String>(),
        )
        assertEquals(MermaidConfigThemes.default.unsafeCast<String>(), defaults.theme.unsafeCast<String>())
        assertEquals(MermaidConfigSecurityLevels.strict.unsafeCast<String>(), defaults.securityLevel.unsafeCast<String>())
    }
}
