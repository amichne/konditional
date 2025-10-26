package io.amichne.konditional.rules

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Rollout
import io.amichne.konditional.rules.versions.Unbounded
import io.amichne.konditional.rules.versions.VersionRange

// ---------- Rule / Condition model ----------

/**
 * Base implementation of Rule that provides standard locale, platform, and version matching.
 *
 * This class guarantees that base matching logic (locales, platforms, versions) is always
 * applied for all subclasses. Custom rules can extend this class to add additional
 * matching criteria by overriding [matches].
 *
 * The [internalMatches] method is final to ensure base logic is always applied. To add custom
 * matching logic, override [matches] which is called after base matching succeeds.
 *
 * @param C The context type that this rule evaluates against
 * @property rollout The percentage of users (0-100) that should match this rule
 * @property locales Set of locales this rule applies to (empty = all locales)
 * @property platforms Set of platforms this rule applies to (empty = all platforms)
 * @property versionRange Version range this rule applies to
 * @property note Optional note or description for this rule
 *
 * @see io.amichne.konditional.core.Flags
 */
abstract class Rule<C : Context> protected constructor(
    val rollout: Rollout,
    val note: String? = null,
    val locales: Set<AppLocale> = emptySet(),
    val platforms: Set<Platform> = emptySet(),
    val versionRange: VersionRange = Unbounded,
) {

    /**
     * Determines if this rule matches the given context.
     *
     * This method is final and internal to guarantee that base matching logic (locales, platforms, versions)
     * is always applied. Override [matches] to add custom matching criteria.
     *
     * @param context The context to evaluate against
     * @return true if the context matches base criteria AND any additional criteria
     */
    internal fun matches(context: C): Boolean =
        matchesBaseAttributes(context) && matchesCustom(context)

    /**
     * Calculates the specificity of this rule.
     *
     * Specificity determines rule precedence - higher values are evaluated first.
     * This is calculated from both base attributes and custom criteria.
     *
     * @return The specificity value (higher is more specific)
     */
    internal fun specificity(): Int =
        computeBaseSpecificity() + customSpecificity()

    /**
     * Checks if the context internalMatches the base attributes (locales, platforms, versions).
     *
     * This method is private to ensure it cannot be overridden or bypassed.
     *
     * @param context The context to evaluate
     * @return true if base attributes match
     */
    private fun matchesBaseAttributes(context: C): Boolean =
        (locales.isEmpty() || context.locale in locales) &&
            (platforms.isEmpty() || context.platform in platforms) &&
            (!versionRange.hasBounds() || versionRange.contains(context.appVersion))

    /**
     * Calculates specificity from base attributes only.
     *
     * @return Base specificity value
     */
    private fun computeBaseSpecificity(): Int =
        (if (locales.isNotEmpty()) 1 else 0) +
            (if (platforms.isNotEmpty()) 1 else 0) +
            (if (versionRange.hasBounds()) 1 else 0)

    /**
     * Additional matching logic for custom rules.
     *
     * Override this method to add custom matching criteria beyond the base attributes.
     * This method is only called after base attributes have matched.
     *
     * @param context The context to evaluate against
     * @return true if additional criteria match (default: true)
     */
    protected open fun matchesCustom(context: C): Boolean = true

    /**
     * Additional specificity for custom rules.
     *
     * Override this method to add specificity values for custom attributes.
     *
     * @return Additional specificity value (default: 0)
     */
    protected open fun customSpecificity(): Int = 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Rule<*>) return false

        if (rollout != other.rollout) return false
        if (locales != other.locales) return false
        if (platforms != other.platforms) return false
        if (versionRange != other.versionRange) return false
        if (note != other.note) return false

        return true
    }

    override fun hashCode(): Int {
        var result = rollout.hashCode()
        result = 31 * result + locales.hashCode()
        result = 31 * result + platforms.hashCode()
        result = 31 * result + versionRange.hashCode()
        result = 31 * result + (note?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "Rule(rollout=$rollout, locales=$locales, platforms=$platforms, versionRange=$versionRange, note=$note)"

    companion object {
        operator fun <C : Context> invoke(
            rollout: Rollout,
            locales: Set<AppLocale> = emptySet(),
            platforms: Set<Platform> = emptySet(),
            versionRange: VersionRange = Unbounded,
            note: String? = null,
        ): Rule<C> = object : Rule<C>(
            rollout,
            note,
            locales,
            platforms,
            versionRange,
        ) {}
    }
}

//open class Rule<C : Context>(
//    override val rampUp: RampUp,
//    override val locales: Set<AppLocale> = emptySet(),
//    override val platforms: Set<Platform> = emptySet(),
//    override val versionRange: VersionRange = Unbounded,
//    override val note: String? = null,
//) : Rule<C>() {
//}
