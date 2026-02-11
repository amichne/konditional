package io.amichne.konditional.configmetadata.contract.openapi

import com.squareup.moshi.Json
import io.amichne.kontracts.schema.FieldSchema
import io.amichne.kontracts.schema.JsonSchema
import io.amichne.kontracts.schema.ObjectSchema
import io.amichne.kontracts.schema.ObjectTraits

internal interface OpenApiKonstrained<out S> where S : JsonSchema<*>, S : ObjectTraits {
    val contractSchema: S
}

private object OpenApiModelSchemas {
    private fun required(schema: JsonSchema<*>): FieldSchema = FieldSchema(schema = schema, required = true)

    private fun optional(schema: JsonSchema<*>): FieldSchema = FieldSchema(schema = schema, required = false)

    private val openApiInfoSchema =
        JsonSchema.obj(
            fields =
                linkedMapOf(
                    "title" to required(JsonSchema.string(minLength = 1)),
                    "version" to required(JsonSchema.string(minLength = 1)),
                    "description" to required(JsonSchema.string(minLength = 1)),
                )
        )

    private val openApiSchemaSchema =
        JsonSchema.obj(
            fields =
                linkedMapOf(
                    "ref" to optional(JsonSchema.string(minLength = 1)),
                    "type" to optional(JsonSchema.string(minLength = 1)),
                    "title" to optional(JsonSchema.string()),
                    "description" to optional(JsonSchema.string()),
                    "default" to optional(JsonSchema.any()),
                    "nullable" to optional(JsonSchema.boolean()),
                    "example" to optional(JsonSchema.any()),
                    "deprecated" to optional(JsonSchema.boolean()),
                    "format" to optional(JsonSchema.string()),
                    "enum" to optional(JsonSchema.array(elementSchema = JsonSchema.any())),
                    "minLength" to optional(JsonSchema.int(minimum = 0)),
                    "maxLength" to optional(JsonSchema.int(minimum = 0)),
                    "pattern" to optional(JsonSchema.string()),
                    "minimum" to optional(JsonSchema.double()),
                    "maximum" to optional(JsonSchema.double()),
                    "minItems" to optional(JsonSchema.int(minimum = 0)),
                    "maxItems" to optional(JsonSchema.int(minimum = 0)),
                    "uniqueItems" to optional(JsonSchema.boolean()),
                    "minProperties" to optional(JsonSchema.int(minimum = 0)),
                    "maxProperties" to optional(JsonSchema.int(minimum = 0)),
                    "additionalProperties" to optional(JsonSchema.any()),
                    "properties" to optional(JsonSchema.map(valueSchema = JsonSchema.any())),
                    "required" to optional(JsonSchema.array(elementSchema = JsonSchema.string())),
                    "items" to optional(JsonSchema.any()),
                    "oneOf" to optional(JsonSchema.array(elementSchema = JsonSchema.any())),
                    "allOf" to optional(JsonSchema.array(elementSchema = JsonSchema.any())),
                    "discriminator" to optional(JsonSchema.any()),
                )
        )

    private val openApiMediaTypeSchema =
        JsonSchema.obj(
            fields =
                linkedMapOf(
                    "schema" to required(openApiSchemaSchema),
                )
        )

    private val openApiContentSchema =
        JsonSchema.obj(
            fields =
                linkedMapOf(
                    "application/json" to required(openApiMediaTypeSchema),
                )
        )

    private val openApiResponseSchema =
        JsonSchema.obj(
            fields =
                linkedMapOf(
                    "description" to required(JsonSchema.string(minLength = 1)),
                    "content" to optional(openApiContentSchema),
                )
        )

    private val openApiParameterSchema =
        JsonSchema.obj(
            fields =
                linkedMapOf(
                    "name" to required(JsonSchema.string(minLength = 1)),
                    "in" to required(JsonSchema.string(minLength = 1)),
                    "required" to required(JsonSchema.boolean()),
                    "description" to required(JsonSchema.string(minLength = 1)),
                    "schema" to required(openApiSchemaSchema),
                )
        )

    private val openApiRequestBodySchema =
        JsonSchema.obj(
            fields =
                linkedMapOf(
                    "description" to required(JsonSchema.string(minLength = 1)),
                    "required" to required(JsonSchema.boolean()),
                    "content" to required(openApiContentSchema),
                )
        )

    private val openApiOperationSchema =
        JsonSchema.obj(
            fields =
                linkedMapOf(
                    "tags" to required(JsonSchema.array(elementSchema = JsonSchema.string(minLength = 1))),
                    "summary" to required(JsonSchema.string(minLength = 1)),
                    "description" to required(JsonSchema.string(minLength = 1)),
                    "operationId" to required(JsonSchema.string(minLength = 1)),
                    "responses" to required(JsonSchema.map(valueSchema = openApiResponseSchema)),
                    "parameters" to optional(JsonSchema.array(elementSchema = openApiParameterSchema)),
                    "requestBody" to optional(openApiRequestBodySchema),
                )
        )

    private val openApiPathItemSchema =
        JsonSchema.obj(
            fields =
                linkedMapOf(
                    "get" to optional(openApiOperationSchema),
                    "post" to optional(openApiOperationSchema),
                    "patch" to optional(openApiOperationSchema),
                )
        )

    private val openApiComponentsSchema =
        JsonSchema.obj(
            fields =
                linkedMapOf(
                    "schemas" to required(JsonSchema.map(valueSchema = openApiSchemaSchema)),
                )
        )

