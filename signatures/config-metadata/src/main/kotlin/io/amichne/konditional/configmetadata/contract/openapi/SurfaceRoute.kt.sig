file=config-metadata/src/main/kotlin/io/amichne/konditional/configmetadata/contract/openapi/SurfaceRoute.kt
package=io.amichne.konditional.configmetadata.contract.openapi
type=io.amichne.konditional.configmetadata.contract.openapi.SurfaceRoute|kind=class|decl=internal data class SurfaceRoute( val path: String, val method: SurfaceHttpMethod, val operationId: String, val summary: String, val description: String, val tags: List<String>, val parameters: List<SurfaceParameter>, val requestBody: SurfaceRequestBody?, val responses: List<SurfaceResponse>, )
