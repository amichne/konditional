package io.amichne.konditional.server.core.surface.route

internal enum class SurfaceHttpMethod(
    val wireName: String,
    val sortOrder: Int,
) {
    GET(wireName = "get", sortOrder = 0),
    POST(wireName = "post", sortOrder = 1),
    PATCH(wireName = "patch", sortOrder = 2),
}
