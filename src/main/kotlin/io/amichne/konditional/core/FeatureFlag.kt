package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.types.EncodableValue
import io.amichne.konditional.rules.ConditionalValue

/**
 * Represents a feature flag that can be evaluated within a specific context.
 *
 * This interface provides the minimal API surface for feature flag evaluation,
 * hiding implementation details like rollout strategies, targeting rules, and bucketing algorithms.
 *
 * Type S is constrained to EncodableValue subtypes at compile time.
 *
 * @param S The EncodableValue type wrapping the actual value (Boolean, String, Int, or Double).
 * @param C The type of context used for evaluation.
 *
 * @property defaultValue The default EncodableValue returned when no targeting rules match or the flag is inactive.
 * @property conditional The conditional that defines the flag's key and evaluation rules.
 * @property isActive Indicates whether this flag is currently active. Inactive flags always return the default value.
 * @property values List of conditional values that define the flag's behavior.
 * @property salt Optional salt string used for hashing and bucketing.
 *
 */

sealed class FeatureFlag<S : EncodableValue<T>, T : Any, C : Context>(
    /**
     * The default value returned when no targeting rules match or the flag is inactive.
     */
    val defaultValue: T,
    val isActive: Boolean,
    val conditional: Conditional<S, T, C>,
    val values: List<ConditionalValue<S, T, C>>,
    val salt: String = "v1",
) {
    /**
     * Evaluates this feature flag within the given context.
     *
     * @param context The evaluation context containing user/environment information.
     * @return The evaluated EncodableValue based on targeting rules, or the default value.
     */
    internal abstract fun evaluate(context: C): T

    internal companion object {
        /**
         * Creates a FeatureFlag instance.
         */
        operator fun <S : EncodableValue<T>, T : Any, C : Context> invoke(
            conditional: Conditional<S, T, C>,
            bounds: List<ConditionalValue<S, T, C>>,
            defaultValue: T,
            salt: String = "v1",
            isActive: Boolean = true,
        ): FeatureFlag<S, T, C> {
            return FlagDefinition(
                conditional,
                bounds,
                defaultValue,
                salt,
                isActive,
            )
        }
    }
}
