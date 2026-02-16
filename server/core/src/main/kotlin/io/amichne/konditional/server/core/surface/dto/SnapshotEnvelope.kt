package io.amichne.konditional.server.core.surface.dto

internal data class SnapshotEnvelope(
    val state: SnapshotState,
    val snapshotVersion: String? = null,
)
