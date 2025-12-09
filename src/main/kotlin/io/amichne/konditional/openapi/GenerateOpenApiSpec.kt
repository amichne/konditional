package io.amichne.konditional.openapi

import io.amichne.konditional.core.dsl.jsonObject
import java.io.File

/**
 * Main entry point for generating OpenAPI specifications.
 *
 * This can be invoked from a Gradle task to generate OpenAPI specs from
 * your JSONSchema definitions.
 *
 * Usage from Gradle:
 * ```
 * ./gradlew generateOpenApiSpec
 * ```
 *
 * Or programmatically:
 * ```
 * GenerateOpenApiSpec.main(arrayOf("build/openapi/spec.json"))
 * ```
 */
object GenerateOpenApiSpec {

    @JvmStatic
    fun main(args: Array<String>) {
        val outputPath = args.getOrNull(0) ?: "build/openapi/api-spec.json"
        val outputFile = File(outputPath)

        println("Generating OpenAPI specification...")
        println("Output: ${outputFile.absolutePath}")

        // Create generator
        val generator = OpenApiGenerator(
            title = "Konditional Feature Flags API",
            version = "1.0.0",
            description = "API specification auto-generated from Konditional feature flag schemas",
            baseUrl = "https://api.example.com/v1"
        )

        // Register all schemas from the registry
        SchemaRegistry.exportTo(generator)

        // If registry is empty, add some examples
        if (SchemaRegistry.size() == 0) {
            println("Warning: No schemas registered in SchemaRegistry")
            println("Adding example schemas for demonstration...")

            // Example schemas
            val userSchema = jsonObject {
                requiredField("id") { int() }
                requiredField("username") { string() }
                requiredField("email") { string() }
                optionalField("firstName") { string() }
                optionalField("lastName") { string() }
                optionalField("active") { boolean() }
            }

            val addressSchema = jsonObject {
                optionalField("street") { string() }
                optionalField("city") { string() }
                optionalField("state") { string() }
                optionalField("zipCode") { string() }
                optionalField("country") { string() }
            }

            val configSchema = jsonObject {
                requiredField("enabled") { boolean() }
                optionalField("maxRetries") { int() }
                optionalField("timeout") { double() }
                optionalField("tags") { array { string() } }
            }

            generator.registerSchema("User", userSchema)
            generator.registerSchema("Address", addressSchema)
            generator.registerSchema("FeatureConfig", configSchema)

            println("Registered ${listOf("User", "Address", "FeatureConfig").size} example schemas")
        } else {
            println("Registered ${SchemaRegistry.size()} schemas from SchemaRegistry")
        }

        // Generate and write
        generator.writeToFile(outputFile)

        println("✓ OpenAPI specification generated successfully!")
        println("  Location: ${outputFile.absolutePath}")
        println("  Size: ${outputFile.length()} bytes")
    }
}

/**
 * Example of how to register your schemas for OpenAPI generation.
 *
 * Place this in your application code, typically in a file like AppSchemas.kt:
 *
 * ```kotlin
 * object AppSchemas {
 *     val userSchema = jsonObject {
 *         requiredField("id") { int() }
 *         requiredField("name") { string() }
 *     }
 *
 *     val productSchema = jsonObject {
 *         requiredField("sku") { string() }
 *         requiredField("price") { double() }
 *         optionalField("description") { string() }
 *     }
 *
 *     init {
 *         SchemaRegistry.register("User", userSchema)
 *         SchemaRegistry.register("Product", productSchema)
 *     }
 * }
 * ```
 */
@Suppress("unused")
private object ExampleSchemaRegistration
