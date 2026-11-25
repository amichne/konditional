package io.amichne.konditional.api

import io.amichne.konditional.context.Rollout

@ConsistentCopyVisibility
data class BucketInfo internal constructor(
    val featureKey: String,
    val salt: String,
    val bucket: Int,
    val rollout: Rollout,
    val thresholdBasisPoints: Int,
    val inRollout: Boolean,
)
