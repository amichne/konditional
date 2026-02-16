package io.amichne.konditional.server.core.surface.route

internal data class SurfaceRequestBody(
    val componentSchema: String,
    val description: String,
    val required: Boolean = true,
)
