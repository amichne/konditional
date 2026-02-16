package io.amichne.konditional.server.core.surface.dto

internal data class CodecOutcomeSuccess(
    val appliedVersion: String,
    val warnings: List<String> = emptyList(),
    override val status: CodecStatus = CodecStatus.SUCCESS,
    override val phase: CodecPhase = CodecPhase.APPLY_MUTATION,
) : CodecOutcome
