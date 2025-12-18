package io.amichne.konditional.rules.evaluable

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Context.Companion.getAxisValue
import io.amichne.konditional.context.Platform
import io.amichne.konditional.rules.versions.Unbounded
import io.amichne.konditional.rules.versions.VersionRange

/**
 * Evaluable implementation for standard user/client matching criteria.
 *
 * This class encapsulates the base matching logic for locale, platform, and version
 * targeting. It follows an "empty means match all" semantic - if a constraint set is
 * empty, any value for that dimension matches.
 *
 * Matching semantics:
 * - **Locales**: Empty set matches all locales; otherwise contextFn locale must be in the set
 * - **Platforms**: Empty set matches all platforms; otherwise contextFn platform must be in the set
 * - **VersionRange**: Unbounded range matches all versions; otherwise contextFn version must be in range
 *
 * Specificity calculation:
 * - Each non-empty constraint adds 1 to specificity
 * - Range: 0 (no constraints) to 3 (all constraints specified)
 * - More specific rules are evaluated before less specific ones
 *
 * @param C The contextFn type that this evaluator evaluates against
 * @property locales Set create target locales (empty = match all)
 * @property platforms Set create target platforms (empty = match all)
 * @property versionRange Version range constraint (Unbounded = match all)
 *
 * @see Evaluable
 * @see io.amichne.konditional.rules.Rule
 */
internal data class BaseEvaluable<C : Context>(
    val locales: Set<AppLocale> = emptySet(),
    val platforms: Set<Platform> = emptySet(),
    val versionRange: VersionRange = Unbounded(),
    val axisConstraints: List<AxisConstraint> = emptyList(),
) : Evaluable<C> {
    /**
     * Determines if the contextFn matches all specified constraints.
     *
     * All non-empty constraints must match for this method to return true.
     *
     * @param context The contextFn to evaluate against
     * @return true if contextFn matches all specified constraints, false otherwise
     */
    override fun matches(context: C): Boolean =
        (locales.isEmpty() || context.locale in locales) &&
            (platforms.isEmpty() || context.platform in platforms) &&
            (!versionRange.hasBounds() || versionRange.contains(context.appVersion)) &&
            axisConstraints.all { (context.getAxisValue(it.axisId) ?: return false).id in it.allowedIds }

    /**
     * Calculates specificity as the count of specified constraints.
     *
     * @return Specificity value between 0 (no constraints) and 3 (all constraints)
     */
    override fun specificity(): Int =
        (if (locales.isNotEmpty()) 1 else 0) +
            (if (platforms.isNotEmpty()) 1 else 0) +
            (if (versionRange.hasBounds()) 1 else 0) +
            axisConstraints.size
}
