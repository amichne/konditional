package io.amichne.konditional.configmetadata.contract.openapi

internal data class SnapshotState(
    val namespaceId: String,
    val featureKey: String,
    val ruleId: String,
    val version: String,
)

internal data class SnapshotEnvelope(
    val state: SnapshotState,
    val snapshotVersion: String? = null,
)

internal data class FeatureEnvelope(
    val namespaceId: String,
    val featureKey: String,
    val version: String,
    val snapshotVersion: String? = null,
)

internal data class RuleEnvelope(
    val state: SnapshotState,
    val snapshotVersion: String? = null,
)

internal data class SnapshotMutationRequest(
    val namespaceId: String,
    val requestedBy: String,
    val reason: String,
    val selector: TargetSelector = TargetSelector.All,
    val profile: SurfaceProfile? = null,
)

internal data class NamespacePatchRequest(
    val note: String? = null,
    val active: Boolean? = null,
)

internal data class FeatureCreateRequest(
    val featureKey: String,
    val description: String? = null,
    val enabled: Boolean = true,
)

internal data class FeaturePatchRequest(
    val note: String? = null,
    val enabled: Boolean? = null,
)

internal data class RulePatchRequest(
    val note: String? = null,
    val active: Boolean? = null,
    val rampUpPercent: Double? = null,
)

internal enum class CodecStatus {
    SUCCESS,
    FAILURE,
}

internal enum class CodecPhase {
    DECODE_REQUEST,
    APPLY_MUTATION,
    ENCODE_RESPONSE,
}

internal sealed interface CodecOutcome {
    val status: CodecStatus
    val phase: CodecPhase
}

internal data class CodecOutcomeSuccess(
    val appliedVersion: String,
    val warnings: List<String> = emptyList(),
    override val status: CodecStatus = CodecStatus.SUCCESS,
    override val phase: CodecPhase = CodecPhase.APPLY_MUTATION,
) : CodecOutcome

internal data class CodecError(
    val code: String,
    val message: String,
    val details: Map<String, String>? = null,
)

internal data class CodecOutcomeFailure(
    val error: CodecError,
    override val status: CodecStatus = CodecStatus.FAILURE,
    override val phase: CodecPhase,
) : CodecOutcome

internal data class MutationEnvelope(
    val state: SnapshotState,
    val codecOutcome: CodecOutcome,
    val snapshotVersion: String? = null,
)

internal data class ApiError(
    val code: String,
    val message: String,
    val details: Map<String, String>? = null,
)

internal data class ErrorEnvelope(
    val error: ApiError,
)
