package io.amichne.konditional.core.types

import io.amichne.kontracts.schema.JsonSchema

/**
 * Interface for custom types that can be encoded with schema validation.
 *
 * Implement this interface to use a custom structured type as a feature flag value.
 * The schema is used for validation and JSON conversion at the library boundary.
 *
 * ## Supported schema types
 *
 * ### Object schemas (data classes)
 * The canonical use-case: a data class with named fields validated by an [io.amichne.kontracts.schema.ObjectSchema].
 * ```kotlin
 * data class UserSettings(
 *     val theme: String = "light",
 *     val notificationsEnabled: Boolean = true,
 *     val maxRetries: Int = 3,
 * ) : Konstrained<ObjectSchema> {
 *     override val schema = schema {
 *         ::theme of { minLength = 1 }
 *         ::notificationsEnabled of { default = true }
 *         ::maxRetries of { minimum = 0 }
 *     }
 * }
 * ```
 *
 * ### Primitive schemas (value classes — recommended)
 * A single primitive (String, Boolean, Int, Double) wrapped with compile-time constraints.
 * Use `@JvmInline value class` to enforce that exactly one underlying property exists;
 * this cannot be enforced at compile time without a Gradle plugin, but is validated at runtime.
 * ```kotlin
 * @JvmInline
 * value class Email(val raw: String) : Konstrained<StringSchema> {
 *     override val schema = stringSchema { pattern = "^[^@]+@[^@]+$" }
 * }
 *
 * @JvmInline
 * value class RetryCount(val value: Int) : Konstrained<IntSchema> {
 *     override val schema = intSchema { minimum = 0; maximum = 10 }
 * }
 * ```
 *
 * ### Array schemas (value classes — recommended)
 * A list of values validated against an element schema.
 * ```kotlin
 * @JvmInline
 * value class Tags(val values: List<String>) : Konstrained<ArraySchema<String>> {
 *     override val schema = arraySchema { elementSchema(stringSchema { minLength = 1 }) }
 * }
 * ```
 *
 * ## Invariants
 * - For primitive and array schemas the implementing class **must** have exactly one property
 *   whose type matches the schema's Kotlin backing type. Violations produce a descriptive
 *   [IllegalArgumentException] at encode/decode time.
 * - Determinism: [schema] must return the same value for every call on the same instance.
 * - Boundary discipline: raw external values (JSON, HTTP) are never accepted as trusted;
 *   all construction goes through the schema-validated codec.
 *
 * @param S The schema type that describes the structure and constraints of this type.
 *   Supported: [io.amichne.kontracts.schema.ObjectSchema], [io.amichne.kontracts.schema.StringSchema],
 *   [io.amichne.kontracts.schema.BooleanSchema], [io.amichne.kontracts.schema.IntSchema],
 *   [io.amichne.kontracts.schema.DoubleSchema], [io.amichne.kontracts.schema.ArraySchema].
 */
interface Konstrained<out S : JsonSchema<*>> {
    /**
     * The schema defining the structure and validation rules for this custom type.
     *
     * Must be deterministic: the same schema value must be returned on every call.
     */
    val schema: S
}
