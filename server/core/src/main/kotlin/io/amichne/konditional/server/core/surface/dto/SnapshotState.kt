package io.amichne.konditional.server.core.surface.dto

internal data class SnapshotState(
    val namespaceId: String,
    val featureKey: String,
    val ruleId: String,
    val version: String,
)
