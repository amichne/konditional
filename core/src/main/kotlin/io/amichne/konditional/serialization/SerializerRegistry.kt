package io.amichne.konditional.serialization

import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.ParseResult
import io.amichne.kontracts.value.JsonBoolean
import io.amichne.kontracts.value.JsonNumber
import io.amichne.kontracts.value.JsonString
import io.amichne.kontracts.value.JsonValue
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * Thread-safe registry for type serializers.
 *
 * This registry replaces reflection-based serialization with explicit, registered
 * serializers for custom types. It maintains type safety while allowing extensibility.
 *
 * ## Thread Safety
 *
 * - **Concurrent registration**: Uses `ConcurrentHashMap` for thread-safe writes
 * - **Lock-free reads**: Serializer lookups require no synchronization
 * - **Registration order independent**: Can register serializers in any order
 *
 * ## Built-in Serializers
 *
 * Primitives and enums have built-in serializers (no registration needed):
 * - `Boolean` → `JsonBoolean`
 * - `String` → `JsonString`
 * - `Int` → `JsonNumber`
 * - `Double` → `JsonNumber`
 * - `Enum<*>` → `JsonString` (stores enum constant name)
 *
 * ## Usage
 *
 * ```kotlin
 * // Define a custom serializer
 * val retryPolicySerializer = object : TypeSerializer<RetryPolicy> {
 *     override fun encode(value: RetryPolicy): JsonValue = ...
 *     override fun decode(json: JsonValue): ParseResult<RetryPolicy> = ...
 * }
 *
 * // Register at application startup
 * SerializerRegistry.register(RetryPolicy::class, retryPolicySerializer)
 *
 * // Use in serialization (happens automatically)
 * val json = SerializerRegistry.encode(retryPolicy)
 * val result = SerializerRegistry.decode<RetryPolicy>(json)
 * ```
 *
 * ## Design Rationale
 *
 * **Why not use Moshi/kotlinx.serialization?**
 * - We need schema validation integration (via Kontracts)
 * - We want explicit control over JSON structure
 * - We avoid heavyweight serialization frameworks for simple use cases
 *
 * **Why separate from `Serializer<T>` interface?**
 * - `Serializer<T>` is for configuration snapshots (high-level)
 * - `TypeSerializer<T>` is for individual value types (low-level)
 * - Different responsibilities, different granularity
 */
object SerializerRegistry {
    private val serializers = ConcurrentHashMap<String, TypeSerializer<*>>()

    /**
     * Registers a serializer for a custom type.
     *
     * **Thread-safe:** Can be called concurrently from multiple threads.
     *
     * **Idempotent:** Registering the same type multiple times will overwrite
     * the previous serializer (last registration wins).
     *
     * @param kClass The class to register a serializer for
     * @param serializer The serializer implementation
     * @throws IllegalArgumentException if attempting to register a built-in type
     */
    fun <T : Any> register(kClass: KClass<T>, serializer: TypeSerializer<T>) {
        val className = kClass.java.name
        require(!isBuiltInType(kClass)) {
            "Cannot register serializer for built-in type: ${kClass.simpleName}. " +
                "Built-in types: Boolean, String, Int, Double, Enum"
        }
        serializers[className] = serializer
    }

    /**
     * Retrieves a registered serializer for a type.
     *
     * **Thread-safe:** Lock-free reads.
     *
     * Returns `null` if no serializer is registered for this type.
     * Built-in types (primitives, enums) are handled separately and will return `null` here.
     *
     * @param kClass The class to get a serializer for
     * @return The registered serializer, or null if not found
     */
    fun <T : Any> get(kClass: KClass<T>): TypeSerializer<T>? {
        @Suppress("UNCHECKED_CAST")
        return serializers[kClass.java.name] as? TypeSerializer<T>
    }

    /**
     * Retrieves a registered serializer by class name.
     *
     * Used during deserialization when we only have the class name string
     * (stored in `FlagValue.DataClassValue.dataClassName`).
     *
     * @param className Fully qualified class name
     * @return The registered serializer, or null if not found
     */
    internal fun getByClassName(className: String): TypeSerializer<*>? {
        return serializers[className]
    }

    /**
     * Encodes a value to JSON using the appropriate serializer.
     *
     * **Type-safe:** Uses registered serializer for custom types, built-in logic for primitives.
     *
     * @param value The value to encode
     * @return JsonValue representation
     * @throws IllegalArgumentException if no serializer is registered for a custom type
     */
    fun <T : Any> encode(value: T): JsonValue {
        // Handle built-in types
        return when (value) {
            is Boolean -> JsonBoolean(value)
            is String -> JsonString(value)
            is Int -> JsonNumber(value.toDouble())
            is Double -> JsonNumber(value)
            is Enum<*> -> JsonString(value.name)
            else -> {
                // Custom type - look up registered serializer
                @Suppress("UNCHECKED_CAST")
                val serializer = get(value::class as KClass<T>)
                    ?: error(
                        "No serializer registered for type: ${value::class.qualifiedName}. " +
                            "Register with SerializerRegistry.register(${value::class.simpleName}::class, serializer)"
                    )
                serializer.encode(value)
            }
        }
    }

