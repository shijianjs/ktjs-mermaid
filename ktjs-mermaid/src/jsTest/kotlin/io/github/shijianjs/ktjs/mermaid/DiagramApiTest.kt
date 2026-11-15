package io.github.shijianjs.ktjs.mermaid

import js.objects.unsafeJso
import kotlin.js.JsAny
import kotlin.js.unsafeCast
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Checks the supporting types the entry point's signatures depend on.
 *
 * Upstream sources: `mermaid/src/diagram-api/types.ts` (the `DiagramDB` / renderer contracts,
 * `DiagramOrientation`), `mermaid/src/utils.ts` (`isDetailedError`).
 */
class DiagramApiTest {
    @Test
    fun orientationConstantsAreTheUpstreamLiterals(): Unit {
        assertEquals("LR", DiagramOrientations.lr.unsafeCast<String>())
        assertEquals("TB", DiagramOrientations.tb.unsafeCast<String>())
        assertEquals("BT", DiagramOrientations.bt.unsafeCast<String>())
        assertEquals("LR", DiagramOrientation("LR").unsafeCast<String>())
    }

    @Test
    fun detailederrorIsRecognisedByTheStrProperty(): Unit {
        val detailed = unsafeJso<DetailedError> {
            str = "Parse error"
            hash = unsafeJso<JsAny> { }
            message = "Parse error"
        }
        assertTrue(isDetailedError(detailed))
        assertEquals("Parse error", detailedError(detailed.unsafeCast<JsAny>()).str)
        assertFalse(isDetailedError("A plain string".unsafeCast<JsAny>()))
        assertFalse(isDetailedError(null))
    }

    @Test
    fun iconLoaderWrapsBothKinds(): Unit {
        val sync = unsafeJso<SyncIconLoader> {
            name = "ktjs-test"
            icons = unsafeJso<IconifyJSON> {
                prefix = "ktjs-test"
                icons = unsafeJso { }
            }
        }
        val async = unsafeJso<AsyncIconLoader> {
            name = "ktjs-test-async"
            loader = { kotlin.js.Promise { resolveLoader, _ -> resolveLoader(sync.icons) } }
        }
        assertEquals("ktjs-test", IconLoader(sync).unsafeCast<SyncIconLoader>().name)
        assertEquals("ktjs-test-async", IconLoader(async).unsafeCast<AsyncIconLoader>().name)
    }

    @Test
    fun parseResultOrFalseWrapsBothBranches(): Unit {
        val result = ParseResultOrFalse(false)
        assertEquals(false, result.unsafeCast<Boolean>())
    }
}
