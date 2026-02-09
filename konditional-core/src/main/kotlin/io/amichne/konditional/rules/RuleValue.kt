package io.amichne.konditional.rules

import io.amichne.konditional.context.Context

sealed interface RuleValue<out T : Any, in C : Context> {
    fun resolve(context: C): T

    @ConsistentCopyVisibility
    data class Fixed<out T : Any, in C : Context> @PublishedApi internal constructor(
        val value: T,
    ) : RuleValue<T, C> {
        override fun resolve(context: C): T = value
    }

    @ConsistentCopyVisibility
    data class Contextual<out T : Any, in C : Context> @PublishedApi internal constructor(
        val resolver: C.() -> T,
    ) : RuleValue<T, C> {
        override fun resolve(context: C): T = context.resolver()
    }

    companion object {
        fun <T : Any, C : Context> fixed(value: T): RuleValue<T, C> = Fixed(value)

        fun <T : Any, C : Context> contextual(
            resolver: C.() -> T,
        ): RuleValue<T, C> = Contextual(resolver)
    }
}
