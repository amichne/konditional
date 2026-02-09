package io.amichne.konditional.rules

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.context.Context

/**
 * Represents a rule paired with its target value.
 * When the rule matches a contextFn, the paired value is returned.
 *
 * @param T The actual value type
 * @param C The contextFn type used for rule evaluation
 */
@ConsistentCopyVisibility
@KonditionalInternalApi
data class ConditionalValue<T : Any, C : Context> private constructor(
    val rule: Rule<C>,
    val value: RuleValue<T, C>,
) {
    fun resolve(context: C): T = value.resolve(context)

    companion object {
        internal fun <T : Any, C : Context> Rule<C>.targetedBy(value: T): ConditionalValue<T, C> =
            ConditionalValue(this, RuleValue.static(value))

        internal fun <T : Any, C : Context> Rule<C>.targetedBy(
            value: RuleValue<T, C>,
        ): ConditionalValue<T, C> =
            ConditionalValue(this, value)

        internal fun <T : Any, C : Context> Rule<C>.targetedBy(
            value: C.() -> T,
        ): ConditionalValue<T, C> =
            ConditionalValue(this, RuleValue.contextual(value))
    }
}
