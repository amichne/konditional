package io.amichne.konditional.configmetadata.contract.openapi

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import io.amichne.kontracts.schema.AllOfSchema
import io.amichne.kontracts.schema.AnySchema
import io.amichne.kontracts.schema.ArraySchema
import io.amichne.kontracts.schema.BooleanSchema
import io.amichne.kontracts.schema.DoubleSchema
import io.amichne.kontracts.schema.EnumSchema
import io.amichne.kontracts.schema.FieldSchema
import io.amichne.kontracts.schema.IntSchema
import io.amichne.kontracts.schema.JsonSchema
import io.amichne.kontracts.schema.MapSchema
import io.amichne.kontracts.schema.NullSchema
import io.amichne.kontracts.schema.ObjectSchema
import io.amichne.kontracts.schema.ObjectTraits
import io.amichne.kontracts.schema.OneOfSchema
import io.amichne.kontracts.schema.RootObjectSchema
import io.amichne.kontracts.schema.StringSchema
import java.util.IdentityHashMap

internal class SurfaceOpenApiSpecBuilder(
    private val routes: List<SurfaceRoute> = SurfaceRouteCatalog.routes,
    private val components: Map<String, JsonSchema<*>> = SurfaceSchemaRegistry.components,
    private val info: OpenApiInfo =
        OpenApiInfo(
            title = "Konditional Surface API",
            version = "1.0.0",
            description = "Contract-first OpenAPI specification generated from route and DTO metadata.",
        ),
) {
    private val schemaEncoder = OpenApiSchemaEncoder(components)

    fun build(): OpenApiDocument =
        OpenApiDocument(
            openapi = "3.0.3",
            info = info,
            paths = buildPaths(),
            components = OpenApiComponents(schemas = buildComponentSchemas()),
        )

    fun buildJson(): String = OpenApiJsonRenderer.render(build())

    private fun buildPaths(): Map<String, OpenApiPathItem> =
        routes
            .sortedWith(compareBy(SurfaceRoute::path, { it.method.sortOrder }, SurfaceRoute::operationId))
            .groupBy(SurfaceRoute::path)
            .toSortedMap()
            .entries
            .associateTo(linkedMapOf()) { (path, groupedRoutes) ->
                path to
                    groupedRoutes
                        .sortedBy { it.method.sortOrder }
                        .fold(OpenApiPathItem()) { pathItem, route ->
                            pathItem.withOperation(route.method, buildOperation(route))
                        }
            }

    private fun buildOperation(route: SurfaceRoute): OpenApiOperation =
        OpenApiOperation(
            tags = route.tags.sorted(),
            summary = route.summary,
            description = route.description,
            operationId = route.operationId,
            responses = buildResponses(route.responses),
            parameters =
                route.parameters
                    .takeIf(List<SurfaceParameter>::isNotEmpty)
                    ?.sortedWith(compareBy({ it.location.ordinal }, SurfaceParameter::name))
                    ?.map(::buildParameter),
            requestBody =
                route.requestBody?.let { body ->
                    OpenApiRequestBody(
                        description = body.description,
                        required = body.required,
                        content = OpenApiContent(applicationJson = OpenApiMediaType(schema = componentRef(body.componentSchema))),
                    )
                },
        )

    private fun buildResponses(responses: List<SurfaceResponse>): Map<String, OpenApiResponse> =
        responses
            .sortedBy(SurfaceResponse::statusCode)
            .associateTo(linkedMapOf()) { response ->
                response.statusCode.toString() to buildResponse(response)
            }

    private fun buildResponse(response: SurfaceResponse): OpenApiResponse =
        OpenApiResponse(
            description = response.description,
            content =
                response.componentSchema?.let { schemaName ->
                    OpenApiContent(applicationJson = OpenApiMediaType(schema = componentRef(schemaName)))
                },
        )

    private fun buildParameter(parameter: SurfaceParameter): OpenApiParameter =
        OpenApiParameter(
            name = parameter.name,
            location = parameter.location.wireName,
            required = parameter.required,
            description = parameter.description,
            schema = schemaEncoder.encodeInline(parameter.schema),
        )

    private fun buildComponentSchemas(): Map<String, OpenApiSchema> =
        components.entries.associateTo(linkedMapOf()) { (componentName, schema) ->
            componentName to schemaEncoder.encodeComponent(componentName, schema)
        }

    private fun componentRef(componentName: String): OpenApiSchema = OpenApiSchema(ref = "#/components/schemas/$componentName")
}

