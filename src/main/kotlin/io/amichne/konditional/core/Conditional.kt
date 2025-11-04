package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.result.EvaluationResult

/**
 * Represents a feature flag that can be used to enable or disable specific functionality
 * in an application based on a given state or condition.
 *
 * @param S The type of the state or value that the feature flag produces.
 * @param C The type of the context that the feature flag evaluates against.
 */
interface Conditional<S : Any, C : Context> {
    val registry: FlagRegistry
    val key: String

    fun update(definition: FeatureFlag<S, C>) = registry.update(definition)

//    fun evaluate(context: C): S = requireNotNull(registry.featureFlag(this)).evaluate(context)
//
//    fun evaluateSafe(): EvaluationResult<S> = EvaluationResult

    companion object {
        operator fun <S : Any, C : Context> invoke(
            key: String,
            registry: FlagRegistry = FlagRegistry,
        ): Conditional<S, C> = object : Conditional<S, C> {
            override val registry: FlagRegistry = registry
            override val key: String = key
        }

        internal inline fun <reified T, S : Any, C : Context> parse(key: String): T where T : Conditional<S, C>, T : Enum<T> =
            enumValues<T>().first { it.key == key }
    }
}
