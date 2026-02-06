package io.amichne.konditional.openapi

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.nio.file.Files
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GenerateOpenApiSchemaTest {
    @Test
    fun `main writes OpenAPI schema file with requested title and version`() {
        val outputDirectory = createTempDirectory(prefix = "konditional-openapi-test")
        val outputPath = outputDirectory.resolve("schema.json")

        try {
            GenerateOpenApiSchema.main(
                arrayOf(outputPath.toString(), "9.9.9", "Custom Serialization Schema"),
            )

            assertTrue(Files.exists(outputPath))
            val json = Files.readString(outputPath)
            val parsed = parseJsonObject(json)
            val info = assertIs<Map<*, *>>(parsed["info"])

            assertEquals("3.0.3", parsed["openapi"])
            assertEquals("Custom Serialization Schema", info["title"])
            assertEquals("9.9.9", info["version"])
        } finally {
            outputDirectory.toFile().deleteRecursively()
        }
    }

    private fun parseJsonObject(json: String): Map<String, Any?> {
        val mapType = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        val moshi =
            Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
        val adapter = moshi.adapter<Map<String, Any?>>(mapType)
        return requireNotNull(adapter.fromJson(json)) { "Expected non-null JSON object" }
    }
}
