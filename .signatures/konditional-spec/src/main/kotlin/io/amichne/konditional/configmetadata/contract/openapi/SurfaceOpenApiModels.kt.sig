file=konditional-spec/src/main/kotlin/io/amichne/konditional/configmetadata/contract/openapi/SurfaceOpenApiModels.kt
package=io.amichne.konditional.configmetadata.contract.openapi
imports=com.squareup.moshi.Json,io.amichne.kontracts.dsl.of,io.amichne.kontracts.dsl.schema,io.amichne.kontracts.schema.JsonSchema,io.amichne.kontracts.schema.ObjectSchema,io.amichne.kontracts.schema.ObjectTraits
type=io.amichne.konditional.configmetadata.contract.openapi.OpenApiKonstrained|kind=interface|decl=internal interface OpenApiKonstrained<out S> where S : JsonSchema<*>, S : ObjectTraits
type=io.amichne.konditional.configmetadata.contract.openapi.OpenApiDocument|kind=class|decl=internal data class OpenApiDocument( val openapi: String, val info: OpenApiInfo, val paths: Map<String, OpenApiPathItem>, val components: OpenApiComponents, ) : OpenApiKonstrained<ObjectSchema>
type=io.amichne.konditional.configmetadata.contract.openapi.OpenApiInfo|kind=class|decl=internal data class OpenApiInfo( val title: String, val version: String, val description: String, ) : OpenApiKonstrained<ObjectSchema>
type=io.amichne.konditional.configmetadata.contract.openapi.OpenApiComponents|kind=class|decl=internal data class OpenApiComponents( val schemas: Map<String, JsonSchema<*>>, ) : OpenApiKonstrained<ObjectSchema>
type=io.amichne.konditional.configmetadata.contract.openapi.OpenApiPathItem|kind=class|decl=internal data class OpenApiPathItem( val get: OpenApiOperation? = null, val post: OpenApiOperation? = null, val patch: OpenApiOperation? = null, ) : OpenApiKonstrained<ObjectSchema>
type=io.amichne.konditional.configmetadata.contract.openapi.OpenApiOperation|kind=class|decl=internal data class OpenApiOperation( val tags: List<String>, val summary: String, val description: String, val operationId: String, val responses: Map<String, OpenApiResponse>, val parameters: List<OpenApiParameter>? = null, val requestBody: OpenApiRequestBody? = null, ) : OpenApiKonstrained<ObjectSchema>
type=io.amichne.konditional.configmetadata.contract.openapi.OpenApiParameter|kind=class|decl=internal data class OpenApiParameter( val name: String, @param:Json(name = "in") val location: String, val required: Boolean, val description: String, val schema: JsonSchema<*>, ) : OpenApiKonstrained<ObjectSchema>
type=io.amichne.konditional.configmetadata.contract.openapi.OpenApiRequestBody|kind=class|decl=internal data class OpenApiRequestBody( val description: String, val required: Boolean, val content: OpenApiContent, ) : OpenApiKonstrained<ObjectSchema>
type=io.amichne.konditional.configmetadata.contract.openapi.OpenApiResponse|kind=class|decl=internal data class OpenApiResponse( val description: String, val content: OpenApiContent? = null, ) : OpenApiKonstrained<ObjectSchema>
type=io.amichne.konditional.configmetadata.contract.openapi.OpenApiContent|kind=class|decl=internal data class OpenApiContent( @param:Json(name = "application/json") val applicationJson: OpenApiMediaType, ) : OpenApiKonstrained<ObjectSchema>
type=io.amichne.konditional.configmetadata.contract.openapi.OpenApiMediaType|kind=class|decl=internal data class OpenApiMediaType( val schema: JsonSchema<*>, ) : OpenApiKonstrained<ObjectSchema>
fields:
- val contractSchema: S
- override val contractSchema: ObjectSchema
- override val contractSchema: ObjectSchema
- override val contractSchema: ObjectSchema
- override val contractSchema: ObjectSchema
- override val contractSchema: ObjectSchema
- override val contractSchema: ObjectSchema
- override val contractSchema: ObjectSchema
- override val contractSchema: ObjectSchema
- override val contractSchema: ObjectSchema
- override val contractSchema: ObjectSchema
methods:
- fun withOperation( method: SurfaceHttpMethod, operation: OpenApiOperation, ): OpenApiPathItem
