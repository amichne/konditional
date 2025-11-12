package io.amichne.konditional.serialization

import com.squareup.moshi.Moshi
import io.amichne.konditional.core.FeatureModule
import io.amichne.konditional.core.instance.Konfig
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.internal.serialization.models.SerializableSnapshot

/**
 * FeatureModule-scoped serializer for feature flag configurations.
 *
 * Provides JSON serialization/deserialization for a single featureModule's flags,
 * enabling independent deployment and management of featureModule configurations.
 *
 * ## FeatureModule Isolation
 *
 * Each featureModule's configuration is serialized independently:
 * - Only serializes flags from the specific featureModule
 * - Deserialization loads directly into the featureModule's registry
 * - No interference with other modules' configurations
 *
 * ## Usage Example
 *
 * ```kotlin
 * // Serialize a featureModule's configuration
 * val serializer = ModuleSnapshotSerializer(FeatureModule.Team.Payments)
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
 *         // Configuration loaded successfully into featureModule registry
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
 * @param M The featureModule type (inferred)
 * @property module The featureModule whose configuration is being serialized
 * @property moshi The Moshi instance for JSON processing (defaults to shared instance)
 */
class ModuleSnapshotSerializer<M : FeatureModule>(
    private val module: M,
    moshi: Moshi = SnapshotSerializer.defaultMoshi()
) : Serializer<Konfig> {

    private val snapshotAdapter = moshi.adapter(SerializableSnapshot::class.java).indent("  ")

    /**
     * Serializes the featureModule's current flag configuration to JSON.
     *
     * Only flags from this featureModule's registry are included in the output.
     * The JSON is formatted with 2-space indentation for readability.
     *
     * @return JSON string representation of the featureModule's configuration
     */
    override fun toJson(): String {
        val konfig = module.registry.konfig()
        val serializable = konfig.toSerializable()
        return snapshotAdapter.toJson(serializable)
    }

    /**
     * Deserializes JSON into a Konfig and loads it into the featureModule's registry.
     *
     * Returns ParseResult for type-safe error handling following parse-don't-validate principles.
     *
     * ## Error Handling
     *
     * Returns [ParseResult.Failure] with structured error if:
     * - JSON is malformed
     * - Required features are not registered in [FeatureRegistry]
     * - Type mismatches occur
     *
     * ## Side Effects
     *
     * On success, the deserialized configuration is **immediately loaded** into the featureModule's
     * registry, replacing any existing configuration. This ensures the featureModule's runtime
     * state matches the serialized configuration.
     *
     * @param json JSON string to deserialize
     * @return ParseResult containing either the loaded Konfig or a structured error
     */
    override fun fromJson(json: String): ParseResult<Konfig> {
        return try {
            val serializable = snapshotAdapter.fromJson(json)
                ?: return ParseResult.Failure(
                    ParseError.InvalidJson("Failed to parse JSON for featureModule '${module.id}': null result")
                )

            // Parse the serializable snapshot into a Konfig
            when (val parseResult = serializable.toSnapshot()) {
                is ParseResult.Success -> {
                    val konfig = parseResult.value
                    // Load the parsed configuration into the featureModule's registry
                    module.registry.load(konfig)
                    ParseResult.Success(konfig)
                }
                is ParseResult.Failure -> parseResult
            }
        } catch (e: Exception) {
            ParseResult.Failure(
                ParseError.InvalidJson(
                    "Failed to deserialize JSON for featureModule '${module.id}': ${e.message ?: "Unknown error"}"
                )
            )
        }
    }

    companion object {
        /**
         * Creates a serializer for the specified featureModule.
         *
         * Convenience factory method for creating featureModule-scoped serializers.
         *
         * Example:
         * ```kotlin
         * val serializer = ModuleSnapshotSerializer.forModule(FeatureModule.Team.Payments)
         * val json = serializer.toJson()
         * ```
         *
         * @param M The featureModule type (inferred)
         * @param module The featureModule to create a serializer for
         * @return A new ModuleSnapshotSerializer instance
         */
        fun <M : FeatureModule> forModule(module: M): ModuleSnapshotSerializer<M> =
            ModuleSnapshotSerializer(module)
    }
}
