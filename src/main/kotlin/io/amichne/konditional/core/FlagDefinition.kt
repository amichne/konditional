package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Rampup
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.id.HexId
import io.amichne.konditional.rules.ConditionalValue
import java.security.MessageDigest
import kotlin.math.roundToInt

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
    private val conditionalValues: List<ConditionalValue<T, C>> =
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
        ): FlagDefinition<T, C, M> = FlagDefinition(
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
    fun evaluate(context: C): T {
        if (!isActive) return defaultValue

        return conditionalValues.firstOrNull {
            it.rule.matches(context) &&
            isInEligibleSegment(
                flagKey = feature.key,
                id = context.stableId.hexId,
                salt = salt,
                rollout = it.rule.rollout
            )
        }?.value ?: defaultValue
    }

    /**
     * Determines if the current contextFn belongs to an ineligible segment.
     *
     * This function evaluates specific conditions to check whether the
     * current contextFn or entity falls under a segment that is considered
     * ineligible for a particular operation or feature.
     *
     * @return `true` if the contextFn is in an ineligible segment, `false` otherwise.
     */
    private fun isInEligibleSegment(
        flagKey: String,
        id: HexId,
        salt: String,
        rollout: Rampup,
    ): Boolean =
        when {
            rollout <= 0.0 -> false
            rollout >= 100.0 -> true
            else -> stableBucket(flagKey, id, salt) < (rollout.value * 100).roundToInt()
        }

    /**
     * Create a new MessageDigest instance per call to ensure thread-safety
     * MessageDigest is NOT thread-safe and cannot be shared across concurrent evaluations
     */
    private fun stableBucket(
        flagKey: String,
        id: HexId,
        salt: String,
    ): Int {
        val digest = MessageDigest.getInstance("SHA-256")
        return with(digest.digest("$salt:$flagKey:${id.id}".toByteArray(Charsets.UTF_8))) {
            (
                (
                    get(0).toInt() and 0xFF shl 24 or
                        (get(1).toInt() and 0xFF shl 16) or
                        (get(2).toInt() and 0xFF shl 8) or
                        (get(3).toInt() and 0xFF)
                ).toLong() and 0xFFFF_FFFFL
            ).mod(10_000L).toInt()
        }
    }
}
