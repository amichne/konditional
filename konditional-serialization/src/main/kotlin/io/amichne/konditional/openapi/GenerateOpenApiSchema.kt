package io.amichne.konditional.openapi

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.nio.file.Files
import java.nio.file.Path

internal object GenerateOpenApiSchema {
    @JvmStatic
    fun main(args: Array<String>) {
        val outputPath = args.getOrNull(0) ?: "build/generated/openapi/konditional-schema.json"
        val version = args.getOrNull(1) ?: "unspecified"
        val title = args.getOrNull(2) ?: "Konditional Serialization Schema"

        val document = SerializationOpenApiDocument.document(version = version, title = title)
        val mapType = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        val moshi =
            Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
        val adapter = moshi.adapter<Map<String, Any?>>(mapType).indent("  ")

        val output = Path.of(outputPath)
        output.parent?.let { Files.createDirectories(it) }
        Files.writeString(output, adapter.toJson(document))
        println("Wrote OpenAPI schema to $output")
    }
}