internal class OpenApiSchemaEncoder(
    componentSchemas: Map<String, JsonSchema<*>>,
) {
    private val componentSchemaNames: Map<JsonSchema<*>, String> =
        IdentityHashMap<JsonSchema<*>, String>().apply {
            componentSchemas.forEach { (name, schema) ->
                put(schema, name)
            }
        }

    fun encodeComponent(
        componentName: String,
        schema: JsonSchema<*>,
    ): OpenApiSchema = encode(schema = schema, currentComponentName = componentName)

    fun encodeInline(schema: JsonSchema<*>): OpenApiSchema = encode(schema = schema, currentComponentName = null)

    private fun encode(
        schema: JsonSchema<*>,
        currentComponentName: String?,
    ): OpenApiSchema {
        val referencedName = componentSchemaNames[schema]
        val useReference = referencedName != null && referencedName != currentComponentName

        return if (useReference) {
            OpenApiSchema(ref = "#/components/schemas/$referencedName")
        } else {
            encodeInlineSchema(schema = schema, currentComponentName = currentComponentName)
        }
    }

    private fun encodeInlineSchema(
        schema: JsonSchema<*>,
        currentComponentName: String?,
    ): OpenApiSchema =
        when (schema) {
            is BooleanSchema -> withCommonProperties(schema, OpenApiSchema(type = "boolean"))
            is StringSchema -> encodeStringSchema(schema)
            is IntSchema -> encodeIntSchema(schema)
            is DoubleSchema -> encodeDoubleSchema(schema)
            is EnumSchema<*> -> encodeEnumSchema(schema)
            is NullSchema -> withCommonProperties(schema, OpenApiSchema(type = "null", nullable = true))
            is AnySchema -> withCommonProperties(schema, OpenApiSchema(type = "object"))
            is ArraySchema<*> ->
                withCommonProperties(
                    schema,
                    OpenApiSchema(
                        type = "array",
                        items = encode(schema.elementSchema, currentComponentName),
                        minItems = schema.minItems,
                        maxItems = schema.maxItems,
                        uniqueItems = schema.uniqueItems.takeIf { it },
                    ),
                )

            is MapSchema<*> ->
                withCommonProperties(
                    schema,
                    OpenApiSchema(
                        type = "object",
                        additionalProperties = encode(schema.valueSchema, currentComponentName),
                        minProperties = schema.minProperties,
                        maxProperties = schema.maxProperties,
                    ),
                )

            is ObjectSchema ->
                encodeObjectSchema(
                    schema = schema,
                    objectTraits = schema,
                    currentComponentName = currentComponentName,
                )

            is RootObjectSchema ->
                encodeObjectSchema(
                    schema = schema,
                    objectTraits = schema,
                    currentComponentName = currentComponentName,
                )

            is OneOfSchema -> encodeOneOfSchema(schema, currentComponentName)
            is AllOfSchema -> encodeAllOfSchema(schema, currentComponentName)
        }

    private fun encodeObjectSchema(
        schema: JsonSchema<*>,
        objectTraits: ObjectTraits,
        currentComponentName: String?,
    ): OpenApiSchema {
        val requiredFields =
            (objectTraits.required ?: objectTraits.fields.filterValues(FieldSchema::required).keys)
                .toSortedSet()

        val properties =
            objectTraits.fields
                .toSortedMap()
                .entries
                .associateTo(linkedMapOf()) { (fieldName, fieldSchema) ->
                    fieldName to encodeFieldSchema(fieldSchema = fieldSchema, currentComponentName = currentComponentName)
                }

        return withCommonProperties(
            schema,
            OpenApiSchema(
                type = "object",
                additionalProperties = false,
                properties = properties,
                required = requiredFields.toList().takeIf(List<String>::isNotEmpty),
            ),
        )
    }

    private fun encodeOneOfSchema(
        schema: OneOfSchema,
        currentComponentName: String?,
    ): OpenApiSchema =
        withCommonProperties(
            schema,
            OpenApiSchema(
                oneOf = schema.options.map { encode(it, currentComponentName) },
                discriminator =
                    schema.discriminator?.let { discriminator ->
                        OpenApiDiscriminator(
                            propertyName = discriminator.propertyName,
                            mapping =
                                discriminator.mapping
                                    .toSortedMap()
                                    .entries
                                    .associateTo(linkedMapOf()) { it.key to it.value },
                        )
                    },
            ),
        )

    private fun encodeAllOfSchema(
        schema: AllOfSchema,
        currentComponentName: String?,
    ): OpenApiSchema =
        withCommonProperties(
            schema,
            OpenApiSchema(
                allOf = schema.options.map { encode(it, currentComponentName) },
            ),
        )

    private fun encodeStringSchema(schema: StringSchema): OpenApiSchema =
        withCommonProperties(
            schema,
            OpenApiSchema(
                type = "string",
                minLength = schema.minLength,
                maxLength = schema.maxLength,
                pattern = schema.pattern,
                format = schema.format,
                enum = schema.enum?.map { it as Any }?.takeIf(List<Any>::isNotEmpty),
            ),
        )

    private fun encodeIntSchema(schema: IntSchema): OpenApiSchema =
        withCommonProperties(
            schema,
            OpenApiSchema(
                type = "integer",
                format = "int32",
                minimum = schema.minimum,
                maximum = schema.maximum,
                enum = schema.enum?.map { it as Any }?.takeIf(List<Any>::isNotEmpty),
            ),
        )

    private fun encodeDoubleSchema(schema: DoubleSchema): OpenApiSchema =
        withCommonProperties(
            schema,
            OpenApiSchema(
                type = "number",
                format = schema.format,
                minimum = schema.minimum,
                maximum = schema.maximum,
                enum = schema.enum?.map { it as Any }?.takeIf(List<Any>::isNotEmpty),
            ),
        )

    private fun encodeEnumSchema(schema: EnumSchema<*>): OpenApiSchema =
        withCommonProperties(
            schema,
            OpenApiSchema(
                type = "string",
                enum = schema.values.map { it.name as Any },
            ),
        )

    private fun encodeFieldSchema(
        fieldSchema: FieldSchema,
        currentComponentName: String?,
    ): OpenApiSchema {
        val encodedSchema = encode(fieldSchema.schema, currentComponentName)

        return encodedSchema.copy(
            description = fieldSchema.description ?: encodedSchema.description,
            default = fieldSchema.defaultValue?.let(::normalizeScalar) ?: encodedSchema.default,
            deprecated = if (fieldSchema.deprecated) true else encodedSchema.deprecated,
        )
    }

    private fun withCommonProperties(
        schema: JsonSchema<*>,
        rawSchema: OpenApiSchema,
    ): OpenApiSchema =
        rawSchema.copy(
            title = schema.title ?: rawSchema.title,
            description = schema.description ?: rawSchema.description,
            default = schema.default?.let(::normalizeScalar) ?: rawSchema.default,
            nullable = if (schema.nullable) true else rawSchema.nullable,
            example = schema.example?.let(::normalizeScalar) ?: rawSchema.example,
            deprecated = if (schema.deprecated) true else rawSchema.deprecated,
        )

    private fun normalizeScalar(value: Any?): Any? =
        when (value) {
            null -> null
            is Enum<*> -> value.name
            is Map<*, *> ->
                value.entries
                    .map { (rawKey, rawValue) -> rawKey.toString() to normalizeScalar(rawValue) }
                    .sortedBy { (key, _) -> key }
                    .associateTo(linkedMapOf()) { (key, normalizedValue) -> key to normalizedValue }

            is List<*> -> value.map(::normalizeScalar)
            else -> value
        }
}

