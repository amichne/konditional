package io.amichne.konditional.context

import io.amichne.konditional.core.Feature
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.FlagRegistry
import io.amichne.konditional.core.internal.SingletonFlagRegistry
import io.amichne.konditional.core.types.EncodableValue

/**
 * Evaluates a specific feature flag in the context of this [Context].
 *
 * This extension function provides convenient access to flag evaluation
 * for any [io.amichne.konditional.core.FlagRegistry] implementation.
 *
 * @param key The feature flag to evaluate
 * @param registry The [io.amichne.konditional.core.FlagRegistry] to use (defaults to [SingletonFlagRegistry])
 * @return The evaluated value of type [T]
 * @throws IllegalStateException if the flag is not found in the registry
 * @param S The EncodableValue type wrapping the actual value
 * @param T The actual value type
 * @param C The type of the context
 */
@Suppress("UNCHECKED_CAST")
fun <S : EncodableValue<T>, T : Any, C : Context> C.evaluate(
    key: Feature<S, T, C>,
    registry: FlagRegistry = FlagRegistry
): T {
    val flag = registry.featureFlag(key)
        ?: throw IllegalStateException("Flag not found: ${key.key}")
    return flag.evaluate(this)
}

/**
 * Evaluates all feature flags in the context of this [Context].
 *
 * This extension function evaluates every flag in the registry and returns
 * a map of the results. Flags that don't match the context type will have
 * null values in the resulting map.
 *
 * @param registry The [FlagRegistry] to use (defaults to [SingletonFlagRegistry])
 * @return A map where each key is a [Feature] and the value is the result
 *         of its evaluation (may be null if the flag doesn't match the context type)
 * @param C The type of the context
 */
@Suppress("UNCHECKED_CAST")
fun <C : Context> C.evaluate(registry: FlagRegistry = FlagRegistry): Map<Feature<*, *, *>, Any?> =
    registry.allFlags().mapValues { (_, flag) ->
        (flag as? FlagDefinition<*, *, C>)?.evaluate(this)
    }
