package demo.e2e

import com.microsoft.playwright.Page
import com.microsoft.playwright.junit.UsePlaywright
import com.microsoft.playwright.options.WaitUntilState
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junitpioneer.jupiter.RetryingTest

/**
 * Smoke checks for the demo page: it must load the bundle, render a diagram through the wrapper,
 * react to the preset and theme controls, and produce no console errors or failed requests.
 */
@UsePlaywright(DemoOptions::class)
class DemoPageTest {
    @RetryingTest(maxAttempts = 2)
    fun `page renders the project heading and styles`(page: Page) {
        openPage(page)

        assertTrue(page.title().contains("ktjs-mermaid"), "title: ${page.title()}")
        assertEquals("ktjs-mermaid demo", page.locator("h1").textContent())

        // The logo is copied in from the site's static files by the `brandAssets` task.
        assertTrue(page.locator("img.logo").isVisible(), "the demo serves the project logo")

        val display = page.locator(".page").evaluate("(node) => getComputedStyle(node).display") as String
        assertTrue(display != "contents", "the stylesheet applies to .page (display = $display)")
    }

    @RetryingTest(maxAttempts = 2)
    fun `the wrapper renders the default diagram`(page: Page) {
        openPage(page)

        val svg = page.locator(".canvas svg")
        assertTrue(svg.isVisible(), "the canvas holds an <svg> the wrapper produced")
        assertTrue(
            page.locator(".status").textContent().contains("flowchart-v2"),
            "status reports the detected diagram type: ${page.locator(".status").textContent()}",
        )
    }

    @RetryingTest(maxAttempts = 2)
    fun `a preset swaps the source and the rendered diagram`(page: Page) {
        openPage(page)

        page.getByText("时序图", com.microsoft.playwright.Page.GetByTextOptions().setExact(true)).click()
        page.waitForSelector(".canvas svg")

        assertTrue(
            page.locator(".editor").inputValue().contains("sequenceDiagram"),
            "the preset fills the editor",
        )
        assertTrue(
            page.locator(".status").textContent().contains("sequence"),
            "status follows the new diagram type: ${page.locator(".status").textContent()}",
        )
    }

    @RetryingTest(maxAttempts = 2)
    fun `an invalid source is reported instead of rendered`(page: Page) {
        openPage(page)

        page.locator(".editor").fill("graph TD; A-->[")
        page.getByText("渲染", com.microsoft.playwright.Page.GetByTextOptions().setExact(true)).click()
        page.waitForFunction("() => document.querySelector('.status').textContent.startsWith('语法错误')")

        assertTrue(
            page.locator(".status").textContent().startsWith("语法错误"),
            "invalid text is reported as a syntax error",
        )
    }

    @RetryingTest(maxAttempts = 2)
    fun `the theme selector re-renders the diagram`(page: Page) {
        openPage(page)

        page.locator(".theme").selectOption("dark")
        page.waitForFunction("() => document.querySelector('.status').textContent.startsWith('已渲染')")

        assertTrue(
            page.locator(".canvas svg").isVisible(),
            "the diagram is rendered again after the theme change",
        )
    }

    @RetryingTest(maxAttempts = 2)
    fun `page logs no console errors or failed requests`(page: Page) {
        val consoleErrors = mutableListOf<String>()
        val failedRequests = mutableListOf<String>()
        page.onConsoleMessage { message ->
            if (message.type() == "error") consoleErrors += message.text()
        }
        page.onRequestFailed { request ->
            failedRequests += "${request.url()} ${request.failure()}"
        }

        openPage(page)

        assertTrue(consoleErrors.isEmpty(), "console errors: $consoleErrors")
        assertTrue(failedRequests.isEmpty(), "failed requests: $failedRequests")
    }

    private fun openPage(page: Page) {
        page.navigate("/", Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE))
    }
}
