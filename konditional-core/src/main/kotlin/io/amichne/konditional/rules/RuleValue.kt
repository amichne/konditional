package io.amichne.konditional.rules

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.evaluation.EvaluationScope

/**
 * Encapsulates a rule's value resolution strategy.
 *
 * Values can be static (compile-time) or derived from the evaluation context.
 */
sealed interface RuleValue<out T : Any, in C : Context, M : Namespace> {
    fun resolve(context: C): T

    fun resolve(scope: EvaluationScope<C, M>): T = resolve(scope.context)

    fun serialization(): Serialization<T>

    sealed interface Serialization<out T> {
        data class Supported<out T>(val value: T) : Serialization<T>

        data class Unsupported<out T>(val description: String) : Serialization<T>
    }

    data class Static<out T : Any, in C : Context, M : Namespace>(
        val value: T,
    ) : RuleValue<T, C, M> {
        override fun resolve(context: C): T = value

        override fun serialization(): Serialization<T> = Serialization.Supported(value)
    }

    data class Contextual<out T : Any, in C : Context, M : Namespace>(
        val compute: C.() -> T,
    ) : RuleValue<T, C, M> {
        override fun resolve(context: C): T = context.compute()

        override fun serialization(): Serialization<T> =
            Serialization.Unsupported("Contextual rule values cannot be serialized.")
    }

    data class Scoped<out T : Any, in C : Context, M : Namespace>(
        val compute: EvaluationScope<C, M>.() -> T,
    ) : RuleValue<T, C, M> {
        override fun resolve(context: C): T =
            error("Scoped rule values must be resolved with an EvaluationScope.")

        override fun resolve(scope: EvaluationScope<C, M>): T = scope.compute()

        override fun serialization(): Serialization<T> =
            Serialization.Unsupported("Scoped rule values cannot be serialized.")
    }

    companion object {
        fun <T : Any, C : Context, M : Namespace> static(value: T): RuleValue<T, C, M> = Static(value)

        fun <T : Any, C : Context, M : Namespace> contextual(
            compute: C.() -> T,
        ): RuleValue<T, C, M> =
            Contextual(compute)

        fun <T : Any, C : Context, M : Namespace> scoped(
            compute: EvaluationScope<C, M>.() -> T,
        ): RuleValue<T, C, M> =
            Scoped(compute)
    }
}
