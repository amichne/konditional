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

internal data class SnapshotMutationRequest(
    val namespaceId: String,
    val requestedBy: String,
    val reason: String,
)

internal data class RulePatchRequest(
    val note: String? = null,
    val active: Boolean? = null,
    val rampUpPercent: Double? = null,
)

internal sealed interface CodecOutcome {
    val status: String
    val phase: String
}

internal data class CodecOutcomeSuccess(
    val appliedVersion: String,
    val warnings: List<String> = emptyList(),
    override val status: String = "SUCCESS",
    override val phase: String = "APPLY_MUTATION",
) : CodecOutcome

internal data class CodecError(
    val code: String,
    val message: String,
    val details: Map<String, String>? = null,
)

internal data class CodecOutcomeFailure(
    val error: CodecError,
    override val status: String = "FAILURE",
    override val phase: String,
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
