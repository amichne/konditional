package io.amichne.konditional.openapi

import io.amichne.konditional.core.types.json.JsonSchema

/**
 * Registry for collecting and managing JSONSchema definitions for OpenAPI generation.
 *
 * This registry allows you to collect schemas from across your codebase in a central
 * location, which can then be used to generate OpenAPI specifications.
 *
 * Example usage:
 * ```kotlin
 * // In your schema definitions file
 * object AppSchemas {
 *     val userSchema = jsonObject {
 *         requiredField("id") { int() }
 *         requiredField("name") { string() }
 *         optionalField("email") { string() }
 *     }
 *
 *     val configSchema = jsonObject {
 *         requiredField("theme") { enum<Theme>() }
 *         optionalField("notifications") { boolean() }
 *     }
 *
 *     // Register them
 *     init {
 *         SchemaRegistry.register("User", userSchema)
 *         SchemaRegistry.register("Config", configSchema)
 *     }
 * }
 *
 * // Later, generate OpenAPI spec
 * fun main() {
 *     val generator = OpenApiGenerator()
 *     SchemaRegistry.exportTo(generator)
 *     generator.writeToFile(File("build/openapi/spec.json"))
 * }
 * ```
 */
object SchemaRegistry {
    private val schemas = mutableMapOf<String, JsonSchema>()
    private val descriptions = mutableMapOf<String, String>()

    /**
     * Registers a schema with a name.
     *
     * @param name The schema name (will be used as the component name in OpenAPI)
     * @param schema The JsonSchema to register
     * @param description Optional description for the schema
     */
    fun register(name: String, schema: JsonSchema, description: String? = null) {
        schemas[name] = schema
        description?.let { descriptions[name] = it }
    }

    /**
     * Registers multiple schemas at once.
     */
    fun registerAll(schemasMap: Map<String, JsonSchema>) {
        schemas.putAll(schemasMap)
    }

    /**
     * Gets all registered schemas.
     */
    fun getAllSchemas(): Map<String, JsonSchema> = schemas.toMap()

    /**
     * Gets a specific schema by name.
     */
    fun getSchema(name: String): JsonSchema? = schemas[name]

    /**
     * Gets the description for a schema.
     */
    fun getDescription(name: String): String? = descriptions[name]

    /**
     * Exports all registered schemas to an OpenApiGenerator.
     */
    fun exportTo(generator: OpenApiGenerator) {
        schemas.forEach { (name, schema) ->
            generator.registerSchema(name, schema)
        }
    }

    /**
     * Clears all registered schemas (useful for testing).
     */
    fun clear() {
        schemas.clear()
        descriptions.clear()
    }

    /**
     * Returns the number of registered schemas.
     */
    fun size(): Int = schemas.size
}

/**
 * Annotation to mark schemas that should be auto-registered.
 * In a more sophisticated implementation, this could be processed by a compiler plugin
 * or annotation processor to auto-register schemas.
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class RegisterSchema(
    val name: String = "",
    val description: String = ""
)
