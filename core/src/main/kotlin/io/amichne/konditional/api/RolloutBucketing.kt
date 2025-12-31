package io.amichne.konditional.api

import io.amichne.konditional.context.RampUp
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
 * Backwards-compatible alias for [RampUpBucketing].
 */
@Deprecated(
    message = "Renamed to RampUpBucketing.",
    replaceWith = ReplaceWith("RampUpBucketing"),
)
object RolloutBucketing {
    fun bucket(
        stableId: StableId,
        featureKey: String,
        salt: String,
    ): Int = RampUpBucketing.bucket(stableId, featureKey, salt)

    fun explain(
        stableId: StableId,
        featureKey: String,
        salt: String,
        rollout: RampUp,
    ): BucketInfo = RampUpBucketing.explain(stableId, featureKey, salt, rampUp = rollout)
}
