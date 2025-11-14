package io.amichne.konditional.context

import io.amichne.konditional.core.Feature
import io.amichne.konditional.core.Taxonomy
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.types.EncodableValue

/**
 * Evaluates a specific feature flag in the context of this [Context].
 *
 * This extension function provides convenient access to flag evaluation.
 * The feature's taxonomy-scoped registry is automatically used.
 *
 * @param key The feature flag to evaluate
 * @return The evaluated value of type [T]
 * @throws IllegalStateException if the flag is not found in the registry
 * @param S The EncodableValue type wrapping the actual value
 * @param T The actual value type
 * @param C The type of the context
 * @param M The taxonomy the feature belongs to
 */
@Suppress("UNCHECKED_CAST")
fun <S : EncodableValue<T>, T : Any, C : Context, M : Taxonomy> C.evaluate(
    key: Feature<S, T, C, M>
): T {
    val flag = key.registry.featureFlag(key)
        ?: throw IllegalStateException("Flag not found: ${key.key}")
    return flag.evaluate(this)
}

/**
 * Evaluates all feature flags from a specific taxonomy in the context of this [Context].
 *
 * This extension function evaluates every flag in the taxonomy's registry and returns
 * a map of the results. Flags that don't match the context type will have
 * null values in the resulting map.
 *
 * @param module The taxonomy whose flags should be evaluated
 * @return A map where each key is a [Feature] and the value is the result
 *         of its evaluation (may be null if the flag doesn't match the context type)
 * @param C The type of the context
 * @param M The taxonomy type
 */
@Suppress("UNCHECKED_CAST")
fun <C : Context, M : Taxonomy> C.evaluateModule(module: M): Map<Feature<*, *, *, *>, Any?> =
    module.registry.allFlags().mapValues { (_, flag) ->
        (flag as? FlagDefinition<*, *, C, M>)?.evaluate(this)
    }
