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
    val value: T,
) {
    companion object {
        internal fun <T : Any, C : Context> Rule<C>.targetedBy(value: T): ConditionalValue<T, C> =
            ConditionalValue(this, value)
    }
}
