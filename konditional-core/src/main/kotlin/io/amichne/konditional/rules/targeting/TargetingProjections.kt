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
 * Locales are always stored as [Targeting.Guarded] wrapping [Targeting.Locale]
 * (see [Targeting.Companion.locale]); bare [Targeting.Locale] leaves cannot
 * appear in a general [Targeting.All]<C> due to variance.
 */
internal fun <C : Context> Targeting.All<C>.localesOrEmpty(): Set<String> =
    targets.filterIsInstance<Targeting.Guarded<C, *>>()
        .mapNotNull { it.inner as? Targeting.Locale }
        .flatMapTo(linkedSetOf()) { it.ids }

/**
 * Extracts platform ids from a [Targeting.All] tree.
 *
 * Platforms are always stored as [Targeting.Guarded] wrapping [Targeting.Platform]
 * (see [Targeting.Companion.platform]); bare [Targeting.Platform] leaves cannot
 * appear in a general [Targeting.All]<C> due to variance.
 */
internal fun <C : Context> Targeting.All<C>.platformsOrEmpty(): Set<String> =
    targets.filterIsInstance<Targeting.Guarded<C, *>>()
        .mapNotNull { it.inner as? Targeting.Platform }
        .flatMapTo(linkedSetOf()) { it.ids }

/**
 * Extracts the first version range from a [Targeting.All] tree.
 *
 * Versions are always stored as [Targeting.Guarded] wrapping [Targeting.Version]
 * (see [Targeting.Companion.version]); bare [Targeting.Version] leaves cannot
 * appear in a general [Targeting.All]<C> due to variance.
 */
internal fun <C : Context> Targeting.All<C>.versionRangeOrNull(): VersionRange? =
    targets.filterIsInstance<Targeting.Guarded<C, *>>()
        .firstNotNullOfOrNull { it.inner as? Targeting.Version }
        ?.range

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
    targets.sumOf { it.customLeafCount() }

internal fun Targeting<*>.customLeafCount(): Int =
    when (this) {
        is Targeting.All<*> -> targets.sumOf { it.customLeafCount() }
        is Targeting.AnyOf<*> -> targets.sumOf { it.customLeafCount() }
        is Targeting.Not<*> -> target.customLeafCount()
        is Targeting.Custom<*> -> 1
        is Targeting.Guarded<*, *> ->
            if (inner is Targeting.Custom<*>) 1 else (inner as Targeting<*>).customLeafCount()
        else -> 0
    }

internal fun Targeting<*>.containsCustomLeaf(): Boolean = customLeafCount() > 0

internal fun Targeting<*>.containsGuardedTargeting(): Boolean =
    when (this) {
        is Targeting.All<*> -> targets.any { it.containsGuardedTargeting() }
        is Targeting.AnyOf<*> -> targets.any { it.containsGuardedTargeting() }
        is Targeting.Not<*> -> target.containsGuardedTargeting()
        is Targeting.Guarded<*, *> -> true
        else -> false
    }
