package io.amichne.konditional.serialization

import io.amichne.konditional.core.result.ParseResult
import io.amichne.kontracts.value.JsonValue

/**
 * Type-safe serializer for custom value types.
 *
 * Replaces reflection-based serialization with explicit, registered serializers
 * for each custom type. This provides:
 * - **No reflection**: Explicit encoding/decoding logic
 * - **Type safety**: Generic type parameter ensures correct value type
 * - **Extensibility**: Users can register custom serializers for their types
 * - **Performance**: No reflection overhead at runtime
 *
 * ## Usage
 *
 * ### Define a Custom Type
 * ```kotlin
 * data class RetryPolicy(
 *     val maxAttempts: Int,
 *     val backoffMs: Double
 * ) : KotlinEncodeable<ObjectSchema> {
 *     override val schema = schemaRoot {
 *         ::maxAttempts of { minimum = 1 }
 *         ::backoffMs of { minimum = 0.0 }
 *     }
 *
 *     companion object {
 *         val serializer = object : TypeSerializer<RetryPolicy> {
 *             override fun encode(value: RetryPolicy): JsonValue =
 *                 jsonObject {
 *                     "maxAttempts" to value.maxAttempts
 *                     "backoffMs" to value.backoffMs
 *                 }
 *
 *             override fun decode(json: JsonValue): ParseResult<RetryPolicy> =
 *                 when (json) {
 *                     is JsonObject -> {
 *                         val maxAttempts = json.fields["maxAttempts"]?.asInt()
 *                         val backoffMs = json.fields["backoffMs"]?.asDouble()
 *
 *                         if (maxAttempts != null && backoffMs != null) {
 *                             ParseResult.Success(RetryPolicy(maxAttempts, backoffMs))
 *                         } else {
 *                             ParseResult.Failure(ParseError.InvalidSnapshot("Missing required fields"))
 *                         }
 *                     }
 *                     else -> ParseResult.Failure(ParseError.InvalidSnapshot("Expected JsonObject"))
 *                 }
 *         }
 *     }
 * }
 * ```
 *
 * ### Register the Serializer
 * ```kotlin
 * // At application startup or in companion object init block
 * SerializerRegistry.register(RetryPolicy::class, RetryPolicy.serializer)
 * ```
 *
 * ### Use in Feature Flags
 * ```kotlin
 * object PolicyFlags : Namespace("policy") {
 *     val retryPolicy by custom(default = RetryPolicy(3, 1000.0)) {
 *         rule(RetryPolicy(5, 2000.0)) {
 *             platforms(Platform.WEB)
 *         }
 *     }
 * }
 * ```
 *
 * ## Built-in Serializers
 *
 * The following types have built-in serializers (no registration needed):
 * - `Boolean`, `String`, `Int`, `Double` (primitives)
 * - `Enum<*>` (via enum class name + constant name)
 *
 * @param T The type being serialized/deserialized
 */
interface TypeSerializer<T : Any> {
    /**
     * Encodes a value to JSON.
     *
     * This replaces reflection-based property extraction with explicit encoding logic.
     *
     * @param value The value to encode
     * @return JsonValue representation
     */
    fun encode(value: T): JsonValue

    /**
     * Decodes JSON to a value.
     *
     * Returns [ParseResult] for type-safe error handling following parse-don't-validate principles.
     * Never throws exceptions - all errors are captured in [ParseResult.Failure].
     *
     * This replaces reflection-based constructor calls with explicit decoding logic.
     *
     * @param json JSON to decode
     * @return ParseResult containing either the decoded value or a structured error
     */
    fun decode(json: JsonValue): ParseResult<T>
}
