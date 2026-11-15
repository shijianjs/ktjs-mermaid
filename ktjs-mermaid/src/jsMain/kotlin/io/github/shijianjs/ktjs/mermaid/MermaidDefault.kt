@file:JsModule("mermaid")

package io.github.shijianjs.ktjs.mermaid

import kotlin.js.JsName

/**
 * The default export of the package, i.e. the singleton Mermaid object.
 *
 * A top-level `external val` carrying `@JsName("default")` is what compiles to a default import;
 * declaring an `external object` for the module namespace instead emits a *named* import
 * (`import { MermaidModule } from "mermaid"`), which webpack rejects because the package only
 * exports `default`. Keep this the single `@JsName("default")` declaration in the module: a second
 * one makes the compiler rename it to `default_0`.
 *
 * A `@JsModule` file may only hold external declarations, which is why the public accessor lives in
 * `Mermaid.kt`.
 */
@JsName("default")
internal external val mermaidDefault: Mermaid
