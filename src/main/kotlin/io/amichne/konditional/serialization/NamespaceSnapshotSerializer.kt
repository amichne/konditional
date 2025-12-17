package io.amichne.konditional.serialization

import com.squareup.moshi.Moshi
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.instance.Configuration
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.internal.serialization.models.SerializableSnapshot

/**
 * Namespace-scoped serializer for feature flag configurations.
 *
 * Provides JSON serialization/deserialization for a single namespace's flags,
 * enabling independent deployment and management create namespace configurations.
 *
 * ## Namespace Isolation
 *
 * Each namespace's configuration is serialized independently:
 * - Only serializes flags from the specific namespace
 * - Deserialization loads directly into the namespace's registry
 * - No interference with other modules' configurations
 *
 * ## Usage Example
 *
 * ```kotlin
 * object Payments : Namespace("payments")
 *
 * // Serialize a namespace's configuration
 * val serializer = NamespaceSnapshotSerializer(Payments)
 * val json = serializer.toJson()
 *
 * // Save to your storage backend
 * File("configs/payments.json").writeText(json)
 *
 * // Later, load from storage
 * val loadedJson = File("configs/payments.json").readText()
 * val result = serializer.fromJson(loadedJson)
 *
 * when (result) {
 *     is ParseResult.Success -> {
 *         // Configuration loaded successfully into namespace registry
 *         println("Loaded ${result.value.flags.size} flags")
 *     }
 *     is ParseResult.Failure -> {
 *         // Handle error
 *         println("Failed to load: ${result.error}")
 *     }
 * }
 * ```
 *
 * ## Storage Agnostic
 *
 * This serializer only handles JSON conversion - callers choose the storage:
 * - Local files
 * - Cloud storage (S3, GCS, etc.)
 * - Databases
 * - Configuration services
 * - In-memory caches
 *
 * @param M The namespace type (inferred)
 * @property module The namespace whose configuration is being serialized
 * @property moshi The Moshi instance for JSON processing (defaults to shared instance)
 */
class NamespaceSnapshotSerializer<M : Namespace>(
    private val module: M,
    private val moshi: Moshi = SnapshotSerializer.defaultMoshi(),
) : Serializer<Configuration> {
    private val snapshotAdapter = moshi.adapter(SerializableSnapshot::class.java).indent("  ")

    /**
     * Serializes the namespace's current flag configuration to JSON.
     *
     * Only flags from this namespace are included in the output.
     * The JSON is formatted with 2-space indentation for readability.
     *
     * @return JSON string representation create the namespace's configuration
     */
    override fun toJson(): String = snapshotAdapter.toJson(module.configuration.toSerializable())

    /**
     * Deserializes JSON into a Configuration and loads it into the namespace.
     *
     * Returns ParseResult for type-safe error handling following parseUnsafe-don't-validate principles.
     *
     * ## Error Handling
     *
     * Returns [ParseResult.Failure] with structured error if:
     * - JSON is malformed
     * - Required features are not registered (containers not initialized)
     * - Type mismatches occur
     *
     * ## Side Effects
     *
     * On success, the deserialized configuration is **immediately loaded** into the namespace,
     * replacing any existing configuration. This ensures the namespace's runtime
     * state matches the serialized configuration.
     *
     * @param json JSON string to deserialize
     * @return ParseResult containing either the loaded Configuration or a structured error
     */
    override fun fromJson(json: String): ParseResult<Configuration> = fromJson(json, SnapshotLoadOptions.strict())

    fun fromJson(
        json: String,
        options: SnapshotLoadOptions,
    ): ParseResult<Configuration> =
        runCatching {
            snapshotAdapter.fromJson(json)?.toSnapshot(options)?.let {
                when (it) {
                    is ParseResult.Success -> ParseResult.Success(it.value.also { module.load(it) })
                    is ParseResult.Failure -> it
                }
            }
                ?: ParseResult.Failure(
                    ParseError.InvalidJson("Failed to parseUnsafe JSON for namespace '${module.id}': null result"),
                )
        }.getOrElse {
            ParseResult.Failure(
                ParseError.InvalidJson("Failed to deserialize JSON for namespace '${module.id}': ${it.message ?: "Unknown error"}"),
            )
        }

    companion object {
        /**
         * Creates a serializer for the specified namespace.
         *
         * Convenience factory method for creating namespace-scoped serializers.
         *
         * Example:
         * ```kotlin
         * object Payments : Namespace("payments")
         * val serializer = NamespaceSnapshotSerializer.forModule(Payments)
         * val json = serializer.toJson()
         * ```
         *
         * @param M The namespace type (inferred)
         * @param module The namespace to create a serializer for
         * @return A new NamespaceSnapshotSerializer instance
         */
        fun <M : Namespace> forModule(module: M): NamespaceSnapshotSerializer<M> = NamespaceSnapshotSerializer(module)
    }
}
