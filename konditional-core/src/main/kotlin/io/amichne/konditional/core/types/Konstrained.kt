package io.amichne.konditional.core.types

import io.amichne.kontracts.dsl.booleanSchema
import io.amichne.kontracts.dsl.doubleSchema
import io.amichne.kontracts.dsl.intSchema
import io.amichne.kontracts.dsl.stringSchema
import io.amichne.kontracts.schema.BooleanSchema
import io.amichne.kontracts.schema.DoubleSchema
import io.amichne.kontracts.schema.IntSchema
import io.amichne.kontracts.schema.JsonSchema
import io.amichne.kontracts.schema.StringSchema

// Default schema singletons used by the As* interface family.
// Each is a plain schema with no constraints — implementors override to add them.
@Suppress("UNCHECKED_CAST")
private val defaultStringSchema: StringSchema = stringSchema() as StringSchema

@Suppress("UNCHECKED_CAST")
private val defaultIntSchema: IntSchema = intSchema() as IntSchema

@Suppress("UNCHECKED_CAST")
private val defaultBooleanSchema: BooleanSchema = booleanSchema() as BooleanSchema

@Suppress("UNCHECKED_CAST")
private val defaultDoubleSchema: DoubleSchema = doubleSchema() as DoubleSchema

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
 * ) : Konstrained.Object<ObjectSchema> {
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
 * value class Email(override val value: String) : Konstrained.Primitive.String<StringSchema> {
 *     override val schema = stringSchema { pattern = "^[^@]+@[^@]+$" }
 * }
 *
 * @JvmInline
 * value class RetryCount(override val value: Int) : Konstrained.Primitive.Int<IntSchema> {
 *     override val schema = intSchema { minimum = 0; maximum = 10 }
 * }
 * ```
 *
 * ### Array schemas (value classes — recommended)
 * A list of values validated against an element schema.
 * ```kotlin
 * @JvmInline
 * value class Tags(override val values: List<String>) : Konstrained.Array<ArraySchema<String>, String> {
 *     override val schema = arraySchema { elementSchema(stringSchema { minLength = 1 }) }
 * }
 * ```
 *
 * ### Custom-type adapters (As* family — for non-primitive domain types)
 * When the domain value type `T` is not itself a JSON primitive (e.g., `LocalDate`, `UUID`,
 * a newtype wrapper, etc.), use the [AsString], [AsInt], [AsBoolean], or [AsDouble] sub-interfaces
 * to declare the JSON wire format and provide the encode/decode logic.
 *
 * The implementing class provides [AsString.encode] as an instance method and supplies a
 * companion object implementing the matching [StringDecoder] (or [IntDecoder], etc.) to
 * support construction from the raw wire value.
 *
 * Schema declaration is optional — a plain, unconstrained schema is used by default.
 * Override [AsString.schema] (or the matching property) to add format, pattern, or range
 * constraints.
 *
 * ```kotlin
 * @JvmInline
 * value class ExpirationDate(val value: LocalDate) : Konstrained.AsString<LocalDate> {
 *     override fun encode(): String = value.toString()
 *     // Schema defaults to StringSchema {}; override to constrain:
 *     // override val schema = stringSchema { format = "date" } as StringSchema
 *
 *     companion object : Konstrained.StringDecoder<ExpirationDate> {
 *         override fun decode(raw: String): ExpirationDate =
 *             ExpirationDate(LocalDate.parse(raw))
 *     }
 * }
 * ```
 *
 * ## Invariants
 * - For primitive and array schemas the implementing class **must** have exactly one property
 *   whose type matches the schema's Kotlin backing type. Violations produce a descriptive
 *   [IllegalArgumentException] at encode/decode time.
 * - For [AsString] / [AsInt] / [AsBoolean] / [AsDouble] schemas the companion object **must**
 *   implement the corresponding [StringDecoder] / [IntDecoder] / [BooleanDecoder] / [DoubleDecoder]
 *   to enable deserialization from a snapshot.
 * - Determinism: [schema] must return a value-equal result on every call. Creating a new schema
 *   instance per call is acceptable as long as its properties are identical each time.
 * - Boundary discipline: raw external values (JSON, HTTP) are never accepted as trusted;
 *   all construction goes through the schema-validated codec.
 *
 * @param S The schema type that describes the structure and constraints of this type.
 *   Supported: [io.amichne.kontracts.schema.ObjectSchema], [io.amichne.kontracts.schema.StringSchema],
 *   [io.amichne.kontracts.schema.BooleanSchema], [io.amichne.kontracts.schema.IntSchema],
 *   [io.amichne.kontracts.schema.DoubleSchema], [io.amichne.kontracts.schema.ArraySchema].
 */
sealed interface Konstrained<out S : JsonSchema<*>> {
    /**
     * The schema defining the structure and validation rules for this custom type.
     *
     * Must be deterministic: the same schema value must be returned on every call.
     */
    val schema: S

    // -------------------------------------------------------------------------
    // Primitive family — value IS a JSON primitive
    // -------------------------------------------------------------------------

    sealed interface Primitive<S : JsonSchema<*>, V> : Konstrained<S> {
        /**
         * The single underlying value of this primitive type.
         *
         * Must be the only property of the implementing class, and its type must match the schema's backing type.
         */
        val value: V

        interface Int<S : JsonSchema<*>> : Primitive<S, kotlin.Int>
        interface String<S : JsonSchema<*>> : Primitive<S, kotlin.String>
        interface Boolean<S : JsonSchema<*>> : Primitive<S, kotlin.Boolean>
        interface Double<S : JsonSchema<*>> : Primitive<S, kotlin.Double>
    }

    interface Object<S : JsonSchema<*>> : Konstrained<S>
    interface Array<S : JsonSchema<*>, E> : Konstrained<S> {
        /**
         * The list of values in this array type.
         *
         * Must be the only property of the implementing class, and its type must match the schema's backing type.
         */
        val values: List<E>
    }

    // -------------------------------------------------------------------------
    // Adapted family — domain type T is encoded AS a JSON primitive
    //
    // Use these when T is not itself a JSON primitive (e.g. LocalDate, UUID).
    // The [encode] method converts T → wire primitive; the companion [StringDecoder]
    // (or equivalent) converts wire primitive → T and constructs the value class.
    // -------------------------------------------------------------------------

    /**
     * Marker for a type whose domain value [T] is serialized as a JSON **string**.
     *
     * - [encode] (instance, abstract) converts the wrapped value to its string representation.
     * - [schema] has a default of plain [StringSchema] — override to add constraints such as
     *   `format`, `pattern`, or `minLength`.
     * - The companion object of the implementing class **must** implement [StringDecoder] so
     *   that the serialization layer can reconstruct instances from raw strings.
     *
     * ### Invariants
     * - [encode] must be deterministic: same [T] value → same [String] output, always.
     * - [StringDecoder.decode] must be the left-inverse of [encode]:
     *   `decode(encode(x)).value == x`.
     *
     * @param T The domain type wrapped by this value class.
     */
    interface AsString<T : Any> : Konstrained<StringSchema> {
        /** The domain value held by this instance. */
        val value: T

        /**
         * Encodes [value] to its JSON string representation.
         *
         * Must be pure and deterministic.
         */
        fun encode(): kotlin.String

        /**
         * The [StringSchema] used to validate and describe the wire representation.
         *
         * Defaults to an unconstrained string schema. Override to add constraints:
         * ```kotlin
         * override val schema = stringSchema { format = "date" } as StringSchema
         * ```
         */
        override val schema: StringSchema get() = defaultStringSchema
    }

    /**
     * Marker for a type whose domain value [T] is serialized as a JSON **integer**.
     *
     * - [encode] (instance, abstract) converts the wrapped value to its integer representation.
     * - [schema] has a default of plain [IntSchema] — override to add `minimum`/`maximum`.
     * - The companion object of the implementing class **must** implement [IntDecoder].
     *
     * @param T The domain type wrapped by this value class.
     */
    interface AsInt<T : Any> : Konstrained<IntSchema> {
        val value: T

        fun encode(): kotlin.Int

        override val schema: IntSchema get() = defaultIntSchema
    }

    /**
     * Marker for a type whose domain value [T] is serialized as a JSON **boolean**.
     *
     * - [encode] (instance, abstract) converts the wrapped value to its boolean representation.
     * - [schema] has a default of plain [BooleanSchema] — override to add a `default`.
     * - The companion object of the implementing class **must** implement [BooleanDecoder].
     *
     * @param T The domain type wrapped by this value class.
     */
    interface AsBoolean<T : Any> : Konstrained<BooleanSchema> {
        val value: T

        fun encode(): kotlin.Boolean

        override val schema: BooleanSchema get() = defaultBooleanSchema
    }

    /**
     * Marker for a type whose domain value [T] is serialized as a JSON **number (double)**.
     *
     * - [encode] (instance, abstract) converts the wrapped value to its double representation.
     * - [schema] has a default of plain [DoubleSchema] — override to add `minimum`/`maximum`.
     * - The companion object of the implementing class **must** implement [DoubleDecoder].
     *
     * @param T The domain type wrapped by this value class.
     */
    interface AsDouble<T : Any> : Konstrained<DoubleSchema> {
        val value: T

        fun encode(): kotlin.Double

        override val schema: DoubleSchema get() = defaultDoubleSchema
    }

    // -------------------------------------------------------------------------
    // Decoder interfaces — implement on the companion object of As* types
    //
    // These are the "fromJson" side of the adapter contract, analogous to a
    // Moshi @FromJson adapter. Each companion decoder reconstructs the full
    // value-class instance from the raw JSON primitive.
    // -------------------------------------------------------------------------

    /**
     * Companion-side decoder for [AsString] types.
     *
     * Implement on the **companion object** of a class that implements [AsString]:
     * ```kotlin
     * companion object : Konstrained.StringDecoder<ExpirationDate> {
     *     override fun decode(raw: String): ExpirationDate =
     *         ExpirationDate(LocalDate.parse(raw))
     * }
     * ```
     *
     * ### Contract
     * - [decode] must be the inverse of the instance's [AsString.encode]:
     *   `instance.value == decode(instance.encode()).value`.
     * - [decode] must throw a descriptive exception (not return null) if [raw] is invalid.
     *
     * @param V The [AsString] implementation type produced by [decode].
     */
    interface StringDecoder<out V : AsString<*>> {
        fun decode(raw: kotlin.String): V
    }

    /**
     * Companion-side decoder for [AsInt] types.
     *
     * @param V The [AsInt] implementation type produced by [decode].
     */
    interface IntDecoder<out V : AsInt<*>> {
        fun decode(raw: kotlin.Int): V
    }

    /**
     * Companion-side decoder for [AsBoolean] types.
     *
     * @param V The [AsBoolean] implementation type produced by [decode].
     */
    interface BooleanDecoder<out V : AsBoolean<*>> {
        fun decode(raw: kotlin.Boolean): V
    }

    /**
     * Companion-side decoder for [AsDouble] types.
     *
     * @param V The [AsDouble] implementation type produced by [decode].
     */
    interface DoubleDecoder<out V : AsDouble<*>> {
        fun decode(raw: kotlin.Double): V
    }
}
