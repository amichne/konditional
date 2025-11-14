package io.amichne.konditional.serialization

import io.amichne.konditional.core.result.ParseResult

/**
 * Storage-agnostic serialization interface for feature flag configurations.
 *
 * This interface provides a common contract for serializing and deserializing
 * feature flag configurations without coupling to specific storage implementations
 * (files, databases, cloud storage, etc.).
 *
 * Follows **parse-don't-validate** principles: deserialization returns [ParseResult]
 * instead of throwing exceptions, enabling type-safe error handling.
 *
 * ## Implementations
 *
 * - [SnapshotSerializer]: Serializes Konfig configurations
 * - [ModuleSnapshotSerializer]: Serializes a single taxonomy's configuration
 *
 * ## Usage Example
 *
 * ```kotlin
 * val serializer: Serializer<Konfig> = ModuleSnapshotSerializer(Taxonomy.Domain.Payments)
 *
 * // Serialize to JSON
 * val json = serializer.toJson()
 *
 * // Persist using your storage solution
 * File("configs/payments.json").writeText(json)
 *
 * // Load from your storage solution
 * val loadedJson = File("configs/payments.json").readText()
 *
 * // Deserialize from JSON (returns ParseResult)
 * when (val result = serializer.fromJson(loadedJson)) {
 *     is ParseResult.Success -> println("Loaded: ${result.value}")
 *     is ParseResult.Failure -> println("Error: ${result.error}")
 * }
 * ```
 *
 * ## Design Philosophy
 *
 * By keeping serialization separate from storage, this interface enables:
 * - **Flexibility**: Use any storage backend (files, S3, database, etc.)
 * - **Testability**: Easy to test serialization logic without I/O
 * - **Portability**: Same serialization logic works across environments
 * - **Separation of concerns**: Serialization logic doesn't know about storage
 * - **Type safety**: ParseResult enables compile-time-checked error handling
 *
 * @param T The type being serialized/deserialized (e.g., [io.amichne.konditional.core.instance.Konfig])
 */
interface Serializer<T> {
    /**
     * Serializes the current state to JSON format.
     *
     * The returned JSON string is storage-agnostic - callers decide where
     * and how to persist it (files, databases, cloud storage, etc.).
     *
     * @return JSON representation of the configuration
     */
    fun toJson(): String

    /**
     * Deserializes JSON into the target type.
     *
     * Returns [ParseResult] for type-safe error handling following parse-don't-validate principles.
     * Never throws exceptions - all errors are captured in [ParseResult.Failure].
     *
     * Callers are responsible for loading the JSON from their storage
     * solution (files, databases, cloud storage, etc.).
     *
     * @param json JSON string to deserialize
     * @return ParseResult containing either the deserialized object or a structured error
     */
    fun fromJson(json: String): ParseResult<T>
}
