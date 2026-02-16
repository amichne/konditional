package io.amichne.konditional.server.core.openapi

import com.squareup.moshi.Json

internal data class OpenApiSchema(
    @param:Json(name = "\$ref") val ref: String? = null,
    val type: String? = null,
    val format: String? = null,
    val title: String? = null,
    val description: String? = null,
    val default: Any? = null,
    val nullable: Boolean? = null,
    val example: Any? = null,
    val deprecated: Boolean? = null,
    val enum: List<Any?>? = null,
    val minimum: Any? = null,
    val maximum: Any? = null,
    val minLength: Int? = null,
    val maxLength: Int? = null,
    val pattern: String? = null,
    val minItems: Int? = null,
    val maxItems: Int? = null,
    val uniqueItems: Boolean? = null,
    val items: OpenApiSchema? = null,
    val minProperties: Int? = null,
    val maxProperties: Int? = null,
    val additionalProperties: Any? = null,
    val properties: Map<String, OpenApiSchema>? = null,
    val required: List<String>? = null,
    val oneOf: List<OpenApiSchema>? = null,
    val allOf: List<OpenApiSchema>? = null,
    val discriminator: OpenApiDiscriminator? = null,
)
