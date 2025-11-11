package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import io.amichne.konditional.rules.ConditionalValue

/**
 * Represents a feature flag that can be evaluated within a specific context.
 *
 * This interface provides the minimal API surface for feature flag evaluation,
 * hiding implementation details like rollout strategies, targeting rules, and bucketing algorithms.
 *
 * @param S The type of value this flag produces. Must be a non-nullable type.
 * @param C The type of context used for evaluation.
 *
 * @property defaultValue The default value returned when no targeting rules match or the flag is inactive.
 * @property conditional The conditional that defines the flag's key and evaluation rules. *
 * @property isActive Indicates whether this flag is currently active. Inactive flags always return the default value.
 * @property values List of conditional values that define the flag's behavior.
 * @property salt Optional salt string used for hashing and bucketing.
 *
 */

sealed class FeatureFlag<S : Any, C : Context>(
    /**
     * The default value returned when no targeting rules match or the flag is inactive.
     */
    val defaultValue: S,
    val isActive: Boolean,
    val conditional: Conditional<S, C>,
    internal val values: List<ConditionalValue<S, C>>,
    val salt: String = "v1"
) {

    /**
     * Evaluates this feature flag within the given context.
     *
     * @param context The evaluation context containing user/environment information.
     * @return The evaluated value of type S based on targeting rules, or the default value.
     */
    internal abstract fun evaluate(context: C): S

    internal companion object {
        operator fun <S : Any, C : Context> invoke(
            conditional: Conditional<S, C>,
            bounds: List<ConditionalValue<S, C>>,
            defaultValue: S,
            salt: String = "v1",
            isActive: Boolean = true,
        ): FeatureFlag<S, C> = FlagDefinition(
            conditional,
            bounds,
            defaultValue,
            salt,
            isActive,
        )
    }
}
