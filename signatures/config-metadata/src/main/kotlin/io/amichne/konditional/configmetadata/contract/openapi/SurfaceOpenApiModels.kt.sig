file=config-metadata/src/main/kotlin/io/amichne/konditional/configmetadata/contract/openapi/SurfaceOpenApiModels.kt
package=io.amichne.konditional.configmetadata.contract.openapi
imports=com.squareup.moshi.Json,io.amichne.kontracts.schema.FieldSchema,io.amichne.kontracts.schema.JsonSchema,io.amichne.kontracts.schema.ObjectSchema,io.amichne.kontracts.schema.ObjectTraits
type=io.amichne.konditional.configmetadata.contract.openapi.OpenApiKonstrained|kind=interface|decl=internal interface OpenApiKonstrained<out S> where S : JsonSchema<*>, S : ObjectTraits
type=io.amichne.konditional.configmetadata.contract.openapi.OpenApiModelSchemas|kind=object|decl=private object OpenApiModelSchemas
type=io.amichne.konditional.configmetadata.contract.openapi.OpenApiDocument|kind=class|decl=internal data class OpenApiDocument( val openapi: String, val info: OpenApiInfo, val paths: Map<String, OpenApiPathItem>, val components: OpenApiComponents, ) : OpenApiKonstrained<ObjectSchema>
type=io.amichne.konditional.configmetadata.contract.openapi.OpenApiInfo|kind=class|decl=internal data class OpenApiInfo( val title: String, val version: String, val description: String, ) : OpenApiKonstrained<ObjectSchema>
type=io.amichne.konditional.configmetadata.contract.openapi.OpenApiComponents|kind=class|decl=internal data class OpenApiComponents( val schemas: Map<String, OpenApiSchema>, ) : OpenApiKonstrained<ObjectSchema>
type=io.amichne.konditional.configmetadata.contract.openapi.OpenApiPathItem|kind=class|decl=internal data class OpenApiPathItem( val get: OpenApiOperation? = null, val post: OpenApiOperation? = null, val patch: OpenApiOperation? = null, ) : OpenApiKonstrained<ObjectSchema>
type=io.amichne.konditional.configmetadata.contract.openapi.OpenApiOperation|kind=class|decl=internal data class OpenApiOperation( val tags: List<String>, val summary: String, val description: String, val operationId: String, val responses: Map<String, OpenApiResponse>, val parameters: List<OpenApiParameter>? = null, val requestBody: OpenApiRequestBody? = null, ) : OpenApiKonstrained<ObjectSchema>
type=io.amichne.konditional.configmetadata.contract.openapi.OpenApiParameter|kind=class|decl=internal data class OpenApiParameter( val name: String, @field:Json(name = "in") val location: String, val required: Boolean, val description: String, val schema: OpenApiSchema, ) : OpenApiKonstrained<ObjectSchema>
type=io.amichne.konditional.configmetadata.contract.openapi.OpenApiRequestBody|kind=class|decl=internal data class OpenApiRequestBody( val description: String, val required: Boolean, val content: OpenApiContent, ) : OpenApiKonstrained<ObjectSchema>
type=io.amichne.konditional.configmetadata.contract.openapi.OpenApiResponse|kind=class|decl=internal data class OpenApiResponse( val description: String, val content: OpenApiContent? = null, ) : OpenApiKonstrained<ObjectSchema>
type=io.amichne.konditional.configmetadata.contract.openapi.OpenApiContent|kind=class|decl=internal data class OpenApiContent( @field:Json(name = "application/json") val applicationJson: OpenApiMediaType, ) : OpenApiKonstrained<ObjectSchema>
type=io.amichne.konditional.configmetadata.contract.openapi.OpenApiMediaType|kind=class|decl=internal data class OpenApiMediaType( val schema: OpenApiSchema, ) : OpenApiKonstrained<ObjectSchema>
type=io.amichne.konditional.configmetadata.contract.openapi.OpenApiDiscriminator|kind=class|decl=internal data class OpenApiDiscriminator( val propertyName: String, val mapping: Map<String, String>, )
type=io.amichne.konditional.configmetadata.contract.openapi.OpenApiSchema|kind=class|decl=internal data class OpenApiSchema( @field:Json(name = "\$ref") val ref: String? = null, val type: String? = null, val title: String? = null, val description: String? = null, val default: Any? = null, val nullable: Boolean? = null, val example: Any? = null, val deprecated: Boolean? = null, val format: String? = null, val enum: List<Any>? = null, val minLength: Int? = null, val maxLength: Int? = null, val pattern: String? = null, val minimum: Number? = null, val maximum: Number? = null, val minItems: Int? = null, val maxItems: Int? = null, val uniqueItems: Boolean? = null, val minProperties: Int? = null, val maxProperties: Int? = null, val additionalProperties: Any? = null, val properties: Map<String, OpenApiSchema>? = null, val required: List<String>? = null, val items: OpenApiSchema? = null, val oneOf: List<OpenApiSchema>? = null, val allOf: List<OpenApiSchema>? = null, val discriminator: OpenApiDiscriminator? = null, ) : OpenApiKonstrained<ObjectSchema>
fields:
- val contractSchema: S
- private val openApiInfoSchema
- private val openApiSchemaSchema
- private val openApiMediaTypeSchema
- private val openApiContentSchema
- private val openApiResponseSchema
- private val openApiParameterSchema
- private val openApiRequestBodySchema
- private val openApiOperationSchema
- private val openApiPathItemSchema
- private val openApiComponentsSchema
- val info: ObjectSchema
- val schema: ObjectSchema
- val mediaType: ObjectSchema
- val content: ObjectSchema
- val response: ObjectSchema
- val parameter: ObjectSchema
- val requestBody: ObjectSchema
- val operation: ObjectSchema
- val pathItem: ObjectSchema
- val components: ObjectSchema
- val document: ObjectSchema
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
- override val contractSchema: ObjectSchema
methods:
- private fun required(schema: JsonSchema<*>): FieldSchema
- private fun optional(schema: JsonSchema<*>): FieldSchema
- fun withOperation( method: SurfaceHttpMethod, operation: OpenApiOperation, ): OpenApiPathItem