    val info: ObjectSchema = openApiInfoSchema
    val schema: ObjectSchema = openApiSchemaSchema
    val mediaType: ObjectSchema = openApiMediaTypeSchema
    val content: ObjectSchema = openApiContentSchema
    val response: ObjectSchema = openApiResponseSchema
    val parameter: ObjectSchema = openApiParameterSchema
    val requestBody: ObjectSchema = openApiRequestBodySchema
    val operation: ObjectSchema = openApiOperationSchema
    val pathItem: ObjectSchema = openApiPathItemSchema
    val components: ObjectSchema = openApiComponentsSchema

    val document: ObjectSchema =
        JsonSchema.obj(
            fields =
                linkedMapOf(
                    "openapi" to required(JsonSchema.string(minLength = 1)),
                    "info" to required(info),
                    "paths" to required(JsonSchema.map(valueSchema = pathItem)),
                    "components" to required(components),
                )
        )
}

internal data class OpenApiDocument(
    val openapi: String,
    val info: OpenApiInfo,
    val paths: Map<String, OpenApiPathItem>,
    val components: OpenApiComponents,
) : OpenApiKonstrained<ObjectSchema> {
    override val contractSchema: ObjectSchema = OpenApiModelSchemas.document
}

internal data class OpenApiInfo(
    val title: String,
    val version: String,
    val description: String,
) : OpenApiKonstrained<ObjectSchema> {
    override val contractSchema: ObjectSchema = OpenApiModelSchemas.info
}

internal data class OpenApiComponents(
    val schemas: Map<String, OpenApiSchema>,
) : OpenApiKonstrained<ObjectSchema> {
    override val contractSchema: ObjectSchema = OpenApiModelSchemas.components
}

internal data class OpenApiPathItem(
    val get: OpenApiOperation? = null,
    val post: OpenApiOperation? = null,
    val patch: OpenApiOperation? = null,
) : OpenApiKonstrained<ObjectSchema> {
    override val contractSchema: ObjectSchema = OpenApiModelSchemas.pathItem

    fun withOperation(
        method: SurfaceHttpMethod,
        operation: OpenApiOperation,
    ): OpenApiPathItem =
        when (method) {
            SurfaceHttpMethod.GET -> copy(get = operation)
            SurfaceHttpMethod.POST -> copy(post = operation)
            SurfaceHttpMethod.PATCH -> copy(patch = operation)
        }
}

internal data class OpenApiOperation(
    val tags: List<String>,
    val summary: String,
    val description: String,
    val operationId: String,
    val responses: Map<String, OpenApiResponse>,
    val parameters: List<OpenApiParameter>? = null,
    val requestBody: OpenApiRequestBody? = null,
) : OpenApiKonstrained<ObjectSchema> {
    override val contractSchema: ObjectSchema = OpenApiModelSchemas.operation
}

internal data class OpenApiParameter(
    val name: String,
    @field:Json(name = "in") val location: String,
    val required: Boolean,
    val description: String,
    val schema: OpenApiSchema,
) : OpenApiKonstrained<ObjectSchema> {
    override val contractSchema: ObjectSchema = OpenApiModelSchemas.parameter
}

internal data class OpenApiRequestBody(
    val description: String,
    val required: Boolean,
    val content: OpenApiContent,
) : OpenApiKonstrained<ObjectSchema> {
    override val contractSchema: ObjectSchema = OpenApiModelSchemas.requestBody
}

internal data class OpenApiResponse(
    val description: String,
    val content: OpenApiContent? = null,
) : OpenApiKonstrained<ObjectSchema> {
    override val contractSchema: ObjectSchema = OpenApiModelSchemas.response
}

internal data class OpenApiContent(
    @field:Json(name = "application/json") val applicationJson: OpenApiMediaType,
) : OpenApiKonstrained<ObjectSchema> {
    override val contractSchema: ObjectSchema = OpenApiModelSchemas.content
}

internal data class OpenApiMediaType(
    val schema: OpenApiSchema,
) : OpenApiKonstrained<ObjectSchema> {
    override val contractSchema: ObjectSchema = OpenApiModelSchemas.mediaType
}

internal data class OpenApiDiscriminator(
    val propertyName: String,
    val mapping: Map<String, String>,
)

internal data class OpenApiSchema(
    @field:Json(name = "\$ref") val ref: String? = null,
    val type: String? = null,
    val title: String? = null,
    val description: String? = null,
    val default: Any? = null,
    val nullable: Boolean? = null,
    val example: Any? = null,
    val deprecated: Boolean? = null,
    val format: String? = null,
    val enum: List<Any>? = null,
    val minLength: Int? = null,
    val maxLength: Int? = null,
    val pattern: String? = null,
    val minimum: Number? = null,
    val maximum: Number? = null,
    val minItems: Int? = null,
    val maxItems: Int? = null,
    val uniqueItems: Boolean? = null,
    val minProperties: Int? = null,
    val maxProperties: Int? = null,
    val additionalProperties: Any? = null,
    val properties: Map<String, OpenApiSchema>? = null,
    val required: List<String>? = null,
    val items: OpenApiSchema? = null,
    val oneOf: List<OpenApiSchema>? = null,
    val allOf: List<OpenApiSchema>? = null,
    val discriminator: OpenApiDiscriminator? = null,
) : OpenApiKonstrained<ObjectSchema> {
    override val contractSchema: ObjectSchema = OpenApiModelSchemas.schema
}
