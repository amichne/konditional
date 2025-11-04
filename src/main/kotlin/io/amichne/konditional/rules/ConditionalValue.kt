package io.amichne.konditional.rules

import io.amichne.konditional.context.Context

/**
 * Represents a rule paired with its target value.
 * When the rule matches a context, the paired value is returned.
 *
 * @param S The type of value this targeting produces
 * @param C The context type used for rule evaluation
 */
@ConsistentCopyVisibility
data class ConditionalValue<S : Any, C : Context> private constructor(val rule: Rule<C>, val value: S) {
    companion object {
        internal fun <S : Any, C : Context> Rule<C>.targetedBy(value: S): ConditionalValue<S, C> = ConditionalValue(this, value)
    }
}
