package io.amichne.konditional.core.types

import io.amichne.konditional.core.types.json.JsonSchema

/**
 * Interface for data classes that can be used as configuration values.
 *
 * Data classes implementing this interface can be used as feature flag values,
 * providing structured, type-safe configuration with full definition validation.
 *
 * Requirements for data classes implementing this interface:
 * - Must provide a definition property defining the structure
 * - All properties must have default values
 * - Properties must be of supported types (primitives, enums, JsonValue, nested SchemaDefined)
 *
 * Example:
 * ```kotlin
 * data class UserSettings(
 *     val theme: String = "light",
 *     val notificationsEnabled: Boolean = true,
 *     val maxRetries: Int = 3
 * ) : SchemaDefined {
 *     override val definition = jsonObject {
 *         field("theme", required = true, default = "light") { string() }
 *         field("notificationsEnabled", required = true, default = true) { boolean() }
 *         field("maxRetries", required = true, default = 3) { int() }
 *     }
 * }
 * ```
 *
 * Alternatively, define the definition in the companion object for reuse:
 * ```kotlin
 * data class UserSettings(
 *     val theme: String = "light",
 *     val notificationsEnabled: Boolean = true,
 *     val maxRetries: Int = 3
 * ) : SchemaDefined {
 *     override val definition = Companion.definition
 *
 *     companion object {
 *         val definition = jsonObject {
 *             field("theme", required = true, default = "light") { string() }
 *             field("notificationsEnabled", required = true, default = true) { boolean() }
 *             field("maxRetries", required = true, default = 3) { int() }
 *         }
 *     }
 * }
 * ```
 */
interface Defined<S : JsonSchema> {
    val definition: S

    /**
     * The JSON definition defining the structure of this data class.
     * Must be an ObjectSchema that defines all fields and their types.
     */
}