    /**
     * Decodes JSON to a value using the appropriate serializer.
     *
     * **Type-safe:** Generic type parameter ensures correct return type.
     *
     * @param kClass The class to decode to
     * @param json JSON to decode
     * @return ParseResult containing either the decoded value or a structured error
     */
    fun <T : Any> decode(kClass: KClass<T>, json: JsonValue): ParseResult<T> {
        // Handle built-in types
        return when {
            kClass == Boolean::class -> {
                when (json) {
                    is JsonBoolean -> ParseResult.Success(json.value as T)
                    else -> ParseResult.Failure(ParseError.InvalidSnapshot("Expected JsonBoolean, got ${json::class.simpleName}"))
                }
            }
            kClass == String::class -> {
                when (json) {
                    is JsonString -> ParseResult.Success(json.value as T)
                    else -> ParseResult.Failure(ParseError.InvalidSnapshot("Expected JsonString, got ${json::class.simpleName}"))
                }
            }
            kClass == Int::class -> {
                when (json) {
                    is JsonNumber -> ParseResult.Success(json.toInt() as T)
                    else -> ParseResult.Failure(ParseError.InvalidSnapshot("Expected JsonNumber, got ${json::class.simpleName}"))
                }
            }
            kClass == Double::class -> {
                when (json) {
                    is JsonNumber -> ParseResult.Success(json.toDouble() as T)
                    else -> ParseResult.Failure(ParseError.InvalidSnapshot("Expected JsonNumber, got ${json::class.simpleName}"))
                }
            }
            kClass.java.isEnum -> {
                when (json) {
                    is JsonString -> {
                        @Suppress("UNCHECKED_CAST")
                        val enumClass = kClass.java as Class<out Enum<*>>
                        val enumValue = enumClass.enumConstants.find { it.name == json.value }
                        if (enumValue != null) {
                            ParseResult.Success(enumValue as T)
                        } else {
                            ParseResult.Failure(
                                ParseError.InvalidSnapshot(
                                    "Unknown enum constant '${json.value}' for ${kClass.simpleName}"
                                )
                            )
                        }
                    }
                    else -> ParseResult.Failure(ParseError.InvalidSnapshot("Expected JsonString for enum, got ${json::class.simpleName}"))
                }
            }
            else -> {
                // Custom type - look up registered serializer
                val serializer = get(kClass)
                    ?: return ParseResult.Failure(
                        ParseError.InvalidSnapshot(
                            "No serializer registered for type: ${kClass.qualifiedName}. " +
                                "Register with SerializerRegistry.register(${kClass.simpleName}::class, serializer)"
                        )
                    )
                serializer.decode(json)
            }
        }
    }

    /**
     * Decodes JSON to a value using the class name.
     *
     * Used during deserialization when we only have the class name string.
     * First attempts to load the class, then looks up its serializer.
     *
     * @param className Fully qualified class name
     * @param json JSON to decode
     * @return ParseResult containing either the decoded value or a structured error
     */
    internal fun decodeByClassName(className: String, json: JsonValue): ParseResult<Any> {
        // Try to load the class
        val kClass = try {
            Class.forName(className).kotlin
        } catch (e: ClassNotFoundException) {
            return ParseResult.Failure(
                ParseError.InvalidSnapshot("Failed to load class '$className': ${e.message}")
            )
        }

        // Handle enums specially
        if (kClass.java.isEnum) {
            return when (json) {
                is JsonString -> {
                    @Suppress("UNCHECKED_CAST")
                    val enumClass = kClass.java as Class<out Enum<*>>
                    val enumValue = enumClass.enumConstants.find { it.name == json.value }
                    if (enumValue != null) {
                        ParseResult.Success(enumValue)
                    } else {
                        ParseResult.Failure(
                            ParseError.InvalidSnapshot("Unknown enum constant '${json.value}' for $className")
                        )
                    }
                }
                else -> ParseResult.Failure(ParseError.InvalidSnapshot("Expected JsonString for enum, got ${json::class.simpleName}"))
            }
        }

        // Look up serializer
        val serializer = getByClassName(className)
            ?: return ParseResult.Failure(
                ParseError.InvalidSnapshot(
                    "No serializer registered for type: $className. " +
                        "Register with SerializerRegistry.register(${kClass.simpleName}::class, serializer)"
                )
            )

        return serializer.decode(json)
    }

    /**
     * Checks if a type has a built-in serializer.
     *
     * Built-in types don't need registration.
     */
    private fun isBuiltInType(kClass: KClass<*>): Boolean {
        return kClass == Boolean::class ||
            kClass == String::class ||
            kClass == Int::class ||
            kClass == Double::class ||
            kClass.java.isEnum
    }

    /**
     * Clears all registered serializers.
     *
     * **Testing only:** Should not be used in production code.
     * Used to reset state between test runs.
     */
    @Suppress("unused")
    internal fun clear() {
        serializers.clear()
    }

    /**
     * Returns the number of registered serializers.
     *
     * **Testing only:** Used to verify registration.
     */
    internal fun size(): Int = serializers.size
}
