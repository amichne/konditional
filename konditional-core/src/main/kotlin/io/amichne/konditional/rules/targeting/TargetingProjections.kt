package io.amichne.konditional.rules.targeting

import io.amichne.konditional.context.Context
import io.amichne.konditional.rules.versions.VersionRange

/**
 * Internal projection helpers for extracting serializable structural data
 * from a [Targeting.All] tree.
 *
 * Custom/Guarded-Custom predicates are intentionally excluded from these projections
 * because they cannot be serialized (they are lambdas defined in code, not snapshots).
 *
 * These helpers are used by serialization round-trip and observability/explanation code.
 */

/**
 * Extracts locale ids from a [Targeting.All] tree.
 *
 * Locales appear as [Targeting.Guarded] wrapping [Targeting.Locale],
 * or directly as [Targeting.Locale] if stored without wrapping.
 */
internal fun <C : Context> Targeting.All<C>.localesOrEmpty(): Set<String> {
    val fromGuarded = targets.filterIsInstance<Targeting.Guarded<C, *>>()
        .mapNotNull { it.inner as? Targeting.Locale }
        .flatMapTo(linkedSetOf()) { it.ids }
    if (fromGuarded.isNotEmpty()) return fromGuarded
    return targets.filterIsInstance<Targeting.Locale>()
        .flatMapTo(linkedSetOf()) { it.ids }
}

/**
 * Extracts platform ids from a [Targeting.All] tree.
 */
internal fun <C : Context> Targeting.All<C>.platformsOrEmpty(): Set<String> {
    val fromGuarded = targets.filterIsInstance<Targeting.Guarded<C, *>>()
        .mapNotNull { it.inner as? Targeting.Platform }
        .flatMapTo(linkedSetOf()) { it.ids }
    if (fromGuarded.isNotEmpty()) return fromGuarded
    return targets.filterIsInstance<Targeting.Platform>()
        .flatMapTo(linkedSetOf()) { it.ids }
}

/**
 * Extracts the first version range from a [Targeting.All] tree.
 */
internal fun <C : Context> Targeting.All<C>.versionRangeOrNull(): VersionRange? {
    val fromGuarded = targets.filterIsInstance<Targeting.Guarded<C, *>>()
        .mapNotNull { it.inner as? Targeting.Version }
        .firstOrNull()?.range
    if (fromGuarded != null) return fromGuarded
    return targets.filterIsInstance<Targeting.Version>().firstOrNull()?.range
}

/**
 * Extracts axis constraints from a [Targeting.All] tree.
 */
internal fun <C : Context> Targeting.All<C>.axesOrEmpty(): Map<String, Set<String>> =
    targets.filterIsInstance<Targeting.Axis>()
        .associate { it.axisId to it.allowedIds }

/**
 * Counts custom/extension leaves in the tree (both direct and guarded-wrapped).
 */
internal fun <C : Context> Targeting.All<C>.customLeafCount(): Int =
    targets.count {
        it is Targeting.Custom<*> || (it is Targeting.Guarded<*, *> && it.inner is Targeting.Custom<*>)
    }
