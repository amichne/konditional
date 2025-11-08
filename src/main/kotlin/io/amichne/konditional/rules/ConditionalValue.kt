package io.amichne.konditional.rules

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.types.EncodableValue

/**
 * Represents a rule paired with its target value.
 * When the rule matches a context, the paired value is returned.
 *
 * @param S The type of value this targeting produces
 * @param C The context type used for rule evaluation
 */
@ConsistentCopyVisibility
data class ConditionalValue<S : EncodableValue<T>, T : Any, C : Context> private constructor(
    val rule: Rule<C>,
    val value: T,
) {
    companion object {
        internal fun <S : EncodableValue<T>, T : Any, C : Context> Rule<C>.targetedBy(value: T): ConditionalValue<S, T, C> =
            ConditionalValue(this, value)
    }
}
