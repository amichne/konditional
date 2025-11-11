package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.result.EvaluationResult

/**
 * Represents a feature that can be used to enable or disable specific functionality
 * in an application based on a given state or condition.
 *
 * @param S The type of the state or value that the feature produces.
 * @param C The type of the context that the feature evaluates against.
 */
interface Feature<S : Any, C : Context> {
    val registry: FlagRegistry
    val key: String

    fun update(definition: FlagDefinition<S, C>) = registry.update(definition)

//    fun evaluate(context: C): S = requireNotNull(registry.featureFlag(this)).evaluate(context)
//
//    fun evaluateSafe(): EvaluationResult<S> = EvaluationResult

    companion object {
        operator fun <S : Any, C : Context> invoke(
            key: String,
            registry: FlagRegistry = FlagRegistry,
        ): Feature<S, C> = object : Feature<S, C> {
            override val registry: FlagRegistry = registry
            override val key: String = key
        }

        internal inline fun <reified T, S : Any, C : Context> parse(key: String): T where T : Feature<S, C>, T : Enum<T> =
            enumValues<T>().first { it.key == key }
    }
}
