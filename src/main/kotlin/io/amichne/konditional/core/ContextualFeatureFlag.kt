package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import io.amichne.konditional.rules.TargetedValue

/**
 * Represents a feature flag that can be evaluated within a specific context.
 *
 * This interface provides the minimal API surface for feature flag evaluation,
 * hiding implementation details like rollout strategies, targeting rules, and bucketing algorithms.
 *
 * @param S The type of value this flag produces. Must be a non-nullable type.
 * @param C The type of context used for evaluation.
 */
interface ContextualFeatureFlag<S : Any, C : Context> {
    /**
     * The unique identifier for this feature flag.
     */
    val key: String

    /**
     * The default value returned when no targeting rules match or the flag is inactive.
     */
    val defaultValue: S

    /**
     * Indicates whether this flag is currently active.
     * Inactive flags always return the default value.
     */
    val isActive: Boolean

    /**
     * Evaluates this feature flag within the given context.
     *
     * @param context The evaluation context containing user/environment information.
     * @return The evaluated value of type S based on targeting rules, or the default value.
     */
    fun evaluate(context: C): S

    companion object {
        operator fun <S : Any, C : Context> invoke(
            conditional: Conditional<S, C>,
            bounds: List<TargetedValue<S, C>>,
            defaultValue: S,
            salt: String = "v1",
            isActive: Boolean = true,
        ): ContextualFeatureFlag<S, C> = FlagDefinition(
            conditional,
            bounds,
            defaultValue,
            salt,
            isActive,
        )
    }
}
