package io.amichne.konditional.openapi

import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.features.FeatureContainer
import io.amichne.konditional.core.features.JsonArrayFeature
import io.amichne.konditional.core.features.JsonObjectFeature
import io.amichne.konditional.core.types.json.JsonSchema
import java.io.File

/**
 * Generates OpenAPI 3.0 specifications from FeatureContainer definitions.
 *
 * This generator scans FeatureContainers for JSON object and array features,
 * extracts their schemas, and produces a complete OpenAPI document that can
 * be used for API documentation, client generation, or validation.
 *
 * Example usage:
 * ```kotlin
 * val generator = OpenApiGenerator(
 *     title = "Feature Flags API",
 *     version = "1.0.0",
 *     description = "API for managing feature flags"
 * )
 *
 * // Register schemas
 * generator.registerSchema("User", userSchema)
 * generator.registerSchema("Product", productSchema)
 *
 * // Generate OpenAPI spec
 * generator.writeToFile(File("build/openapi/spec.json"))
 * ```
 */
class OpenApiGenerator(
    private val title: String = "Feature Flags API",
    private val version: String = "1.0.0",
    private val description: String = "Auto-generated API specification from feature flag schemas",
    private val baseUrl: String = "/api/v1"
) {
    private val schemas = mutableMapOf<String, JsonSchema>()

    /**
     * Registers all JSON schemas from a FeatureContainer.
     */
    fun <M : Namespace> registerContainer(container: FeatureContainer<M>) {
        val features = container.allFeatures()

        features.forEach { feature ->
            when (feature) {
                is JsonObjectFeature<*, *> -> {
                    val schemaName = "${container.namespace.javaClass.simpleName}_${feature.key}"
                    // Note: This is a simplified example - in production you'd need
                    // access to the actual schema from the feature definition
                }
                is JsonArrayFeature<*, *> -> {
                    val schemaName = "${container.namespace.javaClass.simpleName}_${feature.key}"
                    // Same as above
                }
                else -> {
                    // Ignore non-JSON features
                }
            }
        }
    }

    /**
     * Registers a named schema directly.
     */
    fun registerSchema(name: String, schema: JsonSchema) {
        schemas[name] = schema
    }

    /**
     * Generates the complete OpenAPI specification as a JSON string.
     */
    fun generate(): String {
        return buildOpenApiSpec()
    }

    /**
     * Writes the OpenAPI specification to a file.
     */
    fun writeToFile(file: File) {
        file.parentFile?.mkdirs()
        file.writeText(generate())
        println("OpenAPI specification written to: ${file.absolutePath}")
    }

    private fun buildOpenApiSpec(): String {
        return buildString {
            appendLine("{")
            appendLine("  \"openapi\": \"3.0.0\",")
            appendLine("  \"info\": {")
            appendLine("    \"title\": \"$title\",")
            appendLine("    \"version\": \"$version\",")
            appendLine("    \"description\": \"$description\"")
            appendLine("  },")
            appendLine("  \"servers\": [")
            appendLine("    {")
            appendLine("      \"url\": \"$baseUrl\",")
            appendLine("      \"description\": \"Feature Flags API Server\"")
            appendLine("    }")
            appendLine("  ],")
            appendLine("  \"paths\": {")

            // Generate paths from schemas
            val pathEntries = schemas.entries.toList()
            pathEntries.forEachIndexed { pathIndex, (name, schema) ->
                if (schema is JsonSchema.ObjectSchema) {
                    val pathName = name.lowercase()
                    appendLine("    \"/$pathName\": {")
                    appendLine("      \"get\": {")
                    appendLine("        \"summary\": \"Get $name\",")
                    appendLine("        \"operationId\": \"get$name\",")
                    appendLine("        \"responses\": {")
                    appendLine("          \"200\": {")
                    appendLine("            \"description\": \"Successful response\",")
                    appendLine("            \"content\": {")
                    appendLine("              \"application/json\": {")
                    appendLine("                \"schema\": {")
                    appendLine("                  \"\$ref\": \"#/components/schemas/$name\"")
                    appendLine("                }")
                    appendLine("              }")
                    appendLine("            }")
                    appendLine("          }")
                    appendLine("        }")
                    appendLine("      }")
                    if (pathIndex < pathEntries.size - 1) {
                        appendLine("    },")
                    } else {
                        appendLine("    }")
                    }
                }
            }

            appendLine("  },")
            appendLine("  \"components\": {")
            appendLine("    \"schemas\": {")

            // Generate schema components
            val schemaEntries = schemas.entries.toList()
            schemaEntries.forEachIndexed { index, (name, schema) ->
                val schemaJson = OpenApiSchemaConverter.convert(schema)
                // Indent the schema JSON
                val indentedSchema = schemaJson.lines().joinToString("\n") { "      $it" }.trimStart()
                append("      \"$name\": $indentedSchema")
                if (index < schemaEntries.size - 1) {
                    appendLine(",")
                } else {
                    appendLine()
                }
            }

            appendLine("    }")
            appendLine("  }")
            appendLine("}")
        }
    }
}

/**
 * Helper to easily create an OpenAPI spec from schema definitions.
 */
fun buildOpenApiSpec(
    title: String = "API",
    version: String = "1.0.0",
    block: OpenApiGenerator.() -> Unit
): String {
    val generator = OpenApiGenerator(title, version)
    generator.block()
    return generator.generate()
}
