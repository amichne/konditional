package io.amichne.konditional.rules

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.evaluation.EvaluationScope

/**
 * Represents a rule paired with its target value.
 * When the rule matches a contextFn, the paired value is returned.
 *
 * @param T The actual value type
 * @param C The contextFn type used for rule evaluation
 */
@ConsistentCopyVisibility
@KonditionalInternalApi
data class ConditionalValue<T : Any, C : Context, M : Namespace> private constructor(
    val rule: Rule<C>,
    val value: RuleValue<T, C, M>,
) {
    fun resolve(context: C): T = value.resolve(context)

    fun resolve(scope: EvaluationScope<C, M>): T = value.resolve(scope)

    companion object {
        internal fun <T : Any, C : Context, M : Namespace> Rule<C>.targetedBy(
            value: T,
        ): ConditionalValue<T, C, M> =
            ConditionalValue(this, RuleValue.static(value))

        internal fun <T : Any, C : Context, M : Namespace> Rule<C>.targetedBy(
            value: RuleValue<T, C, M>,
        ): ConditionalValue<T, C, M> =
            ConditionalValue(this, value)

        internal fun <T : Any, C : Context, M : Namespace> Rule<C>.targetedBy(
            value: C.() -> T,
        ): ConditionalValue<T, C, M> =
            ConditionalValue(this, RuleValue.contextual(value))
    }
}
