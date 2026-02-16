package io.amichne.konditional.server.core.surface.route

internal data class SurfaceResponse(
    val statusCode: Int,
    val description: String,
    val componentSchema: String?,
)
