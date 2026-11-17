---
sidebar_position: 2
---

# Getting started

## Requirements

- JDK 21 or newer; the Gradle wrapper is checked in (9.4.1).
- Node and npm are downloaded and driven by the Kotlin Gradle plugin — there is no `npm install` step
  to remember, and `kotlin.js.yarn=false` keeps npm as the package manager.
- Chromium is downloaded by Playwright the first time a browser test runs.

## Dependency

```kotlin
dependencies {
    implementation("io.github.shijianjs:ktjs-mermaid:12.3.0-kt.1")
}
```

`mermaid` itself comes in through the module's `npm(...)` dependency, so a Kotlin/JS application does
not declare it a second time.

## The three calls

```kotlin
import io.github.shijianjs.ktjs.mermaid.mermaid
import io.github.shijianjs.ktjs.mermaid.config.MermaidConfig
import io.github.shijianjs.ktjs.mermaid.config.MermaidConfigThemes
import js.objects.unsafeJso

// 1. Configure once. `startOnLoad` is off by default here: nothing renders on its own.
mermaid.initialize(
    unsafeJso<MermaidConfig> {
        startOnLoad = false
        theme = MermaidConfigThemes.forest
    },
)

// 2. Render into SVG. The id is the DOM id the svg gets; keep it unique per diagram.
val host = document.createElement("div")
document.body.append(host)
mermaid.render("graph-1", "graph TD; A-->B", host).then { result ->
    host.innerHTML = HtmlSource(result.svg)
    // 3. Bind the listeners mermaid registered while rendering (tooltips, link clicks).
    result.bindFunctions?.invoke(host)
    println(result.diagramType) // "flowchart-v2"
}
```

`render` returns `RenderResult`: the markup, the detected diagram type and the bind function. Insert
the markup yourself — the wrapper does not touch your DOM beyond the container it measures in.

## Parsing without rendering

`parse` validates the text and reports what mermaid understood; with `suppressErrors` it resolves to
`false` instead of throwing, which Kotlin cannot type as part of the union, so the wrapper adds
`parseOrNull`:

```kotlin
mermaid.parseOrNull("graph TD; A-->[", unsafeJso { suppressErrors = true }).then { parsed ->
    if (parsed == null) {
        println("not valid mermaid")
    } else {
        println("${parsed.diagramType} with theme ${parsed.config.theme}")
    }
}
```

## Rendering what is already on the page

`run` finds the elements itself, so a page that ships `<pre class="mermaid">` markup needs one call:

```kotlin
mermaid.run(unsafeJso { querySelector = ".mermaid" })
```

Rendered elements are marked with `data-processed`, so calling `run` again after adding markup picks
up only the new diagrams.

## Configuration

`MermaidConfig` is the whole upstream configuration tree, generated from mermaid's own `.d.ts`:
diagram-specific blocks (`flowchart`, `sequence`, `gantt`, …), string-literal unions with their
constants (`MermaidConfigThemes.dark`, `MermaidConfigSecurityLevels.loose`), and CSS lengths
(`StringOrDouble("12px")`). Build it with `unsafeJso`:

```kotlin
mermaid.initialize(
    unsafeJso<MermaidConfig> {
        theme = MermaidConfigThemes.dark
        look = MermaidConfigLook("handDrawn")
        flowchart = unsafeJso {
            htmlLabels = false
            curve = FlowchartDiagramConfigCurves.linear
            nodeSpacing = 30.0
        }
    },
)
```

## Icons and layouts

Icon packs are Iconify sets; a set either travels with the bundle or is fetched on first use:

```kotlin
mermaid.registerIconPacks(
    arrayOf(
        IconLoader(
            unsafeJso<SyncIconLoader> {
                name = "logos"
                icons = unsafeJso<IconifyJSON> {
                    prefix = "logos"
                    icons = unsafeJso { }
                }
            },
        ),
    ),
)
```

Custom layout algorithms are registered with a lazy loader, which is how mermaid itself keeps ELK
optional:

```kotlin
mermaid.registerLayoutLoaders(
    arrayOf(
        unsafeJso<LayoutLoaderDefinition> {
            name = "elk"
            loader = { importElk().then { elk -> elkLayout(elk) } }
        },
    ),
)
```

## Build and test

```bash
./gradlew build                    # compile, Detekt, browser tests
./gradlew :ktjs-mermaid:jsBrowserTest
./gradlew :demo:e2eTest            # Playwright against the bundled demo page
./gradlew :docs:docusaurusBuild    # the documentation site
./gradlew pagesSite                # documentation at /, demo at /demo/, Dokka at /api/
```

`build` does not run the demo's end-to-end suite or assemble the Pages site: both need a browser and
are part of CI as separate steps.

## Develop

```bash
./gradlew :docs:docusaurusStart          # dev server for this site
./gradlew :demo:jsBrowserDevelopmentRun  # dev server for the demo page
```

Docusaurus watches the site directory, so Markdown edits reload; Kotlin changes need
`./gradlew :docs:docusaurusSite` first, because the site imports the compiled module.

## The lock file

npm dependencies are resolved once for the whole workspace and pinned in
`kotlin-js-store/package-lock.json`. After adding or upgrading an npm dependency — or changing a
module's version — regenerate it:

```bash
./gradlew kotlinUpgradePackageLock
```

CI verifies the committed file, so a forgotten regeneration fails the build rather than drifting.
