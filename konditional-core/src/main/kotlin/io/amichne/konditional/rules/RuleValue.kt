package io.amichne.konditional.rules

import io.amichne.konditional.context.Context

/**
 * Encapsulates a rule's value resolution strategy.
 *
 * Values can be static (compile-time) or derived from the evaluation context.
 */
sealed interface RuleValue<out T : Any, in C : Context> {
    fun resolve(context: C): T

    fun serialization(): Serialization<T>

    sealed interface Serialization<out T> {
        data class Supported<out T>(val value: T) : Serialization<T>

        data class Unsupported<out T>(val description: String) : Serialization<T>
    }

    data class Static<out T : Any, in C : Context>(val value: T) : RuleValue<T, C> {
        override fun resolve(context: C): T = value

        override fun serialization(): Serialization<T> = Serialization.Supported(value)
    }

    data class Contextual<out T : Any, in C : Context>(
        val compute: C.() -> T,
    ) : RuleValue<T, C> {
        override fun resolve(context: C): T = context.compute()

        override fun serialization(): Serialization<T> =
            Serialization.Unsupported("Contextual rule values cannot be serialized.")
    }

    companion object {
        fun <T : Any, C : Context> static(value: T): RuleValue<T, C> = Static(value)

        fun <T : Any, C : Context> contextual(compute: C.() -> T): RuleValue<T, C> =
            Contextual(compute)
    }
}
