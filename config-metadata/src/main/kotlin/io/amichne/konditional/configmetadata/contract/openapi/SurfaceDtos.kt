package io.amichne.konditional.configmetadata.contract.openapi

import io.amichne.konditional.configmetadata.contract.ConfigMetadata

internal data class SnapshotState(
    val namespaceId: String,
    val featureKey: String,
    val ruleId: String,
    val version: String,
)

internal data class SnapshotEnvelope(
    val state: SnapshotState,
    val metadata: ConfigMetadata,
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
}

internal data class CodecOutcomeSuccess(
    val appliedVersion: String,
    val warnings: List<String> = emptyList(),
    override val status: String = "SUCCESS",
) : CodecOutcome

internal data class CodecOutcomeFailure(
    val reason: String,
    val retryable: Boolean,
    override val status: String = "FAILURE",
) : CodecOutcome

internal data class MutationEnvelope(
    val state: SnapshotState,
    val metadata: ConfigMetadata,
    val codecOutcome: CodecOutcome,
)

internal data class ApiError(
    val code: String,
    val message: String,
    val details: Map<String, String>? = null,
)

internal data class ErrorEnvelope(
    val error: ApiError,
)
