package io.amichne.konditional.server.core.surface.dto

internal data class MutationEnvelope(
    val state: SnapshotState,
    val codecOutcome: CodecOutcome,
    val snapshotVersion: String? = null,
)
