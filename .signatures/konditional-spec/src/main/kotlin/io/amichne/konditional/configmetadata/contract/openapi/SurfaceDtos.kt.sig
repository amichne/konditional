file=konditional-spec/src/main/kotlin/io/amichne/konditional/configmetadata/contract/openapi/SurfaceDtos.kt
package=io.amichne.konditional.configmetadata.contract.openapi
type=io.amichne.konditional.configmetadata.contract.openapi.SnapshotState|kind=class|decl=internal data class SnapshotState( val namespaceId: String, val featureKey: String, val ruleId: String, val version: String, )
type=io.amichne.konditional.configmetadata.contract.openapi.SnapshotEnvelope|kind=class|decl=internal data class SnapshotEnvelope( val state: SnapshotState, val snapshotVersion: String? = null, )
type=io.amichne.konditional.configmetadata.contract.openapi.FeatureEnvelope|kind=class|decl=internal data class FeatureEnvelope( val namespaceId: String, val featureKey: String, val version: String, val snapshotVersion: String? = null, )
type=io.amichne.konditional.configmetadata.contract.openapi.RuleEnvelope|kind=class|decl=internal data class RuleEnvelope( val state: SnapshotState, val snapshotVersion: String? = null, )
type=io.amichne.konditional.configmetadata.contract.openapi.SnapshotMutationRequest|kind=class|decl=internal data class SnapshotMutationRequest( val namespaceId: String, val requestedBy: String, val reason: String, val selector: TargetSelector = TargetSelector.All, val profile: SurfaceProfile? = null, )
type=io.amichne.konditional.configmetadata.contract.openapi.NamespacePatchRequest|kind=class|decl=internal data class NamespacePatchRequest( val note: String? = null, val active: Boolean? = null, )
type=io.amichne.konditional.configmetadata.contract.openapi.FeatureCreateRequest|kind=class|decl=internal data class FeatureCreateRequest( val featureKey: String, val description: String? = null, val enabled: Boolean = true, )
type=io.amichne.konditional.configmetadata.contract.openapi.FeaturePatchRequest|kind=class|decl=internal data class FeaturePatchRequest( val note: String? = null, val enabled: Boolean? = null, )
type=io.amichne.konditional.configmetadata.contract.openapi.RulePatchRequest|kind=class|decl=internal data class RulePatchRequest( val note: String? = null, val active: Boolean? = null, val rampUpPercent: Double? = null, )
type=io.amichne.konditional.configmetadata.contract.openapi.CodecStatus|kind=enum|decl=internal enum class CodecStatus
type=io.amichne.konditional.configmetadata.contract.openapi.CodecPhase|kind=enum|decl=internal enum class CodecPhase
type=io.amichne.konditional.configmetadata.contract.openapi.CodecOutcome|kind=interface|decl=internal sealed interface CodecOutcome
type=io.amichne.konditional.configmetadata.contract.openapi.CodecOutcomeSuccess|kind=class|decl=internal data class CodecOutcomeSuccess( val appliedVersion: String, val warnings: List<String> = emptyList(), override val status: CodecStatus = CodecStatus.SUCCESS, override val phase: CodecPhase = CodecPhase.APPLY_MUTATION, ) : CodecOutcome
type=io.amichne.konditional.configmetadata.contract.openapi.CodecError|kind=class|decl=internal data class CodecError( val code: String, val message: String, val details: Map<String, String>? = null, )
type=io.amichne.konditional.configmetadata.contract.openapi.CodecOutcomeFailure|kind=class|decl=internal data class CodecOutcomeFailure( val error: CodecError, override val status: CodecStatus = CodecStatus.FAILURE, override val phase: CodecPhase, ) : CodecOutcome
type=io.amichne.konditional.configmetadata.contract.openapi.MutationEnvelope|kind=class|decl=internal data class MutationEnvelope( val state: SnapshotState, val codecOutcome: CodecOutcome, val snapshotVersion: String? = null, )
type=io.amichne.konditional.configmetadata.contract.openapi.ApiError|kind=class|decl=internal data class ApiError( val code: String, val message: String, val details: Map<String, String>? = null, )
type=io.amichne.konditional.configmetadata.contract.openapi.ErrorEnvelope|kind=class|decl=internal data class ErrorEnvelope( val error: ApiError, )
fields:
- val status: CodecStatus
- val phase: CodecPhase
