# ktjs-mermaid

Kotlin/JS wrapper for [`mermaid`](https://www.npmjs.com/package/mermaid) 12.3.0 — the library that
turns `graph TD; A-->B` into an SVG diagram. This is the wrapper module of the repository: one npm
package, one Gradle module, one Kotlin package.

## Installation

```kotlin
dependencies {
    implementation("io.github.shijianjs:ktjs-mermaid:12.3.0-kt.1")
}
```

`mermaid` itself arrives through the module's `npm(...)` dependency, so there is nothing else to
declare.

## Usage

```kotlin
import io.github.shijianjs.ktjs.mermaid.mermaid
import io.github.shijianjs.ktjs.mermaid.config.MermaidConfig
import io.github.shijianjs.ktjs.mermaid.config.MermaidConfigThemes
import js.objects.unsafeJso
import web.html.HtmlSource

mermaid.initialize(
    unsafeJso<MermaidConfig> {
        startOnLoad = false
        theme = MermaidConfigThemes.forest
    },
)

val host = document.createElement("div")
document.body.append(host)
mermaid.render("graph-1", "graph TD; A-->B", host).then { result ->
    host.innerHTML = HtmlSource(result.svg)
    result.bindFunctions?.invoke(host)
}
```

- `render(id, text, container?)` returns the markup, the detected diagram type and the bind
  function; inserting the markup is the caller's step. Keep the container's id different from the
  diagram id — mermaid looks its svg up by that id.
- `parse` validates text; `parseOrNull` (a wrapper extension) returns `null` instead of the `false`
  that `suppressErrors` produces, because Kotlin cannot express that branch in the union.
- `run(...)` renders the elements a selector finds, for pages that ship `<pre class="mermaid">`.
- `MermaidConfig` is the whole upstream configuration tree, generated from mermaid's own
  declarations; `registerIconPacks` and `registerLayoutLoaders` take Iconify sets and layout
  algorithms.

## Build and test

```bash
./gradlew :ktjs-mermaid:jsBrowserTest
```

The tests run in Chromium: a diagram is only verified once it has been measured and laid out.

After adding or upgrading an npm dependency, refresh the shared lock file:

```bash
./gradlew kotlinUpgradePackageLock
```
