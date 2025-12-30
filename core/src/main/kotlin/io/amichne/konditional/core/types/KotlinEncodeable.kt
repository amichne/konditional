package io.amichne.konditional.core.types

import io.amichne.kontracts.schema.JsonSchema
import io.amichne.kontracts.schema.ObjectTraits

/**
 * Interface for custom types that can be encoded with schema validation.
 *
 * Implement this interface to use a custom structured type as a feature flag value.
 * The schema is used for validation and JSON conversion at the library boundary.
 *
 * The generic type parameter [S] allows for different schema types, though
 * object schemas are the most common use case for data classes.
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
 * ) : KotlinEncodeable<ObjectSchema> {
 *     override val schema = schemaRoot {
 *         ::theme of { minLength = 1 }
 *         ::notificationsEnabled of { default = true }
 *         ::maxRetries of { minimum = 0 }
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
 * ) : KotlinEncodeable<ObjectSchema> {
 *     override val schema = Companion.schema
 *
 *     companion object {
 *         val schema = schemaRoot {
 *             ::theme of { minLength = 1 }
 *             ::notificationsEnabled of { default = true }
 *             ::maxRetries of { minimum = 0 }
 *         }
 *     }
 * }
 * ```
 *
 * @param S The schema type used for validation (must be an object schema)
 */
interface KotlinEncodeable<out S> where S : JsonSchema<*>, S : ObjectTraits {
    /**
     * The schema defining the structure and validation rules for this custom type.
     */
    val schema: S
}
