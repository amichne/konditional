package io.amichne.konditional.core.types

import io.amichne.kontracts.schema.JsonSchema

/**
 * Interface for custom types that can be encoded with schema validation.
 *
 * This interface follows the Encodeable pattern used throughout the type system
 * (BooleanEncodeable, StringEncodeable, etc.) and enables type-safe encoding
 * of user-defined types with compile-time schema validation.
 *
 * The generic type parameter [S] allows for different schema types, though
 * [JsonSchema.ObjectSchema] is the most common use case for data classes.
 *
 * ## Usage with Data Classes
 *
 * Custom types implementing this interface can be used as feature flag values,
 * providing structured, type-safe configuration with full schema validation.
 *
 * Requirements:
 * - Must provide a schema property defining the structure
 * - All properties should have default values
 * - Properties must be of supported types (primitives, enums, JsonValue, nested KotlinEncodeable)
 *
 * Example:
 * ```kotlin
 * data class UserSettings(
 *     val theme: String = "light",
 *     val notificationsEnabled: Boolean = true,
 *     val maxRetries: Int = 3
 * ) : KotlinEncodeable<JsonSchema.ObjectSchema> {
 *     override val schema = jsonObject {
 *         field("theme", required = true, default = "light") { string() }
 *         field("notificationsEnabled", required = true, default = true) { boolean() }
 *         field("maxRetries", required = true, default = 3) { int() }
 *     }
 * }
 * ```
 *
 * Alternatively, define the schema in the companion object for reuse:
 * ```kotlin
 * data class UserSettings(
 *     val theme: String = "light",
 *     val notificationsEnabled: Boolean = true,
 *     val maxRetries: Int = 3
 * ) : KotlinEncodeable<JsonSchema.ObjectSchema> {
 *     override val schema = Companion.schema
 *
 *     companion object {
 *         val schema = jsonObject {
 *             field("theme", required = true, default = "light") { string() }
 *             field("notificationsEnabled", required = true, default = true) { boolean() }
 *             field("maxRetries", required = true, default = 3) { int() }
 *         }
 *     }
 * }
 * ```
 *
 * @param S The schema type used for validation (typically [JsonSchema.ObjectSchema])
 */
interface KotlinEncodeable<out S : JsonSchema> {
    /**
     * The schema defining the structure and validation rules for this custom type.
     */
    val schema: S
}

/**
 * Type alias for the common case of custom data classes with object schemas.
 * This provides backwards compatibility and ergonomic usage for the most common pattern.
 */
typealias JsonSchemaClass = KotlinEncodeable<JsonSchema.ObjectSchema>
