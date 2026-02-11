file=konditional-core/src/main/kotlin/io/amichne/konditional/core/evaluation/Bucketing.kt
package=io.amichne.konditional.core.evaluation
imports=io.amichne.konditional.context.RampUp,io.amichne.konditional.core.id.HexId,java.security.MessageDigest,kotlin.math.roundToInt
type=io.amichne.konditional.core.evaluation.Bucketing|kind=object|decl=internal object Bucketing
fields:
- private const val BUCKET_SPACE: Int
- private const val MISSING_STABLE_ID_BUCKET: Int
- private val threadLocalDigest
methods:
- fun stableBucket( salt: String, flagKey: String, stableId: HexId, ): Int
- fun rampUpThresholdBasisPoints(rollout: RampUp): Int
- fun missingStableIdBucket(): Int
- fun isInRampUp( rampUp: RampUp, bucket: Int, ): Boolean
