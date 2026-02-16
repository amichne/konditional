package io.amichne.konditional.server.core.surface.dto

internal data class CodecOutcomeFailure(
    val error: CodecError,
    override val status: CodecStatus = CodecStatus.FAILURE,
    override val phase: CodecPhase,
) : CodecOutcome
