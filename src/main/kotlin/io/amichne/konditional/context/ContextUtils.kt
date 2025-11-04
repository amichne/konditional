package io.amichne.konditional.context

import io.amichne.konditional.core.Conditional
import io.amichne.konditional.core.FeatureFlag
import io.amichne.konditional.core.FlagRegistry
import io.amichne.konditional.core.internal.SingletonFlagRegistry

/**
 * Evaluates a specific feature flag in the context of this [Context].
 *
 * This extension function provides convenient access to flag evaluation
 * for any [io.amichne.konditional.core.FlagRegistry] implementation.
 *
 * @param key The feature flag to evaluate
 * @param registry The [io.amichne.konditional.core.FlagRegistry] to use (defaults to [SingletonFlagRegistry])
 * @return The evaluated value of type [S]
 * @throws IllegalStateException if the flag is not found in the registry
 * @param S The type of the flag's value
 * @param C The type of the context
 */
@Suppress("UNCHECKED_CAST")
fun <S : Any, C : Context> C.evaluate(
    key: Conditional<S, C>,
    registry: FlagRegistry = FlagRegistry
): S {
    val flag = registry.featureFlag(key)
        ?: throw IllegalStateException("Flag not found: ${key.key}")
    return flag.evaluate(this)
}

/**
 * Evaluates all feature flags in the context of this [Context].
 *
 * This extension function evaluates every flag in the registry and returns
 * a map of the results. SingletonFlagRegistry that don't match the context type will have
 * null values in the resulting map.
 *
 * @param registry The [FlagRegistry] to use (defaults to [SingletonFlagRegistry])
 * @return A map where each key is a [Conditional] and the value is the result
 *         of its evaluation (may be null if the flag doesn't match the context type)
 * @param C The type of the context
 */
@Suppress("UNCHECKED_CAST")
fun <C : Context> C.evaluate(registry: FlagRegistry = FlagRegistry): Map<Conditional<*, *>, Any?> =
    registry.allFlags().mapValues { (_, flag) ->
        (flag as? FeatureFlag<*, C>)?.evaluate(this)
    }
