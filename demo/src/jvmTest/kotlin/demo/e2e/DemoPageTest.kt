package demo.e2e

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.junit.UsePlaywright
import com.microsoft.playwright.options.WaitUntilState
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junitpioneer.jupiter.RetryingTest

/**
 * Smoke checks for the demo: the shell upgrades, the navigation lists both pages, the wrapper renders
 * a diagram, the preset / theme / editor controls work, and nothing is logged.
 *
 * Selectors prefer what the component itself renders (`vaadin-side-nav-item[data-route=…]`) or the
 * data attributes the playground writes on its editor, canvas and status line, so the suite does not
 * depend on demo class names.
 */
@UsePlaywright(DemoOptions::class)
class DemoPageTest {
    @RetryingTest(maxAttempts = 2)
    fun `the shell renders title, logo and both pages`(page: Page) {
        openPage(page)

        assertTrue(page.title().contains("ktjs-mermaid"), "title: ${page.title()}")
        assertTrue(page.locator("vaadin-side-nav-item[data-route]").count() == 2, "two pages in the drawer")
        assertTrue(page.locator("img.layout__logo").first().isVisible(), "the demo serves the project logo")

        val upgraded = page.evaluate("() => document.querySelector('ktjs-demo-layout')?.shadowRoot != null") as Boolean
        assertTrue(upgraded, "the declared <ktjs-demo-layout> upgraded into a component")
    }

    @RetryingTest(maxAttempts = 2)
    fun `the wrapper renders the default diagram`(page: Page) {
        openPlayground(page)

        page.waitForSelector("[data-canvas] svg")
        assertTrue(page.locator("[data-canvas] svg").first().isVisible(), "the canvas holds an <svg>")

        page.waitForSelector("[data-status]:has-text('Rendered')")
        assertTrue(
            (page.locator("[data-status]").textContent() ?: "").startsWith("Rendered"),
            "the status reports the render",
        )
    }

    @RetryingTest(maxAttempts = 2)
    fun `a preset swaps the source and the rendered diagram`(page: Page) {
        openPlayground(page)

        button(page, "Sequence").click()
        page.waitForSelector("[data-canvas] svg")

        val value = page.locator("[data-editor]").inputValue()
        assertTrue(value.contains("sequenceDiagram"), "the preset fills the editor: $value")
    }

    @RetryingTest(maxAttempts = 2)
    fun `an invalid source is reported instead of rendered`(page: Page) {
        openPlayground(page)

        page.locator("[data-editor]").fill("graph TD; A-->[")
        button(page, "Render").click()
        page.waitForSelector("[data-status]:has-text('Syntax error')")
    }

    @RetryingTest(maxAttempts = 2)
    fun `the theme selector re-renders the diagram`(page: Page) {
        openPlayground(page)

        val select = page.locator("ktjs-demo-page select").first()
        select.selectOption("dark")
        page.waitForSelector("[data-canvas] svg")

        assertTrue(select.inputValue() == "dark", "the picker holds the chosen theme")
        assertTrue(page.locator("[data-canvas] svg").first().isVisible(), "the diagram survives the re-render")
    }

    @RetryingTest(maxAttempts = 2)
    fun `the language menu swaps the page copy`(page: Page) {
        openPlayground(page)

        assertEquals("Playground", page.locator("ktjs-demo-page h1").first().textContent())

        page.locator("[data-menu='lang']").first().click()
        page.locator("[data-action='lang:zh-Hans']").first().click()

        assertEquals("演练场", page.locator("ktjs-demo-page h1").first().textContent())
        assertEquals("zh-Hans", page.locator("html").getAttribute("lang"))
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

        openPlayground(page)

        assertTrue(consoleErrors.isEmpty(), "console errors: $consoleErrors")
        assertTrue(failedRequests.isEmpty(), "failed requests: $failedRequests")
    }

    private fun button(page: Page, label: String): Locator =
        page.locator("vaadin-button").filter(Locator.FilterOptions().setHasText(label)).first()

    private fun openPage(page: Page) {
        page.navigate("/", Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE))
    }

    private fun openPlayground(page: Page) {
        page.navigate("/#/playground", Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE))
    }
}
