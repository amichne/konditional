package io.amichne.konditional.api

import io.amichne.konditional.context.Rampup
import io.amichne.konditional.core.evaluation.Bucketing
import io.amichne.konditional.core.id.StableId

@ConsistentCopyVisibility
data class BucketInfo internal constructor(
    val featureKey: String,
    val salt: String,
    val bucket: Int,
    val rollout: Rampup,
    val thresholdBasisPoints: Int,
    val inRollout: Boolean,
)

/**
 * Deterministic rollout bucketing utilities.
 *
 * These functions are useful for production debugging (e.g., "why is user X not in the 10% rollout?")
 * and are guaranteed to match Konditional's evaluation behavior.
 */
object RolloutBucketing {
    fun bucket(
        stableId: StableId,
        featureKey: String,
        salt: String,
    ): Int = Bucketing.stableBucket(
        salt = salt,
        flagKey = featureKey,
        stableId = stableId.hexId,
    )

    fun explain(
        stableId: StableId,
        featureKey: String,
        salt: String,
        rollout: Rampup,
    ): BucketInfo = bucket(stableId, featureKey, salt).let {
        BucketInfo(
            featureKey = featureKey,
            salt = salt,
            bucket = it,
            rollout = rollout,
            thresholdBasisPoints = Bucketing.rolloutThresholdBasisPoints(rollout),
            inRollout = Bucketing.isInRollout(rollout, it),
        )
    }
}
