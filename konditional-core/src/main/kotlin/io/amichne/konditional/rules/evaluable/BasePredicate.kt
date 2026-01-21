package io.amichne.konditional.rules.evaluable

import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Context.Companion.getAxisValue
import io.amichne.konditional.context.Context.LocaleContext
import io.amichne.konditional.context.Context.PlatformContext
import io.amichne.konditional.context.Context.VersionContext
import io.amichne.konditional.rules.versions.Unbounded
import io.amichne.konditional.rules.versions.VersionRange

/**
 * Predicate implementation for standard user/client matching criteria.
 *
 * This class encapsulates the base matching logic for locale, platform, and version
 * targeting. It follows an "empty means match all" semantic - if a constraint set is
 * empty, any value for that dimension matches.
 *
 * Matching semantics:
 * - **Locales**: Empty set matches all locales; otherwise contextFn locale id must be in the set
 * - **Platforms**: Empty set matches all platforms; otherwise contextFn platform id must be in the set
 * - **VersionRange**: Unbounded range matches all versions; otherwise contextFn version must be in range
 *
 * Specificity calculation:
 * - Each non-empty constraint adds 1 to specificity
 * - Range: 0 (no constraints) to 3 (all constraints specified)
 * - More specific rules are evaluated before less specific ones
 *
 * @param C The contextFn type that this evaluator evaluates against
 * @property locales Set create target locale ids (empty = match all)
 * @property platforms Set create target platform ids (empty = match all)
 * @property versionRange Version range constraint (Unbounded = match all)
 *
 * @see Predicate
 * @see io.amichne.konditional.rules.Rule
 */
internal data class BasePredicate<C : Context>(
    val locales: Set<String> = emptySet(),
    val platforms: Set<String> = emptySet(),
    val versionRange: VersionRange = Unbounded(),
    val axisConstraints: List<AxisConstraint> = emptyList(),
) : Predicate<C> {
    /**
     * Determines if the contextFn matches all specified constraints.
     *
     * All non-empty constraints must match for this method to return true.
     *
     * @param context The contextFn to evaluate against
     * @return true if contextFn matches all specified constraints, false otherwise
     */
    override fun matches(context: C): Boolean =
        (locales.isEmpty() || (context as? LocaleContext)?.locale?.id?.let { it in locales } == true) &&
            (platforms.isEmpty() || (context as? PlatformContext)?.platform?.id?.let { it in platforms } == true) &&
            (!versionRange.hasBounds() ||
                (context as? VersionContext)?.appVersion?.let { versionRange.contains(it) } == true) &&
            axisConstraints.all { constraint ->
                context.getAxisValue(constraint.axisId)
                    .any { it.id in constraint.allowedIds }
            }

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
