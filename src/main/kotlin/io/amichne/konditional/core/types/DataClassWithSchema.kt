package io.amichne.konditional.core.types

import io.amichne.konditional.core.types.json.JsonSchema

/**
 * Interface for data classes that can be used as configuration values.
 *
 * Data classes implementing this interface can be used as feature flag values,
 * providing structured, type-safe configuration with full schema validation.
 *
 * Requirements for data classes implementing this interface:
 * - Must provide a schema property defining the structure
 * - All properties must have default values
 * - Properties must be of supported types (primitives, enums, JsonValue, nested DataClassWithSchema)
 *
 * Example:
 * ```kotlin
 * data class UserSettings(
 *     val theme: String = "light",
 *     val notificationsEnabled: Boolean = true,
 *     val maxRetries: Int = 3
 * ) : DataClassWithSchema {
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
 * ) : DataClassWithSchema {
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
 */
interface DataClassWithSchema {
    /**
     * The JSON schema defining the structure of this data class.
     * Must be an ObjectSchema that defines all fields and their types.
     */
    val schema: JsonSchema.ObjectSchema
}
