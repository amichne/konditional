package io.amichne.konditional.context.axis


/**
 * A value that exists along some axis (environment, region, tenant, etc.).
 *
 * This interface represents a specific value within a dimensional axis. For example,
 * if `Environment` is an axis, then `PROD`, `STAGE`, and `DEV` would be AxisValue
 * implementations.
 *
 * ## Usage
 *
 * Define an enum that implements this interface:
 * ```kotlin
 * enum class Environment(override val id: String) : AxisValue<Environment> {
 *     PROD("prod"),
 *     STAGE("stage"),
 *     DEV("dev")
 * }
 * ```
 *
 * The `id` field must be stable and unique within the axis, as it's used for:
 * - Serialization and deserialization
 * - Rule matching and evaluation
 * - Storage and retrieval
 *
 * Each enum type that implements [AxisValue] must have exactly one [Axis] registered.
 *
 * @property id A stable, unique identifier for this value within its axis
 */
interface AxisValue<T> where T : Enum<T> {
    val id: String
}
