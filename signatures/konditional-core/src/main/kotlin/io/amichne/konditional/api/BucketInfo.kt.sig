file=konditional-core/src/main/kotlin/io/amichne/konditional/api/BucketInfo.kt
package=io.amichne.konditional.api
imports=io.amichne.konditional.context.RampUp
type=io.amichne.konditional.api.BucketInfo|kind=class|decl=data class BucketInfo internal constructor( val featureKey: String, val salt: String, val bucket: Int, val rollout: RampUp, val thresholdBasisPoints: Int, val inRollout: Boolean, )
