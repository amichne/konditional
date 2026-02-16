package io.amichne.konditional.server.core.surface.route

internal enum class SurfaceParameterLocation(
    val wireName: String,
) {
    PATH(wireName = "path"),
    QUERY(wireName = "query"),
}
