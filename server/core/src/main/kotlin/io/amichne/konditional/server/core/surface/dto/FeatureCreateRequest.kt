package io.amichne.konditional.server.core.surface.dto

internal data class FeatureCreateRequest(
    val featureKey: String,
    val description: String? = null,
    val enabled: Boolean = true,
)
