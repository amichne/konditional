@file:Suppress("PackageDirectoryMismatch")

package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Rollout
import io.amichne.konditional.core.id.HexId
import io.amichne.konditional.rules.ConditionalValue
import java.security.MessageDigest
import kotlin.math.roundToInt

/**
 * Represents a complete flag definition with its evaluation rules and default value.
 *
 * @param S The type of the value this flag produces. It must be a non-nullable type.
 * @param C The type of the context that this flag evaluates against.
 */
internal class FlagDefinition<S : Any, C : Context>(
    conditional: Conditional<S, C>,
    values: List<ConditionalValue<S, C>>,
    defaultValue: S,
    salt: String = "v1",
    isActive: Boolean = true,
) : FeatureFlag<S, C>(defaultValue, isActive, conditional, values, salt) {
    val key: String
        get() = conditional.key

    private companion object {
        val shaDigestSpi: MessageDigest = requireNotNull(MessageDigest.getInstance("SHA-256"))
    }

    private val conditionalValues: List<ConditionalValue<S, C>> =
        values.sortedWith(compareByDescending<ConditionalValue<S, C>> { it.rule.specificity() }.thenBy {
            it.rule.note ?: ""
        })

    /**
     * Evaluates the current flag based on the provided context and returns a result of type `S`.
     *
     * @param context The context in which the flag evaluation is performed.
     * @return The result of the evaluation, of type `S`. If the flag is not active, returns the defaultValue.
     */
    override fun evaluate(context: C): S {
        if (!isActive) return defaultValue

        return conditionalValues.firstOrNull {
            it.rule.matches(context) &&
                isInEligibleSegment(
                    flagKey = conditional.key,
                    id = context.stableId.hexId,
                    salt = salt,
                    rollout = it.rule.rollout
                )
        }?.value ?: defaultValue
    }

    /**
     * Determines if the current context belongs to an ineligible segment.
     *
     * This function evaluates specific conditions to check whether the
     * current context or entity falls under a segment that is considered
     * ineligible for a particular operation or feature.
     *
     * @return `true` if the context is in an ineligible segment, `false` otherwise.
     */
    private fun isInEligibleSegment(
        flagKey: String,
        id: HexId,
        salt: String,
        rollout: Rollout,
    ): Boolean =
        when {
            rollout <= 0.0 -> false
            rollout >= 100.0 -> true
            else -> stableBucket(flagKey, id, salt) < (rollout.value * 100).roundToInt()
        }

    private fun stableBucket(
        flagKey: String,
        id: HexId,
        salt: String,
    ): Int =
        with(shaDigestSpi.digest("$salt:$flagKey:${id.id}".toByteArray(Charsets.UTF_8))) {
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
