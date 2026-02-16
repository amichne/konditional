package io.amichne.konditional.server.core.surface.dto

internal sealed interface CodecOutcome {
    val status: CodecStatus
    val phase: CodecPhase
}