internal object OpenApiJsonRenderer {
    private val mapType = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)

    @Suppress("UNCHECKED_CAST")
    private val jsonAdapter: JsonAdapter<Map<String, Any?>> =
        Moshi.Builder().build().adapter<Map<String, Any?>>(mapType).indent("  ")

    fun render(document: OpenApiDocument): String =
        jsonAdapter
            .toJson(document.toJsonValue())
            .let { serialized -> if (serialized.endsWith("\n")) serialized else "$serialized\n" }

    fun parse(json: String): Map<String, Any?> =
        requireNotNull(jsonAdapter.fromJson(json)) {
            "Failed to parse OpenAPI JSON document."
        }

    private fun OpenApiDocument.toJsonValue(): Map<String, Any?> =
        linkedMapOf(
            "openapi" to openapi,
            "info" to info.toJsonValue(),
            "paths" to
                paths.entries
                    .associateTo(linkedMapOf()) { (path, pathItem) ->
                        path to pathItem.toJsonValue()
                    },
            "components" to components.toJsonValue(),
        )

    private fun OpenApiInfo.toJsonValue(): Map<String, Any?> =
        linkedMapOf(
            "title" to title,
            "version" to version,
            "description" to description,
        )

    private fun OpenApiComponents.toJsonValue(): Map<String, Any?> =
        linkedMapOf(
            "schemas" to
                schemas.entries
                    .associateTo(linkedMapOf()) { (name, schema) ->
                        name to schema.toJsonValue()
                    },
        )

    private fun OpenApiPathItem.toJsonValue(): Map<String, Any?> =
        linkedMapOf<String, Any?>().apply {
            get?.let { put("get", it.toJsonValue()) }
            post?.let { put("post", it.toJsonValue()) }
            patch?.let { put("patch", it.toJsonValue()) }
        }

    private fun OpenApiOperation.toJsonValue(): Map<String, Any?> =
        linkedMapOf<String, Any?>(
            "tags" to tags,
            "summary" to summary,
            "description" to description,
            "operationId" to operationId,
            "responses" to
                responses.entries
                    .associateTo(linkedMapOf()) { (statusCode, response) ->
                        statusCode to response.toJsonValue()
                    },
        ).apply {
            parameters?.takeIf(List<OpenApiParameter>::isNotEmpty)?.let { parameters ->
                put("parameters", parameters.map { parameter -> parameter.toJsonValue() })
            }
            requestBody?.let { put("requestBody", it.toJsonValue()) }
        }

    private fun OpenApiParameter.toJsonValue(): Map<String, Any?> =
        linkedMapOf(
            "name" to name,
            "in" to location,
            "required" to required,
            "description" to description,
            "schema" to schema.toJsonValue(),
        )

    private fun OpenApiRequestBody.toJsonValue(): Map<String, Any?> =
        linkedMapOf(
            "description" to description,
            "required" to required,
            "content" to content.toJsonValue(),
        )

    private fun OpenApiResponse.toJsonValue(): Map<String, Any?> =
        linkedMapOf<String, Any?>("description" to description).apply {
            content?.let { put("content", it.toJsonValue()) }
        }

    private fun OpenApiContent.toJsonValue(): Map<String, Any?> =
        linkedMapOf("application/json" to applicationJson.toJsonValue())

    private fun OpenApiMediaType.toJsonValue(): Map<String, Any?> =
        linkedMapOf("schema" to schema.toJsonValue())

    private fun OpenApiSchema.toJsonValue(): Map<String, Any?> =
        linkedMapOf<String, Any?>().apply {
            ref?.let { put("\$ref", it) }
            type?.let { put("type", it) }
            title?.let { put("title", it) }
            description?.let { put("description", it) }
            default?.let { put("default", normalizeJsonValue(it)) }
            nullable?.takeIf { it }?.let { put("nullable", true) }
            example?.let { put("example", normalizeJsonValue(it)) }
            deprecated?.takeIf { it }?.let { put("deprecated", true) }
            format?.let { put("format", it) }
            enum?.let { put("enum", it.map(::normalizeJsonValue)) }
            minLength?.let { put("minLength", it) }
            maxLength?.let { put("maxLength", it) }
            pattern?.let { put("pattern", it) }
            minimum?.let { put("minimum", it) }
            maximum?.let { put("maximum", it) }
            minItems?.let { put("minItems", it) }
            maxItems?.let { put("maxItems", it) }
            uniqueItems?.takeIf { it }?.let { put("uniqueItems", true) }
            minProperties?.let { put("minProperties", it) }
            maxProperties?.let { put("maxProperties", it) }
            additionalProperties?.let {
                put(
                    "additionalProperties",
                    when (it) {
                        is OpenApiSchema -> it.toJsonValue()
                        else -> normalizeJsonValue(it)
                    },
                )
            }
            properties?.let { schemaProperties ->
                put(
                    "properties",
                    schemaProperties.entries
                        .associateTo(linkedMapOf()) { (propertyName, propertySchema) ->
                            propertyName to propertySchema.toJsonValue()
                        },
                )
            }
            required?.takeIf(List<String>::isNotEmpty)?.let { put("required", it) }
            items?.let { put("items", it.toJsonValue()) }
            oneOf?.takeIf(List<OpenApiSchema>::isNotEmpty)?.let { options ->
                put("oneOf", options.map { option -> option.toJsonValue() })
            }
            allOf?.takeIf(List<OpenApiSchema>::isNotEmpty)?.let { options ->
                put("allOf", options.map { option -> option.toJsonValue() })
            }
            discriminator?.let { put("discriminator", it.toJsonValue()) }
        }

    private fun OpenApiDiscriminator.toJsonValue(): Map<String, Any?> =
        linkedMapOf(
            "propertyName" to propertyName,
            "mapping" to
                mapping.entries
                    .associateTo(linkedMapOf()) { (key, value) ->
                        key to value
                    },
        )

    private fun normalizeJsonValue(value: Any?): Any? =
        when (value) {
            null -> null
            is Enum<*> -> value.name
            is OpenApiSchema -> value.toJsonValue()
            is OpenApiDocument -> value.toJsonValue()
            is OpenApiInfo -> value.toJsonValue()
            is OpenApiComponents -> value.toJsonValue()
            is OpenApiPathItem -> value.toJsonValue()
            is OpenApiOperation -> value.toJsonValue()
            is OpenApiParameter -> value.toJsonValue()
            is OpenApiRequestBody -> value.toJsonValue()
            is OpenApiResponse -> value.toJsonValue()
            is OpenApiContent -> value.toJsonValue()
            is OpenApiMediaType -> value.toJsonValue()
            is OpenApiDiscriminator -> value.toJsonValue()
            is Map<*, *> ->
                value.entries
                    .map { (key, innerValue) -> key.toString() to normalizeJsonValue(innerValue) }
                    .sortedBy { (key, _) -> key }
                    .associateTo(linkedMapOf()) { (key, normalizedValue) ->
                        key to normalizedValue
                    }

            is List<*> -> value.map(::normalizeJsonValue)
            else -> value
        }
}
