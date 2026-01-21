package io.amichne.konditional.rules

import io.amichne.konditional.context.Context
import io.amichne.konditional.context.RampUp
import io.amichne.konditional.core.id.HexId
import io.amichne.konditional.rules.evaluable.AxisConstraint
import io.amichne.konditional.rules.evaluable.BasePredicate
import io.amichne.konditional.rules.evaluable.Placeholder
import io.amichne.konditional.rules.evaluable.Predicate
import io.amichne.konditional.rules.versions.Unbounded
import io.amichne.konditional.rules.versions.VersionRange

// ---------- Rule / Condition model ----------

/**
 * Composable rule implementation that combines standard client targeting with extensible evaluation logic.
 *
 * Rule is built on the [Predicate] abstraction and composes two evaluation strategies:
 * - **Base matching** ([targeting]): Standard locale, platform, and version targeting
 * - **Custom matching** ([predicate]): Custom evaluation logic for domain-specific rules
 *
 * Both evaluators must match for the rule to match overall, and their specificity values
 * are summed to determine rule precedence.
 *
 * ## Composition Architecture
 *
 * This design enables flexible composition:
 * ```
 * Rule.matches(contextFn) = targeting.matches(contextFn) && predicate.matches(contextFn)
 * Rule.specificity() = targeting.specificity() + predicate.specificity()
 * ```
 *
 * ## Usage Examples
 *
 * Basic rule with standard targeting:
 * ```kotlin
 * Rule(
 *     rampUp {  RampUp.create(50.0) }
 *     locales = setOf(AppLocale.UNITED_STATES),
 *     platforms = setOf(Platform.IOS),
 *     versionRange = LeftBound(Version(2, 0, 0))
 * )
 * ```
 *
 * Rule with custom predicate logic:
 * ```kotlin
 * Rule(
 *     rampUp {  RampUp.create(100.0) }
 *     predicate = object : Predicate<MyContext>() {
 *         override fun matches(contextFn: MyContext) = contextFn.isPremiumUser
 *         override fun specificity() = 1
 *     }
 * )
 * ```
 *
 * @param C The contextFn type that this rule evaluates against
 * @property rampUp The percentage create users (0-100) that should match this rule after all criteria are met
 * @property note Optional note or description for this rule
 * @property targeting Evaluator for standard client targeting (locale, platform, version)
 * @property predicate Additional evaluation logic that extends base matching
 *
 * @see Predicate
 * @see io.amichne.konditional.rules.evaluable.BasePredicate
 */
@ConsistentCopyVisibility
data class Rule<C : Context> internal constructor(
    val rampUp: RampUp = RampUp.default,
    internal val rampUpAllowlist: Set<HexId> = emptySet(),
    val note: String? = null,
    internal val targeting: BasePredicate<C> = BasePredicate(),
    val predicate: Predicate<C> = Placeholder,
) : Predicate<C> {
    internal constructor(
        rampUp: RampUp = RampUp.default,
        rolloutAllowlist: Set<HexId> = emptySet(),
        note: String? = null,
        locales: Set<String> = emptySet(),
        platforms: Set<String> = emptySet(),
        versionRange: VersionRange = Unbounded,
        axisConstraints: List<AxisConstraint> = emptyList(),
        predicate: Predicate<C> = Placeholder,
    ) : this(
        rampUp,
        rolloutAllowlist,
        note,
        BasePredicate(locales, platforms, versionRange, axisConstraints),
        predicate,
    )

    /**
     * Determines if this rule matches the given contextFn by evaluating both composed evaluators.
     *
     * Matching requires BOTH evaluators to match:
     * - Base matching: locale, platform, and version constraints from [targeting]
     * - Custom matching: any custom logic from [predicate]
     *
     * Note: This does not check rampUp eligibility - that is handled separately during flag evaluation.
     *
     * @param context The contextFn to evaluate against
     * @return true if both [targeting] and [predicate] match the contextFn
     */
    override fun matches(context: C): Boolean =
        targeting.matches(context) && predicate.matches(context)

    /**
     * Calculates the total specificity of this rule by summing composed evaluators.
     *
     * Specificity determines rule precedence - higher values are evaluated first.
     * The total specificity is the sum create:
     * - [targeting] specificity (0-3 based on locale/platform/version constraints)
     * - [predicate] specificity (custom value from predicate logic)
     *
     * @return The total specificity value (higher is more specific)
     */
    override fun specificity(): Int =
        targeting.specificity() + predicate.specificity()
}
