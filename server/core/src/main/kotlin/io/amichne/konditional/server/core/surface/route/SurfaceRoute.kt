package io.amichne.konditional.server.core.surface.route

import io.amichne.konditional.server.core.surface.profile.SurfaceCapability

internal data class SurfaceRoute(
    val path: String,
    val method: SurfaceHttpMethod,
    val operationId: String,
    val summary: String,
    val description: String,
    val tags: List<String>,
    val parameters: List<SurfaceParameter>,
    val requestBody: SurfaceRequestBody?,
    val responses: List<SurfaceResponse>,
    val capability: SurfaceCapability,
)
