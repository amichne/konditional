package io.amichne.konditional.server.core.openapi

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

internal object OpenApiJsonRenderer {
    private val moshi: Moshi =
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

    private val documentAdapter: JsonAdapter<OpenApiDocument> =
        moshi.adapter(OpenApiDocument::class.java).indent("  ")

    private val mapType = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)

    @Suppress("UNCHECKED_CAST")
    private val mapAdapter: JsonAdapter<Map<String, Any?>> =
        moshi.adapter<Map<String, Any?>>(mapType).indent("  ")

    fun render(document: OpenApiDocument): String =
        requireNotNull(documentAdapter.toJson(document)) {
            "Failed to render OpenAPI document."
        }.let { serialized ->
            if (serialized.endsWith("\n")) serialized else "$serialized\n"
        }

    fun parse(json: String): Map<String, Any?> =
        requireNotNull(mapAdapter.fromJson(json)) {
            "Failed to parse OpenAPI JSON document."
        }
}
