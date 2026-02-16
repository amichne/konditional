package io.amichne.konditional.server.core.surface.dto

internal data class FeatureEnvelope(
    val namespaceId: String,
    val featureKey: String,
    val version: String,
    val snapshotVersion: String? = null,
)
