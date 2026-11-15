# ktjs-mermaid

<p align="center"><img src="docs/src/jsMain/resources/static/img/ktjs-mermaid-logo.png" width="120" alt="ktjs-mermaid"></p>

A Kotlin/JS wrapper for [`mermaid`](https://www.npmjs.com/package/mermaid) 12.3.0: the whole public
API of the npm package as compiler-checked `external` declarations, plus a demo page, a documentation
site built with [ktjs-docusaurus](https://github.com/shijianjs/ktjs-docusaurus), and the tests and
release pipeline around them.

[English](README.md) | [简体中文](README.zh-CN.md)

**[Documentation](https://shijianjs.github.io/ktjs-mermaid/)** ·
**[Live demo](https://shijianjs.github.io/ktjs-mermaid/demo/)** ·
**[API reference](https://shijianjs.github.io/ktjs-mermaid/api/)**

## Modules

| Module | npm package | Wrapper version |
| --- | --- | --- |
| [ktjs-mermaid](ktjs-mermaid/README.md) | `mermaid` | `12.3.0-kt.1` |

The repository keeps one Gradle module per npm package, or per group of packages that share an
upstream version and release cadence. Tree shaking removes unused declarations from the final
application bundle, so grouping related packages into one module is cheaper than one module per
package.

## What the wrapper covers

- the entry object (`mermaid`) with `initialize` / `render` / `parse` / `run` / `detectType` /
  `registerIconPacks` / `registerExternalDiagrams` / `registerLayoutLoaders`, the `MermaidAPI`
  singleton behind it and the `Diagram` type it returns;
- the result types those calls hand back (`RenderResult`, `ParseResult`, `ParseOptions`,
  `RunOptions`, `DetailedError`, `UnknownDiagramError`);
- the diagram-definition contracts an external diagram implements (`DiagramDefinition`,
  `DiagramDB`, `DiagramRenderer`, `ParserDefinition`);
- the Iconify shapes and both icon-loader kinds, plus the layout-loader contract;
- `MermaidConfig`, generated from mermaid's own `.d.ts`: 36 interfaces, 572 members, constants for
  every string-literal union.

What is deliberately not wrapped — the all-`any` themes module, `Diagram.fromText`, the detector
registry, the logger — is listed with reasons in
[coverage.md](docs/development/coverage.md).

## Requirements

- JDK 21 or newer (the build uses Gradle 9.4.1).
- Node and npm are downloaded automatically by the Kotlin Gradle plugin; `kotlin.js.yarn=false` in
  `gradle.properties` keeps npm as the package manager.
- Chromium is downloaded by Playwright the first time browser tests run.

## Build and test

```bash
./gradlew build                    # compile, Detekt, browser tests
./gradlew :ktjs-mermaid:jsBrowserTest

# End-to-end checks for the demo page (Playwright, against the bundled output)
./gradlew :demo:e2eTest

# Documentation site: Kotlin configuration, Markdown pages, built by ktjs-docusaurus
./gradlew :docs:docusaurusBuild

# GitHub Pages site (documentation at the root, demo under /demo/, Dokka output under /api/)
./gradlew pagesSite
```

`build` does not run the demo's end-to-end suite or assemble the Pages site: both need a browser and
run as separate CI steps.

After adding or upgrading an npm dependency, refresh the shared lock file:

```bash
./gradlew kotlinUpgradePackageLock
```

The `docs` module is a Kotlin/JS project too. All three ktjs-docusaurus versions live in
`gradle.properties` — the wrapper klib, the Gradle plugin and the kit whose home sections the landing
page uses — because each of those modules carries its own revision. A SNAPSHOT there is resolved from
`mavenLocal`, so building the site against an unreleased wrapper means publishing that wrapper
locally first. The kit's browser bundle is not a Maven artifact: it ships as a resource of the kit's
klib, and `extractKitBundle` copies it to the site's `static/ktjs-client/`.

## Versioning

Each module is versioned as `{upstream npm version}-kt.{revision}`. The upstream npm versions are
declared once in the root [`package.json`](package.json); the revision is read per module from
[`wrapper-revisions.properties`](wrapper-revisions.properties). Gradle derives the base version and
the `npm(...)` dependency from these two files:

- Only `-kt.N` versions are published; a version without the suffix never exists.
- A working tree always adds `-SNAPSHOT`: the derived number is the *next* release, which Central does
  not have yet, so development builds never impersonate it. Publishing passes `-Pktjs.release=true`,
  which drops the suffix.
- Upstream upgrade: change the version in `package.json`; the base version follows automatically
  while the revision stays, even when the wrapper code itself did not change.
- Wrapper-only fix: the revision increments (`kt.2`, `kt.3`, ...) while the base version stays.
- Revisions are tracked per module and only ever increase; they never reset to `kt.1`. A release
  bumps and publishes only the modules that changed.

## Publishing

Publishing is configured once in the root [`build.gradle.kts`](build.gradle.kts) for all modules.
Releases run through `.github/workflows/release.yml`: it signs the artifacts, packages one archive per
Maven coordinate and registers them with the shared Maven Central hub, which merges the pending
registrations into a single deployment. A derived project that does not want to publish can drop that
workflow and the publishing block; nothing else depends on it.

## License

Released under the [MIT License](LICENSE).
