file=konditional-spec/src/main/kotlin/io/amichne/konditional/configmetadata/contract/openapi/SurfaceOpenApiSpecBuilder.kt
package=io.amichne.konditional.configmetadata.contract.openapi
imports=io.amichne.kontracts.dsl.schemaRef,io.amichne.kontracts.schema.JsonSchema
type=io.amichne.konditional.configmetadata.contract.openapi.SurfaceOpenApiSpecBuilder|kind=class|decl=internal class SurfaceOpenApiSpecBuilder( routes: List<SurfaceRoute> = SurfaceRouteCatalog.routes, private val components: Map<String, JsonSchema<*>> = SurfaceSchemaRegistry.components, private val info: OpenApiInfo = OpenApiInfo( title = "Konditional Surface API", version = "1.0.0", description = "Contract-first OpenAPI specification generated from route and DTO metadata.", ), private val profile: SurfaceProfile? = null, )
fields:
- private val filteredRoutes: List<SurfaceRoute>
- private val orderedComponents: Map<String, JsonSchema<*>>
methods:
- fun build(): OpenApiDocument
- fun buildJson(): String
- private fun buildPaths(): Map<String, OpenApiPathItem>
- private fun buildOperation(route: SurfaceRoute): OpenApiOperation
- private fun buildResponses(responses: List<SurfaceResponse>): Map<String, OpenApiResponse>
- private fun buildResponse(response: SurfaceResponse): OpenApiResponse
- private fun buildParameter(parameter: SurfaceParameter): OpenApiParameter
- private fun componentRef(componentName: String): JsonSchema<Any>
