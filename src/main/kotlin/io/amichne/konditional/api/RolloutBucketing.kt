package io.amichne.konditional.api

import io.amichne.konditional.context.RampUp
import io.amichne.konditional.core.evaluation.Bucketing
import io.amichne.konditional.core.id.StableId

@ConsistentCopyVisibility
data class BucketInfo internal constructor(
    val featureKey: String,
    val salt: String,
    val bucket: Int,
    val rollout: RampUp,
    val thresholdBasisPoints: Int,
    val inRollout: Boolean,
)

/**
 * Deterministic rampUp bucketing utilities.
 *
 * These functions are useful for production debugging (e.g., "why is user X not in the 10% rampUp?")
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
        rollout: RampUp,
    ): BucketInfo = bucket(stableId, featureKey, salt).let {
        BucketInfo(
            featureKey = featureKey,
            salt = salt,
            bucket = it,
            rollout = rollout,
            thresholdBasisPoints = Bucketing.rampUpThresholdBasisPoints(rollout),
            inRollout = Bucketing.isInRampUp(rollout, it),
        )
    }
}
