package io.github.shijianjs.ktjs.mermaid

import js.objects.Record
import kotlin.js.Promise
import kotlin.js.unsafeCast

/**
 * An icon set that is fetched when it is first needed.
 *
 * Upstream: `AsyncIconLoader` in `mermaid/src/rendering-util/icons.ts`.
 */
external interface AsyncIconLoader {
    /** The prefix the set is addressable by, e.g. `"logos"`. */
    var name: String

    /** Fetches the Iconify JSON of the set. */
    var loader: () -> Promise<IconifyJSON>
}

/**
 * An icon set that is already in memory.
 *
 * Upstream: `SyncIconLoader` in `mermaid/src/rendering-util/icons.ts`.
 */
external interface SyncIconLoader {
    /** The prefix the set is addressable by, e.g. `"mermaid"`. */
    var name: String

    /** The Iconify JSON of the set. */
    var icons: IconifyJSON
}

/**
 * `AsyncIconLoader | SyncIconLoader`: an argument of [Mermaid.registerIconPacks].
 *
 * Upstream: `IconLoader` in `mermaid/src/rendering-util/icons.ts`.
 */
external interface IconLoader

/** Wraps the async half of [IconLoader]. */
@Suppress("NOTHING_TO_INLINE")
inline fun IconLoader(value: AsyncIconLoader): IconLoader = value.unsafeCast<IconLoader>()

/** Wraps the sync half of [IconLoader]. */
@Suppress("NOTHING_TO_INLINE")
inline fun IconLoader(value: SyncIconLoader): IconLoader = value.unsafeCast<IconLoader>()

/**
 * A single icon in Iconify's JSON format.
 *
 * Upstream: `IconifyIcon` in the `@iconify/types` package, which mermaid's icon loaders carry.
 * Dimensions and transformations are optional and fall back to the values on the set.
 */
external interface IconifyIcon {
    /** The SVG body of the icon, e.g. `<path d="..." />`. */
    var body: String

    /** Left edge of the icon's viewBox. Defaults to `0`. */
    var left: Double?

    /** Top edge of the icon's viewBox. Defaults to `0`. */
    var top: Double?

    /** Width of the icon's viewBox. Defaults to the set's [IconifyJSON.width]. */
    var width: Double?

    /** Height of the icon's viewBox. Defaults to the set's [IconifyJSON.height]. */
    var height: Double?

    /** Number of 90° rotations to apply. */
    var rotate: Double?

    /** Flips the icon horizontally. */
    var hFlip: Boolean?

    /** Flips the icon vertically. */
    var vFlip: Boolean?
}

/**
 * An icon as it appears inside a set, where it may also be marked hidden.
 *
 * Upstream: `ExtendedIconifyIcon` in `@iconify/types`.
 */
external interface ExtendedIconifyIcon : IconifyIcon {
    /** Keeps the icon in the set but out of every listing. */
    var hidden: Boolean?
}

/**
 * An alias of another icon, optionally overriding its dimensions and transformations.
 *
 * Upstream: `IconifyAlias` in `@iconify/types`.
 */
external interface IconifyAlias {
    /** The aliased icon, by name without the prefix. */
    var parent: String

    /** Left edge of the viewBox, when the alias overrides it. */
    var left: Double?

    /** Top edge of the viewBox, when the alias overrides it. */
    var top: Double?

    /** Width of the viewBox, when the alias overrides it. */
    var width: Double?

    /** Height of the viewBox, when the alias overrides it. */
    var height: Double?

    /** Additional 90° rotations, merged with the parent icon's. */
    var rotate: Double?

    /** Flips horizontally, merged with the parent icon's. */
    var hFlip: Boolean?

    /** Flips vertically, merged with the parent icon's. */
    var vFlip: Boolean?
}

/** An alias inside a set, which may also be marked hidden; see [ExtendedIconifyIcon]. */
external interface ExtendedIconifyAlias : IconifyAlias {
    /** Keeps the alias in the set but out of every listing. */
    var hidden: Boolean?
}

/**
 * An icon set in Iconify's JSON format.
 *
 * Upstream: `IconifyJSON` in `@iconify/types`, the payload an [IconLoader] provides.
 */
external interface IconifyJSON {
    /** The prefix every icon of the set is addressed with. */
    var prefix: String

    /** The icons, keyed by name without the prefix. */
    var icons: Record<String, ExtendedIconifyIcon>

    /** Aliases, keyed by alias name without the prefix. */
    var aliases: Record<String, ExtendedIconifyAlias>?

    /** The API provider the set was fetched from. */
    var provider: String?

    /** Default left edge of every icon's viewBox. */
    var left: Double?

    /** Default top edge of every icon's viewBox. */
    var top: Double?

    /** Default width of every icon's viewBox. */
    var width: Double?

    /** Default height of every icon's viewBox. */
    var height: Double?

    /** Human-readable metadata for icon-set browsers. */
    var info: IconifyJSONInfo?

    /** Characters that map to icon names, for icon fonts. */
    var chars: Record<String, String>?

    /** Categories the icons belong to. */
    var categories: Record<String, Array<String>>?

    /** Prefixes that group icons into themes. */
    var prefixes: Record<String, String>?

    /** Suffixes that group icons into themes. */
    var suffixes: Record<String, String>?

    /** When the icon data was last modified, as a Unix timestamp in seconds. */
    var lastModified: Double?

    /** Icons the set references but does not contain. */
    var not_found: Array<String>?
}

/**
 * Metadata of an icon set.
 *
 * Upstream: `IconifyInfo` in `@iconify/types`.
 */
external interface IconifyJSONInfo {
    /** Name of the set. */
    var name: String

    /** Total number of icons. */
    var total: Double?

    /** Version string of the set. */
    var version: String?

    /** Author of the set. */
    var author: IconifyJSONInfoAuthor

    /** License of the set. */
    var license: IconifyJSONInfoLicense

    /** Icons to show as samples of the set. */
    var samples: Array<String>?

    /** Icon grid, either one value for all icons or one per icon. */
    var height: DoubleOrDoubleArray?

    /** Height the samples are displayed at, `16`–`24`. */
    var displayHeight: Double?

    /** Category the set is listed under. */
    var category: String?

    /** Tags that group the set with similar ones. */
    var tags: Array<String>?

    /** `true` when the icons carry their own colours, `false` when they use `currentColor`. */
    var palette: Boolean?

    /** Keeps the set out of the icon-set listing. */
    var hidden: Boolean?
}

/** The author block of an [IconifyJSONInfo]. */
external interface IconifyJSONInfoAuthor {
    /** Name of the author. */
    var name: String

    /** Link to the author's or the set's website. */
    var url: String?
}

/** The license block of an [IconifyJSONInfo]. */
external interface IconifyJSONInfoLicense {
    /** Human-readable license name. */
    var title: String

    /** SPDX identifier of the license. */
    var spdx: String?

    /** Link to the license text. */
    var url: String?
}

/**
 * `number | number[]`: the `height` of an [IconifyJSONInfo].
 */
external interface DoubleOrDoubleArray

/** Wraps the single-value half of [DoubleOrDoubleArray]. */
@Suppress("NOTHING_TO_INLINE")
inline fun DoubleOrDoubleArray(value: Double): DoubleOrDoubleArray =
    value.unsafeCast<DoubleOrDoubleArray>()

/** Wraps the array half of [DoubleOrDoubleArray]. */
@Suppress("NOTHING_TO_INLINE")
inline fun DoubleOrDoubleArray(value: Array<Double>): DoubleOrDoubleArray =
    value.unsafeCast<DoubleOrDoubleArray>()
