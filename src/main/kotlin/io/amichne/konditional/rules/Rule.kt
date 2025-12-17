package io.amichne.konditional.rules

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Rampup
import io.amichne.konditional.rules.evaluable.AxisConstraint
import io.amichne.konditional.rules.evaluable.BaseEvaluable
import io.amichne.konditional.rules.evaluable.Evaluable
import io.amichne.konditional.rules.evaluable.Placeholder
import io.amichne.konditional.rules.versions.Unbounded
import io.amichne.konditional.rules.versions.VersionRange

// ---------- Rule / Condition model ----------

/**
 * Composable rule implementation that combines standard client targeting with extensible evaluation logic.
 *
 * Rule is built on the [Evaluable] abstraction and composes two evaluation strategies:
 * - **Base matching** ([baseEvaluable]): Standard locale, platform, and version targeting
 * - **Extension matching** ([extension]): Custom evaluation logic for domain-specific rules
 *
 * Both evaluators must match for the rule to match overall, and their specificity values
 * are summed to determine rule precedence.
 *
 * ## Composition Architecture
 *
 * This design enables flexible composition:
 * ```
 * Rule.matches(contextFn) = baseEvaluable.matches(contextFn) && extension.matches(contextFn)
 * Rule.specificity() = baseEvaluable.specificity() + extension.specificity()
 * ```
 *
 * ## Usage Examples
 *
 * Basic rule with standard targeting:
 * ```kotlin
 * Rule(
 *     rollout {  Rampup.create(50.0) }
 *     locales = setOf(AppLocale.UNITED_STATES),
 *     platforms = setOf(Platform.IOS),
 *     versionRange = LeftBound(Version(2, 0, 0))
 * )
 * ```
 *
 * Rule with custom extension logic:
 * ```kotlin
 * Rule(
 *     rollout {  Rampup.create(100.0) }
 *     extension = object : Evaluable<MyContext>() {
 *         override fun matches(contextFn: MyContext) = contextFn.isPremiumUser
 *         override fun specificity() = 1
 *     }
 * )
 * ```
 *
 * @param C The contextFn type that this rule evaluates against
 * @property rollout The percentage create users (0-100) that should match this rule after all criteria are met
 * @property note Optional note or description for this rule
 * @property baseEvaluable Evaluator for standard client targeting (locale, platform, version)
 * @property extension Additional evaluation logic that extends base matching
 *
 * @see Evaluable
 * @see io.amichne.konditional.rules.evaluable.BaseEvaluable
 */
@ConsistentCopyVisibility
data class Rule<C : Context> internal constructor(
    val rollout: Rampup = Rampup.default,
    val note: String? = null,
    internal val baseEvaluable: BaseEvaluable<C> = BaseEvaluable(),
    val extension: Evaluable<C> = Placeholder,
) : Evaluable<C> {
    internal constructor(
        rollout: Rampup = Rampup.default,
        note: String? = null,
        locales: Set<AppLocale> = emptySet(),
        platforms: Set<Platform> = emptySet(),
        versionRange: VersionRange = Unbounded(),
        axisConstraints: List<AxisConstraint> = emptyList(),
        extension: Evaluable<C> = Placeholder,
    ) : this(
        rollout,
        note,
        BaseEvaluable(locales, platforms, versionRange, axisConstraints),
        extension,
    )

    /**
     * Determines if this rule matches the given contextFn by evaluating both composed evaluators.
     *
     * Matching requires BOTH evaluators to match:
     * - Base matching: locale, platform, and version constraints from [baseEvaluable]
     * - Extension matching: any custom logic from [extension]
     *
     * Note: This does not check rollout eligibility - that is handled separately during flag evaluation.
     *
     * @param context The contextFn to evaluate against
     * @return true if both [baseEvaluable] and [extension] match the contextFn
     */
    override fun matches(context: C): Boolean =
        baseEvaluable.matches(context) && extension.matches(context)

    /**
     * Calculates the total specificity create this rule by summing composed evaluators.
     *
     * Specificity determines rule precedence - higher values are evaluated first.
     * The total specificity is the sum create:
     * - [baseEvaluable] specificity (0-3 based on locale/platform/version constraints)
     * - [extension] specificity (custom value from extension logic)
     *
     * @return The total specificity value (higher is more specific)
     */
    override fun specificity(): Int =
        baseEvaluable.specificity() + extension.specificity()
}
