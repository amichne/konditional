file=konditional-core/src/main/kotlin/io/amichne/konditional/api/EvaluationResult.kt
package=io.amichne.konditional.api
imports=io.amichne.konditional.context.RampUp,io.amichne.konditional.core.ops.Metrics,io.amichne.konditional.rules.versions.VersionRange
type=io.amichne.konditional.api.EvaluationResult|kind=class|decl=data class EvaluationResult<T : Any> internal constructor( val namespaceId: String, val featureKey: String, val configVersion: String?, val mode: Metrics.Evaluation.EvaluationMode, val durationNanos: Long, val value: T, val decision: Decision, )
type=io.amichne.konditional.api.Decision|kind=interface|decl=sealed interface Decision
type=io.amichne.konditional.api.RegistryDisabled|kind=object|decl=data object RegistryDisabled : Decision
type=io.amichne.konditional.api.Inactive|kind=object|decl=data object Inactive : Decision
type=io.amichne.konditional.api.Rule|kind=class|decl=data class Rule internal constructor( val matched: RuleMatch, val skippedByRollout: RuleMatch? = null, ) : Decision
type=io.amichne.konditional.api.Default|kind=class|decl=data class Default internal constructor( val skippedByRollout: RuleMatch? = null, ) : Decision
type=io.amichne.konditional.api.RuleMatch|kind=class|decl=data class RuleMatch internal constructor( val rule: RuleExplanation, val bucket: BucketInfo, )
type=io.amichne.konditional.api.RuleExplanation|kind=class|decl=data class RuleExplanation internal constructor( val note: String?, val rollout: RampUp, val locales: Set<String>, val platforms: Set<String>, val versionRange: VersionRange, val axes: Map<String, Set<String>>, val baseSpecificity: Int, val extensionSpecificity: Int, val totalSpecificity: Int, val extensionClassName: String?, )
