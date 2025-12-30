package io.amichne.konditional.api

import io.amichne.konditional.context.RampUp
import io.amichne.konditional.core.evaluation.Bucketing
import io.amichne.konditional.core.id.StableId

/**
 * Deterministic ramp-up bucketing utilities.
 *
 * These functions are useful for production debugging (e.g., "why is user X not in the 10% ramp-up?")
 * and are guaranteed to match Konditional's evaluation behavior.
 */
object RampUpBucketing {
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
        rampUp: RampUp,
    ): BucketInfo = bucket(stableId, featureKey, salt).let { bucket ->
        BucketInfo(
            featureKey = featureKey,
            salt = salt,
            bucket = bucket,
            rollout = rampUp,
            thresholdBasisPoints = Bucketing.rampUpThresholdBasisPoints(rampUp),
            inRollout = Bucketing.isInRampUp(rampUp, bucket),
        )
    }
}

