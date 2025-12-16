package io.amichne.konditional.core.evaluation

import io.amichne.konditional.context.Rampup
import io.amichne.konditional.core.id.HexId
import java.security.MessageDigest
import kotlin.math.roundToInt

@PublishedApi
internal object Bucketing {
    private const val BUCKET_SPACE: Int = 10_000

    /**
     * Computes a stable bucket assignment in the range [0, 10_000).
     *
     * The bucketing input is deterministic for a given `(salt, flagKey, stableId)` triple.
     */
    fun stableBucket(
        salt: String,
        flagKey: String,
        stableId: HexId,
    ): Int {
        val digest = MessageDigest.getInstance("SHA-256")
        return with(digest.digest("$salt:$flagKey:${stableId.id}".toByteArray(Charsets.UTF_8))) {
            (
                (
                    get(0).toInt() and 0xFF shl 24 or
                        (get(1).toInt() and 0xFF shl 16) or
                        (get(2).toInt() and 0xFF shl 8) or
                        (get(3).toInt() and 0xFF)
                ).toLong() and 0xFFFF_FFFFL
            ).mod(BUCKET_SPACE.toLong()).toInt()
        }
    }

    /**
     * Converts a rollout percentage (0.0-100.0) into basis points (0-10_000).
     */
    fun rolloutThresholdBasisPoints(rollout: Rampup): Int = (rollout.value * 100.0).roundToInt()

    fun isInRollout(
        rollout: Rampup,
        bucket: Int,
    ): Boolean = when {
        rollout <= 0.0 -> false
        rollout >= 100.0 -> true
        else -> bucket < rolloutThresholdBasisPoints(rollout)
    }
}

