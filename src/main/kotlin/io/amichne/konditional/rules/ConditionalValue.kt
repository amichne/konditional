package io.amichne.konditional.rules

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace

/**
 * Represents a rule paired with its target value.
 * When the rule matches a context, the paired value is returned.
 *
 * @param T The actual value type
 * @param C The context type used for rule evaluation
 * @param M The namespace type for isolation
 */
@ConsistentCopyVisibility
internal data class ConditionalValue<T : Any, C : Context, M : Namespace> private constructor(
    val rule: Rule<C>,
    val value: T
) {
    companion object {
        internal fun <T : Any, C : Context, M : Namespace> Rule<C>.targetedBy(value: T): ConditionalValue<T, C, M> =
            ConditionalValue(this, value)
    }
}
