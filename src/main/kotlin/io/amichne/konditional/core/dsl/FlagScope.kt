package io.amichne.konditional.core.dsl

import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.types.EncodableValue
import io.amichne.konditional.kontext.Kontext
import io.amichne.konditional.rules.Rule
import kotlin.experimental.ExperimentalTypeInference

/**
 * DSL scope for flag configuration.
 *
 * This interface defines the public API for configuring individual feature flags.
 * Users cannot instantiate implementations of this interface directly - it is only
 * available as a receiver in DSL blocks through internal implementations.
 *
 * Example usage:
 * ```kotlin
 * MyFeature.FEATURE_A with {
 *     default(true)
 *     salt("v2")
 *     rule {
 *         platforms(Platform.IOS)
 *         rampUp {  Rampup.of(50.0) }
 *     }.returns(false)
 * }
 * ```
 *
 * @param S The EncodableValue type wrapping the actual value
 * @param T The actual value type
 * @param C The kontextFn type the flag evaluates against
 * @since 0.0.2
 */
@OptIn(ExperimentalTypeInference::class)
@KonditionalDsl
interface FlagScope<S : EncodableValue<T>, T : Any, C : Kontext<M>, M : Namespace> {

    fun active(block: () -> Boolean): Unit

    /**
     * Sets the default value for the flag.
     *
     * This value is returned when no rules match the current kontextFn.
     *
     * @param value The default value to assign to the flag
     */
    fun default(value: T)

    /**
     * Sets the salt value for the flag.
     *
     * Salt is used in hash-based rampUp calculations. Changing the salt
     * will redistribute users across rampUp percentages.
     *
     * @param value The salt value (default is "v1")
     */
    fun salt(value: String)

    /**
     * Defines a targeting rule for this flag.
     *
     * Rules determine which users receive which values based on kontextFn properties.
     *
     * Example:
     * ```kotlin
     * rule {
     *     platforms(Platform.IOS)
     *     locales(AppLocale.UNITED_STATES)
     *     rampUp {  Rampup.of(50.0) }
     * }.returns(true)
     * ```
     *
     * @param build DSL block for configuring the rule
     * @return A Rule object that can be associated with a value using `returns`
     */
    fun rule(build: RuleScope<C>.() -> Unit): Rule<C>

    /**
     * Associates a rule with a specific value.
     *
     * When the rule matches the kontextFn, the flag will return this value.
     *
     * Example:
     * ```kotlin
     * rule { platforms(Platform.IOS) }.returns(true)
     * ```
     *
     * @param value The value to return when the rule matches
     */
    infix fun Rule<C>.returns(value: T)
}

interface EncodableFlagScope<S : EncodableValue<T>, T : Any, C : Kontext<M>, M : Namespace> :
    FlagScope<S, T, C, M> {
    /**
     * Sets the default value for the flag using an EncodableValue.
     *
     * This value is returned when no rules match the current kontextFn.
     *
     * @param value The default EncodableValue to assign to the flag
     */
    fun default(value: S)

    /**
     * Associates a rule with a specific EncodableValue.
     *
     * When the rule matches the kontextFn, the flag will return this value.
     *
     * Example:
     * ```kotlin
     * rule { platforms(Platform.IOS) }.returns(BooleanEncodableValue(true))
     * ```
     *
     * @param value The EncodableValue to return when the rule matches
     */
    infix fun Rule<C>.returns(value: S)
}
