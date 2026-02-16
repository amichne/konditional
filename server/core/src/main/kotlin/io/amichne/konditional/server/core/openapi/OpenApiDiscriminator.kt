package io.amichne.konditional.server.core.openapi

internal data class OpenApiDiscriminator(
    val propertyName: String,
    val mapping: Map<String, String>,
)
