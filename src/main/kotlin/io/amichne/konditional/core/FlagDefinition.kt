package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.evaluation.Bucketing
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.rules.ConditionalValue

/**
 * Represents a flag definition that can be evaluated within a specific contextFn.
 *
 * This sealed class provides the minimal API surface for feature flag evaluation,
 * hiding implementation details like rollout strategies, targeting rules, and bucketing algorithms.
 *
 * @param T The value type produced by this flag.
 * @param C The type of context used for evaluation.
 *
 * @property defaultValue The default value returned when no targeting rules match or the flag is inactive.
 * @property feature The feature that defines the flag's key and evaluation rules.
 * @property isActive Indicates whether this flag is currently active. Inactive flags always return the default value.
 * @property values List of conditional values that define the flag's behavior.
 * @property salt Optional salt string used for hashing and bucketing.
 *
 */

@ConsistentCopyVisibility
data class FlagDefinition<T : Any, C : Context, M : Namespace> internal constructor(
    /**
     * The default value returned when no targeting rules match or the flag is inactive.
     */
    val defaultValue: T,
    val feature: Feature<T, C, M>,
    internal val values: List<ConditionalValue<T, C>> = listOf(),
    val isActive: Boolean = true,
    val salt: String = "v1",
) {
    internal val valuesByPrecedence: List<ConditionalValue<T, C>> =
        values.sortedWith(compareByDescending<ConditionalValue<T, C>> { it.rule.specificity() })

    internal companion object {
        /**
         * Creates a FlagDefinition instance.
         */
        operator fun <T : Any, C : Context, M : Namespace> invoke(
            feature: Feature<T, C, M>,
            bounds: List<ConditionalValue<T, C>>,
            defaultValue: T,
            salt: String = "v1",
            isActive: Boolean = true,
        ): FlagDefinition<T, C, M> =
            FlagDefinition(
                defaultValue = defaultValue,
                feature = feature,
                values = bounds,
                isActive = isActive,
                salt = salt,
            )
    }

    /**
     * Evaluates the current flag based on the provided contextFn and returns a result of type `T`.
     *
     * @param context The contextFn in which the flag evaluation is performed.
     * @return The result of the evaluation, of type `T`. If the flag is not active, returns the defaultValue.
     */
    internal fun evaluate(context: C): T {
        if (!isActive) return defaultValue

        return evaluateTrace(context).value
    }

    internal data class Trace<T : Any, C : Context> internal constructor(
        val value: T,
        val bucket: Int?,
        val matched: ConditionalValue<T, C>?,
        val skippedByRollout: ConditionalValue<T, C>?,
    )

    internal fun evaluateTrace(context: C): Trace<T, C> {
        if (!isActive) {
            return Trace(
                value = defaultValue,
                bucket = null,
                matched = null,
                skippedByRollout = null,
            )
        }

        var bucket: Int? = null
        var skippedByRollout: ConditionalValue<T, C>? = null

        val stableId = context.stableId.hexId
        for (candidate in valuesByPrecedence) {
            if (!candidate.rule.matches(context)) continue

            val computedBucket =
                bucket ?: Bucketing
                    .stableBucket(
                        salt = salt,
                        flagKey = feature.key,
                        stableId = stableId,
                    ).also { bucket = it }

            if (Bucketing.isInRollout(candidate.rule.rollout, computedBucket)) {
                return Trace(
                    value = candidate.value,
                    bucket = computedBucket,
                    matched = candidate,
                    skippedByRollout = skippedByRollout,
                )
            }

            if (skippedByRollout == null) skippedByRollout = candidate
        }

        return Trace(
            value = defaultValue,
            bucket = bucket,
            matched = null,
            skippedByRollout = skippedByRollout,
        )
    }
}
