package io.amichne.konditional.rules

import io.amichne.konditional.context.Context

@ConsistentCopyVisibility
data class Surjection<S : Any, C : Context> private constructor(val rule: Rule<C>, val value: S) {
    companion object {
        internal fun <S : Any, C : Context> Rule<C>.boundedBy(value: S): Surjection<S, C> = Surjection(this, value)
    }
}
