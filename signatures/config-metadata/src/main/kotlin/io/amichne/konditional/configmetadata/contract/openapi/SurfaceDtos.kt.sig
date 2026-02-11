file=config-metadata/src/main/kotlin/io/amichne/konditional/configmetadata/contract/openapi/SurfaceDtos.kt
package=io.amichne.konditional.configmetadata.contract.openapi
imports=io.amichne.konditional.configmetadata.contract.ConfigMetadata
type=io.amichne.konditional.configmetadata.contract.openapi.SnapshotState|kind=class|decl=internal data class SnapshotState( val namespaceId: String, val featureKey: String, val ruleId: String, val version: String, )
type=io.amichne.konditional.configmetadata.contract.openapi.SnapshotEnvelope|kind=class|decl=internal data class SnapshotEnvelope( val state: SnapshotState, val metadata: ConfigMetadata, )
type=io.amichne.konditional.configmetadata.contract.openapi.SnapshotMutationRequest|kind=class|decl=internal data class SnapshotMutationRequest( val namespaceId: String, val requestedBy: String, val reason: String, )
type=io.amichne.konditional.configmetadata.contract.openapi.RulePatchRequest|kind=class|decl=internal data class RulePatchRequest( val note: String? = null, val active: Boolean? = null, val rampUpPercent: Double? = null, )
type=io.amichne.konditional.configmetadata.contract.openapi.CodecOutcome|kind=interface|decl=internal sealed interface CodecOutcome
type=io.amichne.konditional.configmetadata.contract.openapi.CodecOutcomeSuccess|kind=class|decl=internal data class CodecOutcomeSuccess( val appliedVersion: String, val warnings: List<String> = emptyList(), override val status: String = "SUCCESS", ) : CodecOutcome
type=io.amichne.konditional.configmetadata.contract.openapi.CodecOutcomeFailure|kind=class|decl=internal data class CodecOutcomeFailure( val reason: String, val retryable: Boolean, override val status: String = "FAILURE", ) : CodecOutcome
type=io.amichne.konditional.configmetadata.contract.openapi.MutationEnvelope|kind=class|decl=internal data class MutationEnvelope( val state: SnapshotState, val metadata: ConfigMetadata, val codecOutcome: CodecOutcome, )
type=io.amichne.konditional.configmetadata.contract.openapi.ApiError|kind=class|decl=internal data class ApiError( val code: String, val message: String, val details: Map<String, String>? = null, )
type=io.amichne.konditional.configmetadata.contract.openapi.ErrorEnvelope|kind=class|decl=internal data class ErrorEnvelope( val error: ApiError, )
fields:
- val status: String
