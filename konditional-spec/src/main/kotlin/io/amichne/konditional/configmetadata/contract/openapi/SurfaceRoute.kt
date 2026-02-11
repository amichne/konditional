package io.amichne.konditional.configmetadata.contract.openapi

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
)
