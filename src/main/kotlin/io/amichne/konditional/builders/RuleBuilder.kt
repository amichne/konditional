package io.amichne.konditional.builders

import io.amichne.konditional.builders.versions.VersionRangeBuilder
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.RampUp
import io.amichne.konditional.context.Platform
import io.amichne.konditional.core.FeatureFlagDsl
import io.amichne.konditional.rules.BaseRule
import io.amichne.konditional.rules.Rule
import io.amichne.konditional.rules.versions.Unbounded
import io.amichne.konditional.rules.versions.VersionRange

/**
 * A builder class for constructing rules with a specific context type.
 *
 * This class is open to allow custom rule builders that can add additional
 * properties and build custom Rule implementations. Override [build] to
 * create your custom rule type.
 *
 * Example:
 * ```kotlin
 * class EnterpriseRuleBuilder<C : EnterpriseContext> : RuleBuilder<C>() {
 *     var requiredTier: SubscriptionTier? = null
 *     var requiredRole: UserRole? = null
 *
 *     override fun build(): Rule<C> = EnterpriseRule(
 *         baseRule = super.build() as BaseRule<C>,
 *         requiredTier = requiredTier,
 *         requiredRole = requiredRole
 *     )
 * }
 * ```
 *
 * @param C The type of the context that the rules will evaluate against.
 */
@FeatureFlagDsl
open class RuleBuilder<C : Context> {
    var rampUp: RampUp? = null
    protected val locales = linkedSetOf<AppLocale>()
    protected val platforms = linkedSetOf<Platform>()
    protected var range: VersionRange = Unbounded
    protected var note: String? = null

    fun locales(vararg appLocales: AppLocale) {
        locales += appLocales
    }

    @FeatureFlagDsl
    fun platforms(vararg ps: Platform) {
        platforms += ps
    }

    fun versions(build: VersionRangeBuilder.() -> Unit) {
        range = VersionRangeBuilder().apply(build).build()
    }

    fun note(text: String) {
        note = text
    }

    /**
     * Builds a Rule instance. Override this method in custom builders to create
     * custom rule implementations.
     *
     * @return A Rule instance (BaseRule by default)
     */
    open fun build(): Rule<C> =
        BaseRule(
            rampUp = rampUp ?: RampUp.default,
            locales = locales,
            platforms = platforms,
            versionRange = range,
            note = note,
        )
}
