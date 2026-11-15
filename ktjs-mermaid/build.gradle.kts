import org.jetbrains.kotlin.gradle.ExperimentalJsTestDsl

plugins {
    kotlin("multiplatform")
}

// Wrapper version = {upstream npm version}-kt.{revision}. See the versioning section in AGENTS.md.
@Suppress("UNCHECKED_CAST")
val npmVersions = rootProject.extra["npmVersions"] as Map<String, String>
@Suppress("UNCHECKED_CAST")
val wrapperRevisions = rootProject.extra["wrapperRevisions"] as Map<String, Int>
val upstreamVersion = npmVersions.getValue("mermaid")
val wrapperRevision = wrapperRevisions.getValue(project.name)
// Empty in a release build, `-SNAPSHOT` in a working tree: see the note in the root build script.
@Suppress("UNCHECKED_CAST")
val versionSuffix = rootProject.extra["versionSuffix"] as String
version = "$upstreamVersion-kt.$wrapperRevision$versionSuffix"
description = "Kotlin/JS wrapper for the mermaid package."

kotlin {
    js {
        useEsModules()

        compilerOptions {
            target = "es2015"
        }

        // mermaid renders into the document and needs a DOM, so the tests run in Chromium
        // (Playwright) instead of Node.
        browser {
            @OptIn(ExperimentalJsTestDsl::class)
            test {
                chromium()
            }
        }

        binaries.library()
    }

    // The Node test runtime has no DOM, and every case here renders a diagram, so the Node target
    // would only ever fail. Keep the browser suite as the single source of test results.
    tasks.matching { it.name == "jsNodeTest" }.configureEach { enabled = false }

    compilerOptions {
        // Kotlin/JS dead-code-eliminates a library's JS output down to the `@JsExport` roots, and a
        // wrapper module has none: the whole module would compile to an empty file, so any consumer
        // that imports it at runtime gets `undefined`. Keep the public API; a consumer's bundler
        // still shakes whatever it does not use. See the DCE note in AGENTS.md.
        freeCompilerArgs.add("-Xir-dce=false")
        // `kotlin.js.JsAny` is the type of upstream members declared as `any` / `unknown`.
        optIn.add("kotlin.js.ExperimentalWasmJsInterop")
    }

    sourceSets {
        jsMain.dependencies {
            implementation(npm("mermaid", upstreamVersion))

            // The public surface exposes kotlin-browser types (`HTMLElement`, `Element`), so this is
            // `api`: a consumer compiles against them through this module. `kotlin-js` is where
            // `js.objects.unsafeJso` / `js.objects.Record` live, the way callers build a
            // `MermaidConfig` object literal.
            api("org.jetbrains.kotlin-wrappers:kotlin-browser:2026.11.1")
            api("org.jetbrains.kotlin-wrappers:kotlin-js:2026.11.1")
        }
        jsTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
