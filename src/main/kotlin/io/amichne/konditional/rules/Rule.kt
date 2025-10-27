package io.amichne.konditional.rules

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Rollout
import io.amichne.konditional.rules.evaluable.Evaluable
import io.amichne.konditional.rules.evaluable.UserClientEvaluator
import io.amichne.konditional.rules.versions.Unbounded
import io.amichne.konditional.rules.versions.VersionRange

// ---------- Rule / Condition model ----------

/**
 * Composable rule implementation that combines standard client targeting with extensible evaluation logic.
 *
 * Rule is built on the [Evaluable] abstraction and composes two evaluation strategies:
 * - **Base matching** ([userClientEvaluator]): Standard locale, platform, and version targeting
 * - **Extension matching** ([extension]): Custom evaluation logic for domain-specific rules
 *
 * Both evaluators must match for the rule to match overall, and their specificity values
 * are summed to determine rule precedence.
 *
 * ## Composition Architecture
 *
 * This design enables flexible composition:
 * ```
 * Rule.matches(context) = userClientEvaluator.matches(context) && extension.matches(context)
 * Rule.specificity() = userClientEvaluator.specificity() + extension.specificity()
 * ```
 *
 * ## Usage Examples
 *
 * Basic rule with standard targeting:
 * ```kotlin
 * Rule(
 *     rollout = Rollout.of(50.0),
 *     locales = setOf(AppLocale.EN_US),
 *     platforms = setOf(Platform.IOS),
 *     versionRange = LeftBound(Version(2, 0, 0))
 * )
 * ```
 *
 * Rule with custom extension logic:
 * ```kotlin
 * Rule(
 *     rollout = Rollout.of(100.0),
 *     extension = object : Evaluable<MyContext>() {
 *         override fun matches(context: MyContext) = context.isPremiumUser
 *         override fun specificity() = 1
 *     }
 * )
 * ```
 *
 * @param C The context type that this rule evaluates against
 * @property rollout The percentage of users (0-100) that should match this rule after all criteria are met
 * @property note Optional note or description for this rule
 * @property userClientEvaluator Evaluator for standard client targeting (locale, platform, version)
 * @property extension Additional evaluation logic that extends base matching
 *
 * @see Evaluable
 * @see io.amichne.konditional.rules.evaluable.UserClientEvaluator
 * @see io.amichne.konditional.core.Flags
 */
data class Rule<C : Context>(
    val rollout: Rollout = Rollout.of(100.0),
    val note: String? = null,
    val userClientEvaluator: UserClientEvaluator<C> = UserClientEvaluator(),
    val extension: Evaluable<C> = object : Evaluable<C>() {},
) : Evaluable<C>() {
    constructor(
        rollout: Rollout = Rollout.of(100.0),
        note: String? = null,
        locales: Set<AppLocale> = emptySet(),
        platforms: Set<Platform> = emptySet(),
        versionRange: VersionRange = Unbounded,
        extension: Evaluable<C> = object : Evaluable<C>() {},
    ) : this(rollout, note, UserClientEvaluator(locales, platforms, versionRange), extension)

    /**
     * Determines if this rule matches the given context by evaluating both composed evaluators.
     *
     * Matching requires BOTH evaluators to match:
     * - Base matching: locale, platform, and version constraints from [userClientEvaluator]
     * - Extension matching: any custom logic from [extension]
     *
     * Note: This does not check rollout eligibility - that is handled separately during flag evaluation.
     *
     * @param context The context to evaluate against
     * @return true if both [userClientEvaluator] and [extension] match the context
     */
    override fun matches(context: C): Boolean =
        userClientEvaluator.matches(context) && extension.matches(context)

    /**
     * Calculates the total specificity of this rule by summing composed evaluators.
     *
     * Specificity determines rule precedence - higher values are evaluated first.
     * The total specificity is the sum of:
     * - [userClientEvaluator] specificity (0-3 based on locale/platform/version constraints)
     * - [extension] specificity (custom value from extension logic)
     *
     * @return The total specificity value (higher is more specific)
     */
    override fun specificity(): Int =
        userClientEvaluator.specificity() + extension.specificity()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Rule<*>) return false

        if (rollout != other.rollout) return false
        if (note != other.note) return false
        if (userClientEvaluator != other.userClientEvaluator) return false
        if (extension != other.extension) return false

        return true
    }

    override fun hashCode(): Int {
        var result = rollout.hashCode()
        result = 31 * result + (note?.hashCode() ?: 0)
        result = 31 * result + userClientEvaluator.hashCode()
        result = 31 * result + extension.hashCode()
        return result
    }
}
