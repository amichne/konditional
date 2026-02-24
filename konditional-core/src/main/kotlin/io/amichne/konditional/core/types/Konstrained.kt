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
 * a newtype wrapper, etc.), use the [AsString], [AsInt], [AsBoolean], or [AsDouble]
 * sub-interfaces to declare the JSON wire format and supply encode/decode logic.
 *
 * Both [AsString.encode] (instance → wire) and [AsString.decode] (wire → instance) are
 * declared directly on the interface, making the full codec contract visible. Shared logic
 * can be extracted into standalone [Encoder] and [Decoder] objects and reused across types:
 *
 * ```kotlin
 * private val localDateDecoder = Konstrained.Decoder<String, LocalDate> { LocalDate.parse(it) }
 *
 * @JvmInline
 * value class ExpirationDate(val value: LocalDate) : Konstrained.AsString<LocalDate, ExpirationDate> {
 *     override fun encode(): String = value.toString()
 *     override fun decode(raw: String): ExpirationDate = ExpirationDate(localDateDecoder.decode(raw))
 *
 *     companion object : Konstrained.Decoder<String, ExpirationDate> {
 *         override fun decode(raw: String) = ExpirationDate(localDateDecoder.decode(raw))
 *     }
 * }
 * ```
 *
 * The companion object implementing [Decoder] allows the serialization codec to reconstruct
 * instances from snapshots (where no prototype is available). Schema declaration is optional;
 * the default is an unconstrained schema. Override [AsString.schema] to add constraints.
 *
 * ## Invariants
 * - For primitive and array schemas the implementing class **must** have exactly one property
 *   whose type matches the schema's Kotlin backing type. Violations produce a descriptive
 *   [IllegalArgumentException] at encode/decode time.
 * - For [AsString] / [AsInt] / [AsBoolean] / [AsDouble] types whose companion implements
 *   [Decoder], [Decoder.decode] must be the left-inverse of [AsString.encode]:
 *   `decode(encode(x)).value == x`.
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
    // Composable codec interfaces — standalone building blocks for the As* family
    // -------------------------------------------------------------------------

    /**
     * Converts a domain value [T] to its JSON-primitive wire representation [P].
     *
     * Implement as a standalone `object` or `fun interface` lambda to share encoding
     * logic across multiple [AsString] / [AsInt] / [AsBoolean] / [AsDouble] types that
     * wrap the same domain type:
     *
     * ```kotlin
     * val localDateEncoder = Konstrained.Encoder<LocalDate, String> { it.toString() }
     * ```
     *
     * ### Invariants
     * - [encode] must be pure and deterministic: same [T] → same [P] always.
     *
     * @param T The domain type being encoded.
     * @param P The JSON-primitive target type (`String`, `Int`, `Boolean`, or `Double`).
     */
    fun interface Encoder<T : Any, P : Any> {
        fun encode(value: T): P
    }

    /**
     * Reconstructs a [V] instance from a raw JSON-primitive value [P].
     *
     * Implement as a standalone `object` to share decoding logic across multiple types,
     * or supply from a **companion object** of an [AsString] / [AsInt] / [AsBoolean] /
     * [AsDouble] implementation to enable codec discoverability:
     *
     * ```kotlin
     * val localDateDecoder = Konstrained.Decoder<String, LocalDate> { LocalDate.parse(it) }
     *
     * companion object : Konstrained.Decoder<String, ExpirationDate> {
     *     override fun decode(raw: String) = ExpirationDate(localDateDecoder.decode(raw))
     * }
     * ```
     *
     * The serialization codec checks whether a class's companion object implements this
     * interface and uses it to reconstruct instances from snapshots (where no prototype
     * instance is available). If no companion [Decoder] is present, the codec falls back
     * to primary-constructor invocation — suitable only for [Primitive] types whose single
     * constructor parameter is the raw primitive itself.
     *
     * ### Invariants
     * - [decode] must be the left-inverse of the corresponding [Encoder.encode]:
     *   `decoder.decode(encoder.encode(x)) == x`.
     * - [decode] must throw a descriptive exception (not return `null`) if [raw] is invalid.
     *
     * @param P The raw JSON-primitive source type.
     * @param V The [Konstrained] value type produced by [decode].
     */
    fun interface Decoder<P : Any, out V : Any> {
        fun decode(raw: P): V
    }

    // -------------------------------------------------------------------------
    // Adapted family — domain type T is encoded AS a JSON primitive
    //
    // Use these when T is not itself a JSON primitive (e.g. LocalDate, UUID).
    //
    // Both encode() and decode() are declared on the interface, making the full
    // codec contract explicit and testable without relying on hidden conventions.
    //
    // Composition: delegate to shared Encoder / Decoder objects to avoid
    // duplicating parse/format logic across types that wrap the same domain type.
    // -------------------------------------------------------------------------

    /**
     * Marker for a type whose domain value [T] is serialized as a JSON **string**.
     *
     * - [encode] (instance) converts the wrapped value to its string wire form.
     * - [decode] (instance) reconstructs a new [V] instance from the raw wire string.
     *   Typically delegates to the companion [Decoder] for a single source of truth.
     * - [schema] defaults to an unconstrained [StringSchema]; override to add `format`,
     *   `pattern`, or `minLength` constraints.
     *
     * ## Codec discoverability
     *
     * The companion object of the implementing class **should** implement
     * [Konstrained.Decoder]`<String, V>` so the serialization codec can reconstruct
     * instances from snapshots without a prototype:
     *
     * ```kotlin
     * companion object : Konstrained.Decoder<String, ExpirationDate> {
     *     override fun decode(raw: String) = ExpirationDate(LocalDate.parse(raw))
     * }
     * ```
     *
     * ## Composition
     *
     * ```kotlin
     * private val localDateDecoder = Konstrained.Decoder<String, LocalDate> { LocalDate.parse(it) }
     *
     * @JvmInline
     * value class ExpirationDate(val value: LocalDate) : Konstrained.AsString<LocalDate, ExpirationDate> {
     *     override fun encode(): String = value.toString()
     *     override fun decode(raw: String): ExpirationDate = ExpirationDate(localDateDecoder.decode(raw))
     *     companion object : Konstrained.Decoder<String, ExpirationDate> {
     *         override fun decode(raw: String) = ExpirationDate(localDateDecoder.decode(raw))
     *     }
     * }
     * ```
     *
     * ### Invariants
     * - [encode] must be pure and deterministic: same [T] → same [String] always.
     * - [decode] must be the left-inverse of [encode]: `decode(encode(x)).value == x`.
     *
     * @param T The domain type wrapped by this value class.
     * @param V The concrete self-type; enables type-safe [decode] return without casting.
     */
    interface AsString<T : Any, V : AsString<T, V>> : Konstrained<StringSchema> {
        /** The domain value held by this instance. */
        val value: T

        /**
         * Encodes [value] to its JSON string wire representation.
         *
         * Must be pure and deterministic.
         */
        fun encode(): kotlin.String

        /**
         * Reconstructs a new [V] instance from the raw wire string [raw].
         *
         * Must be the left-inverse of [encode]: `decode(encode(x)).value == x`.
         * Must throw a descriptive exception if [raw] is invalid.
         */
        fun decode(raw: kotlin.String): V

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
     * See [AsString] for the full contract; this variant uses [Int] as the wire type.
     *
     * @param T The domain type wrapped by this value class.
     * @param V The concrete self-type; enables type-safe [decode] return.
     */
    interface AsInt<T : Any, V : AsInt<T, V>> : Konstrained<IntSchema> {
        val value: T
        fun encode(): kotlin.Int
        fun decode(raw: kotlin.Int): V
        override val schema: IntSchema get() = defaultIntSchema
    }

    /**
     * Marker for a type whose domain value [T] is serialized as a JSON **boolean**.
     *
     * See [AsString] for the full contract; this variant uses [Boolean] as the wire type.
     *
     * @param T The domain type wrapped by this value class.
     * @param V The concrete self-type; enables type-safe [decode] return.
     */
    interface AsBoolean<T : Any, V : AsBoolean<T, V>> : Konstrained<BooleanSchema> {
        val value: T
        fun encode(): kotlin.Boolean
        fun decode(raw: kotlin.Boolean): V
        override val schema: BooleanSchema get() = defaultBooleanSchema
    }

    /**
     * Marker for a type whose domain value [T] is serialized as a JSON **number (double)**.
     *
     * See [AsString] for the full contract; this variant uses [Double] as the wire type.
     *
     * @param T The domain type wrapped by this value class.
     * @param V The concrete self-type; enables type-safe [decode] return.
     */
    interface AsDouble<T : Any, V : AsDouble<T, V>> : Konstrained<DoubleSchema> {
        val value: T
        fun encode(): kotlin.Double
        fun decode(raw: kotlin.Double): V
        override val schema: DoubleSchema get() = defaultDoubleSchema
    }
}
