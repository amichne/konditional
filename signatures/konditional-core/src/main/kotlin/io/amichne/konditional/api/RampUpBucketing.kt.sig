file=konditional-core/src/main/kotlin/io/amichne/konditional/api/RampUpBucketing.kt
package=io.amichne.konditional.api
imports=io.amichne.konditional.context.RampUp,io.amichne.konditional.core.evaluation.Bucketing,io.amichne.konditional.core.id.StableId
type=io.amichne.konditional.api.RampUpBucketing|kind=object|decl=object RampUpBucketing
methods:
- fun bucket( stableId: StableId, featureKey: String, salt: String, ): Int
