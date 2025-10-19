package io.amichne.konditional.rules

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.RampUp
import io.amichne.konditional.context.Platform
import io.amichne.konditional.rules.versions.Unbounded
import io.amichne.konditional.rules.versions.VersionRange

// ---------- Rule / Condition model ----------

/**
 * Rule interface for defining feature flag evaluation rules.
 *
 * This interface allows for custom rule implementations that can extend
 * the base matching logic to support custom context types with additional
 * properties and matching criteria.
 *
 * @param C The context type that this rule evaluates against
 * @property rampUp The percentage of users (0-100) that should match this rule
 * @property note Optional note or description for this rule
 *
 * @see BaseRule
 * @see io.amichne.konditional.core.Flags
 */
interface Rule<C : Context> {
    val rampUp: RampUp
    val note: String?

    /**
     * Determines if this rule matches the given context.
     *
     * @param context The context to evaluate against
     * @return true if the context matches this rule's criteria
     */
    fun matches(context: C): Boolean

    /**
     * Calculates the specificity of this rule.
     *
     * Higher specificity values indicate more specific rules, which take
     * precedence over less specific rules when multiple rules match.
     *
     * @return The specificity value (higher is more specific)
     */
    fun specificity(): Int
}

/**
 * Base implementation of Rule that provides standard locale, platform, and version matching.
 *
 * This is the default rule implementation used by the framework. Custom rules can
 * extend this class or implement the Rule interface directly to add custom matching logic.
 *
 * @param C The context type that this rule evaluates against
 * @property rampUp The percentage of users (0-100) that should match this rule
 * @property locales Set of locales this rule applies to (empty = all locales)
 * @property platforms Set of platforms this rule applies to (empty = all platforms)
 * @property versionRange Version range this rule applies to
 * @property note Optional note or description for this rule
 *
 * @see Rule
 */
data class BaseRule<C : Context>(
    override val rampUp: RampUp,
    val locales: Set<AppLocale> = emptySet(),
    val platforms: Set<Platform> = emptySet(),
    val versionRange: VersionRange = Unbounded,
    override val note: String? = null,
) : Rule<C> {
    init {
        require(rampUp.value in 0.0..100.0) { "coveragePct out of range" }
    }

    override fun matches(context: C): Boolean =
        (locales.isEmpty() || context.locale in locales) &&
            (platforms.isEmpty() || context.platform in platforms) &&
            (!versionRange.hasBounds() || versionRange.contains(context.appVersion))

    override fun specificity(): Int =
        (if (locales.isNotEmpty()) 1 else 0) +
            (if (platforms.isNotEmpty()) 1 else 0) +
            (if (versionRange.hasBounds()) 1 else 0)
}
