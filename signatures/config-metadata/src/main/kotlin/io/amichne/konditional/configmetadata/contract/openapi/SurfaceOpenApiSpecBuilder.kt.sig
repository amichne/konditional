file=config-metadata/src/main/kotlin/io/amichne/konditional/configmetadata/contract/openapi/SurfaceOpenApiSpecBuilder.kt
package=io.amichne.konditional.configmetadata.contract.openapi
imports=com.squareup.moshi.JsonAdapter,com.squareup.moshi.Moshi,com.squareup.moshi.Types,io.amichne.kontracts.schema.AllOfSchema,io.amichne.kontracts.schema.AnySchema,io.amichne.kontracts.schema.ArraySchema,io.amichne.kontracts.schema.BooleanSchema,io.amichne.kontracts.schema.DoubleSchema,io.amichne.kontracts.schema.EnumSchema,io.amichne.kontracts.schema.FieldSchema,io.amichne.kontracts.schema.IntSchema,io.amichne.kontracts.schema.JsonSchema,io.amichne.kontracts.schema.MapSchema,io.amichne.kontracts.schema.NullSchema,io.amichne.kontracts.schema.ObjectSchema,io.amichne.kontracts.schema.ObjectTraits,io.amichne.kontracts.schema.OneOfSchema,io.amichne.kontracts.schema.RootObjectSchema,io.amichne.kontracts.schema.StringSchema,java.util.IdentityHashMap
type=io.amichne.konditional.configmetadata.contract.openapi.SurfaceOpenApiSpecBuilder|kind=class|decl=internal class SurfaceOpenApiSpecBuilder( private val routes: List<SurfaceRoute> = SurfaceRouteCatalog.routes, private val components: Map<String, JsonSchema<*>> = SurfaceSchemaRegistry.components, private val info: OpenApiInfo = OpenApiInfo( title = "Konditional Surface API", version = "1.0.0", description = "Contract-first OpenAPI specification generated from route and DTO metadata.", ), )
type=io.amichne.konditional.configmetadata.contract.openapi.OpenApiSchemaEncoder|kind=class|decl=internal class OpenApiSchemaEncoder( componentSchemas: Map<String, JsonSchema<*>>, )
type=io.amichne.konditional.configmetadata.contract.openapi.OpenApiJsonRenderer|kind=object|decl=internal object OpenApiJsonRenderer
fields:
- private val schemaEncoder
- private val componentSchemaNames: Map<JsonSchema<*>, String>
- private val mapType
- private val jsonAdapter: JsonAdapter<Map<String, Any?>>
methods:
- fun build(): OpenApiDocument
- fun buildJson(): String
- private fun buildPaths(): Map<String, OpenApiPathItem>
- private fun buildOperation(route: SurfaceRoute): OpenApiOperation
- private fun buildResponses(responses: List<SurfaceResponse>): Map<String, OpenApiResponse>
- private fun buildResponse(response: SurfaceResponse): OpenApiResponse
- private fun buildParameter(parameter: SurfaceParameter): OpenApiParameter
- private fun buildComponentSchemas(): Map<String, OpenApiSchema>
- private fun componentRef(componentName: String): OpenApiSchema
- fun encodeComponent( componentName: String, schema: JsonSchema<*>, ): OpenApiSchema
- fun encodeInline(schema: JsonSchema<*>): OpenApiSchema
- private fun encode( schema: JsonSchema<*>, currentComponentName: String?, ): OpenApiSchema
- private fun encodeInlineSchema( schema: JsonSchema<*>, currentComponentName: String?, ): OpenApiSchema
- private fun encodeObjectSchema( schema: JsonSchema<*>, objectTraits: ObjectTraits, currentComponentName: String?, ): OpenApiSchema
- private fun encodeOneOfSchema( schema: OneOfSchema, currentComponentName: String?, ): OpenApiSchema
- private fun encodeAllOfSchema( schema: AllOfSchema, currentComponentName: String?, ): OpenApiSchema
- private fun encodeStringSchema(schema: StringSchema): OpenApiSchema
- private fun encodeIntSchema(schema: IntSchema): OpenApiSchema
- private fun encodeDoubleSchema(schema: DoubleSchema): OpenApiSchema
- private fun encodeEnumSchema(schema: EnumSchema<*>): OpenApiSchema
- private fun encodeFieldSchema( fieldSchema: FieldSchema, currentComponentName: String?, ): OpenApiSchema
- private fun withCommonProperties( schema: JsonSchema<*>, rawSchema: OpenApiSchema, ): OpenApiSchema
- private fun normalizeScalar(value: Any?): Any?
- fun render(document: OpenApiDocument): String
- fun parse(json: String): Map<String, Any?>
- private fun OpenApiDocument.toJsonValue(): Map<String, Any?>
- private fun OpenApiInfo.toJsonValue(): Map<String, Any?>
- private fun OpenApiComponents.toJsonValue(): Map<String, Any?>
- private fun OpenApiPathItem.toJsonValue(): Map<String, Any?>
- private fun OpenApiOperation.toJsonValue(): Map<String, Any?>
- private fun OpenApiParameter.toJsonValue(): Map<String, Any?>
- private fun OpenApiRequestBody.toJsonValue(): Map<String, Any?>
- private fun OpenApiResponse.toJsonValue(): Map<String, Any?>
- private fun OpenApiContent.toJsonValue(): Map<String, Any?>
- private fun OpenApiMediaType.toJsonValue(): Map<String, Any?>
- private fun OpenApiSchema.toJsonValue(): Map<String, Any?>
- private fun OpenApiDiscriminator.toJsonValue(): Map<String, Any?>
- private fun normalizeJsonValue(value: Any?): Any?
