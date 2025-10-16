package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import io.amichne.konditional.rules.Surjection
import java.security.MessageDigest
import kotlin.math.roundToInt

/**
 * Represents a flag with a specific state type.
 *
 * @param S The type of the state associated with this flag. It must be a non-nullable type.
 */
data class Flag<S : Any>(
    val key: FeatureFlag<S>,
    val rules: List<Surjection<S>>,
    val defaultValue: S,
    val fallbackValue: S,
    val defaultEligibleSegment: Double = 100.0,
    val salt: String = "v1",
) {
    init {
        require(defaultEligibleSegment in 0.0..100.0)
    }

    private companion object {
        val shaDigestSpi: MessageDigest = requireNotNull(MessageDigest.getInstance("SHA-256"))
    }

    private val orderedRules: List<Surjection<S>> =
        rules.sortedWith(compareByDescending<Surjection<S>> { it.rule.specificity() }.thenBy { it.rule.note ?: "" })

    /**
     * Evaluates the current flag based on the provided context and returns a result of type `S`.
     *
     * @param context The context in which the flag evaluation is performed.
     * @return The result of the evaluation, of type `S`.
     */
    fun evaluate(context: Context): S {
        for (invariant in orderedRules) {
            when {
                !invariant.rule.matches(context) -> continue
                isInEligibleSegment(
                    key.key,
                    context.stableId.hexId,
                    salt,
                    invariant.rule.coveragePct
                ) -> return invariant.value
            }
        }
        return if (isInEligibleSegment("${key}#default", context.stableId.hexId, salt, defaultEligibleSegment)) {
            defaultValue
        } else {
            fallbackValue
        }
    }

    /*
     * Determines if the current context belongs to an ineligible segment.
     *
     * This function evaluates specific conditions to check whether the
     * current context or entity falls under a segment that is considered
     * ineligible for a particular operation or feature.
     *
     * @return `true` if the context is in an ineligible segment, `false` otherwise.
     */
    private fun isInEligibleSegment(
        flagKey: String, id: HexId, salt: String, coveragePct: Double
    ): Boolean = when {
        coveragePct <= 0.0 -> false
        coveragePct >= 100.0 -> true
        else -> stableBucket(flagKey, id, salt) < (coveragePct * 100).roundToInt()
    }

    private fun stableBucket(
        flagKey: String, id: HexId, salt: String
    ): Int = with(shaDigestSpi.digest("$salt:$flagKey:${id.id}".toByteArray(Charsets.UTF_8))) {
        ((get(0).toInt() and 0xFF shl 24 or
            (get(1).toInt() and 0xFF shl 16) or
            (get(2).toInt() and 0xFF shl 8) or
            (get(3).toInt() and 0xFF)).toLong() and 0xFFFF_FFFFL).mod(10_000L).toInt()
    }
}
