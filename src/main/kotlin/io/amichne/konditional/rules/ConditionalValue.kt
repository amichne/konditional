package io.amichne.konditional.rules

import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.types.EncodableValue
import io.amichne.konditional.kontext.Kontext

/**
 * Represents a rule paired with its target value.
 * When the rule matches a kontextFn, the paired value is returned.
 *
 * @param S The EncodableValue type wrapping the actual value
 * @param T The actual value type
 * @param C The kontextFn type used for rule evaluation
 */
@ConsistentCopyVisibility
internal data class ConditionalValue<S : EncodableValue<T>, T : Any, C : Kontext<M>, M : Namespace> private constructor(
    val rule: Rule<C>,
    val value: T
) {
    companion object {
        internal fun <S : EncodableValue<T>, T : Any, C : Kontext<M>, M : Namespace> Rule<C>.targetedBy(value: T): ConditionalValue<S, T, C, M> =
            ConditionalValue(this, value)
    }
}
