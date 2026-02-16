package io.amichne.konditional.server.core.surface.dto

internal data class ApiError(
    val code: String,
    val message: String,
    val details: Map<String, String>? = null,
)
