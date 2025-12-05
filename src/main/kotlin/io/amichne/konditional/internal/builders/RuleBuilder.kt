package io.amichne.konditional.internal.builders

import io.amichne.konditional.core.dsl.KonditionalDsl
import io.amichne.konditional.core.dsl.RuleScope
import io.amichne.konditional.core.dsl.VersionRangeScope
import io.amichne.konditional.internal.builders.versions.VersionRangeBuilder
import io.amichne.konditional.kontext.AppLocale
import io.amichne.konditional.kontext.Kontext
import io.amichne.konditional.kontext.Platform
import io.amichne.konditional.kontext.Rampup
import io.amichne.konditional.rules.Rule
import io.amichne.konditional.rules.evaluable.Evaluable
import io.amichne.konditional.rules.evaluable.Evaluable.Companion.factory
import io.amichne.konditional.rules.evaluable.Placeholder
import io.amichne.konditional.rules.versions.Unbounded
import io.amichne.konditional.rules.versions.VersionRange
import io.amichne.konditional.util.NonEmptySet
import io.amichne.konditional.util.NonEmptySet.Companion.nonEmptySetOf

/**
 * Internal implementation of [RuleScope].
 *
 * This class is the internal implementation of the rule configuration DSL scope.
 * Users interact with the public [RuleScope] interface,
 * not this implementation directly.
 *
 * @param C The type of the kontextFn that the rules will evaluate against.
 * @constructor Internal constructor - users cannot instantiate this class directly.
 */
@KonditionalDsl
@PublishedApi
internal data class RuleBuilder<C : Kontext<*>>(
    private var extension: Evaluable<C> = Placeholder,
    private var note: String? = null,
    private var range: VersionRange = Unbounded(),
    private var platforms: NonEmptySet<Platform> = nonEmptySetOf(Platform.IOS, Platform.ANDROID),
    private val locales: LinkedHashSet<AppLocale> = linkedSetOf(),
    private var rollout: Rampup? = null,
) : RuleScope<C> {

    /**
     * Implementation of [RuleScope.locales].
     */
    override fun locales(vararg appLocales: AppLocale) {
        locales += appLocales
    }

    /**
     * Implementation of [RuleScope.platforms].
     */
    @Deprecated("Use ios() or android() instead", replaceWith = ReplaceWith(""))
    @KonditionalDsl
    override fun platforms(vararg ps: Platform) {
        platforms = nonEmptySetOf(*ps)
    }

    /**
     * Implementation of [RuleScope.ios].
     *
     * Registers iOS as the only target platform for this rule.
     *
     * By default, both iOS and Android are targeted.
     *
     */
    override fun ios() {
        platforms = nonEmptySetOf(Platform.IOS)
    }

    /**
     * Implementation of [RuleScope.android].
     *
     * Registers Android as the only target platform for this rule.
     *
     * By default, both iOS and Android are targeted.
     *
     */
    override fun android() {
        platforms = nonEmptySetOf(Platform.ANDROID)
    }

    /**
     * Implementation of [RuleScope.versions] that delegates to [VersionRangeBuilder].
     */
    override fun versions(build: VersionRangeScope.() -> Unit) {
        range = VersionRangeBuilder().apply(build).build()
    }

    /**
     *  Implementation of [RuleScope.extension].
     */
    override fun extension(block: C.() -> Boolean) {
        extension = factory { block(it) }
    }

    /**
     * Implementation of [RuleScope.note].
     */
    override fun note(text: String) {
        note = text
    }

    override fun rampUp(function: () -> Number) {
        this.rollout = Rampup.of(function().toDouble())
    }

    /**
     * Builds a Rule instance. Override this method in custom builders to create
     * custom rule implementations. Internal method - not intended for direct use.
     *
     * @return A Rule instance (Rule by default)
     */
    internal fun build(): Rule<C> =
        Rule(
            rollout = rollout ?: Rampup.default,
            locales = locales,
            platforms = platforms,
            versionRange = range,
            note = note,
            extension = extension,
        )
}
