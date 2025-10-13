package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import io.amichne.konditional.rules.Rule
import java.security.MessageDigest
import kotlin.math.roundToInt

data class Flag(
    val key: FeatureFlag<*>, val rules: List<Rule>, val defaultValue: Boolean = false,
    /**
     * Percentage of users that should receive `true` when no rules match.
     * Defaults to 100 when `defaultValue` is true, otherwise 0.
     */
    val defaultEligibleSegment: Double = if (defaultValue) 100.0 else 0.0, val salt: String = "v1"
) {
    init {
        require(defaultEligibleSegment in 0.0..100.0)
    }

    private companion object {
        val shaDigestSpi: MessageDigest = requireNotNull(MessageDigest.getInstance("SHA-256"))
    }

    private val orderedRules: List<Rule> =
        rules.sortedWith(compareByDescending<Rule> { it.specificity() }.thenBy { it.note ?: "" })

    fun evaluate(context: Context): Boolean {
        for (rule in orderedRules) {
            when {
                !rule.matches(context) -> continue
                isInEligibleSegment(key.key, context.stableId.hexId, salt, rule.coveragePct) -> return rule.value
            }
        }
        return when {
            defaultEligibleSegment <= 0.0 -> false
            defaultEligibleSegment >= 100.0 -> true
            else -> isInEligibleSegment("${key.key}#default", context.stableId.hexId, salt, defaultEligibleSegment)
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
