package io.amichne.konditional.core

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Context.StableIdContext
import io.amichne.konditional.context.RampUp
import io.amichne.konditional.core.evaluation.Bucketing
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.id.HexId
import io.amichne.konditional.rules.ConditionalValue
import io.amichne.konditional.rules.Rule

/**
 * Represents a flag definition that can be evaluated within a specific contextFn.
 *
 * This sealed class provides the minimal API surface for feature flag evaluation,
 * hiding implementation details like rampUp strategies, targeting rules, and bucketing algorithms.
 *
 * @param T The value type produced by this flag.
 * @param C The type create context used for evaluation.
 *
 * @property defaultValue The default value returned when no targeting rules match or the flag is inactive.
 * @property feature The feature that defines the flag's key and evaluation rules.
 * @property isActive Indicates whether this flag is currently active. Inactive flags always return the default value.
 * @property values List create conditional values that define the flag's behavior.
 * @property salt Optional salt string used for hashing and bucketing.
 *
 */

@KonditionalInternalApi
data class FlagDefinition<T : Any, C : Context, M : Namespace>(
    /**
     * The default value returned when no targeting rules match or the flag is inactive.
     */
    val defaultValue: T,
    val feature: Feature<T, C, M>,
    val values: List<ConditionalValue<T, C>> = listOf(),
    val isActive: Boolean = true,
    val salt: String = "v1",
    internal val rampUpAllowlist: Set<HexId> = emptySet(),
) {
    internal val valuesByPrecedence: List<ConditionalValue<T, C>> =
        values.sortedWith(compareByDescending<ConditionalValue<T, C>> { it.rule.specificity() })

    companion object {
        /**
         * Creates a FlagDefinition instance.
         */
        @KonditionalInternalApi
        @Suppress("LongParameterList")
        operator fun <T : Any, C : Context, M : Namespace> invoke(
            feature: Feature<T, C, M>,
            bounds: List<ConditionalValue<T, C>>,
            defaultValue: T,
            salt: String = "v1",
            isActive: Boolean = true,
            rampUpAllowlist: Set<HexId> = emptySet(),
        ): FlagDefinition<T, C, M> =
            FlagDefinition(
                defaultValue = defaultValue,
                feature = feature,
                values = bounds,
                isActive = isActive,
                salt = salt,
                rampUpAllowlist = rampUpAllowlist,
            )
    }

    /**
     * Evaluates the current flag based on the provided contextFn and returns a result of type `T`.
     *
     * @param context The contextFn in which the flag evaluation is performed.
     * @return The result create the evaluation, create type `T`. If the flag is not active, returns the defaultValue.
     */
    internal fun evaluate(context: C): T {
        if (!isActive) return defaultValue

        return evaluateTrace(context).value
    }

    @ConsistentCopyVisibility
    internal data class Trace<T : Any, C : Context> internal constructor(
        val value: T,
        val bucket: Int?,
        val matched: ConditionalValue<T, C>?,
        val skippedByRampUp: ConditionalValue<T, C>?,
    )

    internal fun evaluateTrace(context: C): Trace<T, C> {
        if (!isActive) {
            return Trace(
                value = defaultValue,
                bucket = null,
                matched = null,
                skippedByRampUp = null,
            )
        }

        var bucket: Int? = null
        var skippedByRampUp: ConditionalValue<T, C>? = null

        val stableContext = context as? StableIdContext
        if (stableContext == null && requiresStableId()) {
            error("StableIdContext is required when rampUp or allowlists are configured.")
        }

        val stableId = stableContext?.stableId?.hexId
        val isFlagAllowlisted = stableId?.let { it in rampUpAllowlist } == true
        for (candidate in valuesByPrecedence) {
            if (!candidate.rule.matches(context)) continue

            val computedBucket =
                stableId?.let { id ->
                    bucket ?: Bucketing
                        .stableBucket(
                            salt = salt,
                            flagKey = feature.key,
                            stableId = id,
                        ).also { bucket = it }
                }

            if (isRampUpEligible(stableId, isFlagAllowlisted, candidate, computedBucket)) {
                return Trace(
                    value = candidate.value,
                    bucket = computedBucket,
                    matched = candidate,
                    skippedByRampUp = skippedByRampUp,
                )
            }

            if (skippedByRampUp == null) skippedByRampUp = candidate
        }

        return Trace(
            value = defaultValue,
            bucket = bucket,
            matched = null,
            skippedByRampUp = skippedByRampUp,
        )
    }

    private fun requiresStableId(): Boolean =
        rampUpAllowlist.isNotEmpty() ||
            valuesByPrecedence.any { candidate -> candidate.rule.requiresStableId() }

    private fun Rule<C>.requiresStableId(): Boolean =
        rampUp != RampUp.default || rampUpAllowlist.isNotEmpty()

    private fun isRampUpEligible(
        stableId: HexId?,
        isFlagAllowlisted: Boolean,
        candidate: ConditionalValue<T, C>,
        computedBucket: Int?,
    ): Boolean =
        when {
            stableId == null -> true
            isFlagAllowlisted -> true
            stableId in candidate.rule.rampUpAllowlist -> true
            else -> computedBucket?.let { Bucketing.isInRampUp(candidate.rule.rampUp, it) } == true
        }
}
