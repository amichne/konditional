package io.amichne.konditional.server.core.surface.dto

internal data class RuleEnvelope(
    val state: SnapshotState,
    val snapshotVersion: String? = null,
)
