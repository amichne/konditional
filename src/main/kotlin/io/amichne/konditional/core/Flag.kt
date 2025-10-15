package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import io.amichne.konditional.rules.Rule
import java.security.MessageDigest
import kotlin.math.roundToInt

data class Flag<T : Flaggable<S>, S : Any>(
    val key: FeatureFlag<T, S>,
    val rules: List<Rule<T, S>>,
    val defaultValue: T,
    /**
     * Value to return when user is not in the eligible segment for the default.
     */
    val fallbackValue: T,
    /**
     * Percentage of users that should receive the default value when no rules match.
     * Defaults to 100.0.
     */
    val defaultEligibleSegment: Double = 100.0,
    val salt: String = "v1"
) {
    init {
        require(defaultEligibleSegment in 0.0..100.0)
    }

    private companion object {
        val shaDigestSpi: MessageDigest = requireNotNull(MessageDigest.getInstance("SHA-256"))
    }

    private val orderedRules: List<Rule<T, S>> =
        rules.sortedWith(compareByDescending<Rule<T, S>> { it.specificity() }.thenBy { it.note ?: "" })

    fun evaluate(context: Context): T {
        for (rule in orderedRules) {
            when {
                !rule.matches(context) -> continue
                isInEligibleSegment(key.key, context.stableId.hexId, salt, rule.coveragePct) -> return rule.value
            }
        }
        return if (isInEligibleSegment("${key.key}#default", context.stableId.hexId, salt, defaultEligibleSegment)) {
            defaultValue
        } else {
            fallbackValue
        }
    }

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
