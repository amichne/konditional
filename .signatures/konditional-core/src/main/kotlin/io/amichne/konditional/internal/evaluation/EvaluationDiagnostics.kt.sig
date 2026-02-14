file=konditional-core/src/main/kotlin/io/amichne/konditional/internal/evaluation/EvaluationDiagnostics.kt
package=io.amichne.konditional.internal.evaluation
imports=io.amichne.konditional.api.BucketInfo,io.amichne.konditional.api.KonditionalInternalApi,io.amichne.konditional.context.RampUp,io.amichne.konditional.core.ops.Metrics,io.amichne.konditional.rules.versions.VersionRange
type=io.amichne.konditional.internal.evaluation.EvaluationDiagnostics|kind=class|decl=data class EvaluationDiagnostics<T : Any>( val namespaceId: String, val featureKey: String, val configVersion: String?, val mode: Metrics.Evaluation.EvaluationMode, val durationNanos: Long, val value: T, val decision: Decision, )
type=io.amichne.konditional.internal.evaluation.Decision|kind=interface|decl=sealed interface Decision
type=io.amichne.konditional.internal.evaluation.RegistryDisabled|kind=object|decl=data object RegistryDisabled : Decision
type=io.amichne.konditional.internal.evaluation.Inactive|kind=object|decl=data object Inactive : Decision
type=io.amichne.konditional.internal.evaluation.Rule|kind=class|decl=data class Rule( val matched: RuleMatch, val skippedByRollout: RuleMatch? = null, ) : Decision
type=io.amichne.konditional.internal.evaluation.Default|kind=class|decl=data class Default( val skippedByRollout: RuleMatch? = null, ) : Decision
type=io.amichne.konditional.internal.evaluation.RuleMatch|kind=class|decl=data class RuleMatch( val rule: RuleExplanation, val bucket: BucketInfo, )
type=io.amichne.konditional.internal.evaluation.RuleExplanation|kind=class|decl=data class RuleExplanation( val note: String?, val rollout: RampUp, val locales: Set<String>, val platforms: Set<String>, val versionRange: VersionRange, val axes: Map<String, Set<String>>, val baseSpecificity: Int, val extensionSpecificity: Int, val totalSpecificity: Int, val extensionClassName: String?, )
