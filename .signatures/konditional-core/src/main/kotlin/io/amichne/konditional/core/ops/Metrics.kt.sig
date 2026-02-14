file=konditional-core/src/main/kotlin/io/amichne/konditional/core/ops/Metrics.kt
package=io.amichne.konditional.core.ops
type=io.amichne.konditional.core.ops.Metrics|kind=object|decl=object Metrics
type=io.amichne.konditional.core.ops.Evaluation|kind=class|decl=data class Evaluation internal constructor( val namespaceId: String, val featureKey: String, val mode: EvaluationMode, val durationNanos: Long, val decision: DecisionKind, val configVersion: String? = null, val bucket: Int? = null, val matchedRuleSpecificity: Int? = null, )
type=io.amichne.konditional.core.ops.EvaluationMode|kind=enum|decl=enum class EvaluationMode
type=io.amichne.konditional.core.ops.DecisionKind|kind=enum|decl=enum class DecisionKind
type=io.amichne.konditional.core.ops.ConfigLoadMetric|kind=class|decl=data class ConfigLoadMetric internal constructor( val namespaceId: String, val featureCount: Int, val version: String? = null, )
type=io.amichne.konditional.core.ops.ConfigRollbackMetric|kind=class|decl=data class ConfigRollbackMetric internal constructor( val namespaceId: String, val steps: Int, val success: Boolean, val version: String? = null, )
