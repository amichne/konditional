package io.amichne.konditional.serialization

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.core.instance.Konfig
import io.amichne.konditional.core.instance.KonfigPatch
import io.amichne.konditional.serialization.adapters.FlagValueAdapter
import io.amichne.konditional.serialization.adapters.VersionRangeAdapter
import io.amichne.konditional.serialization.models.SerializablePatch
import io.amichne.konditional.serialization.models.SerializableSnapshot

/**
 * Main serialization interface for Konfig configurations.
 * Provides methods to serialize/deserialize snapshots to/from JSON, and apply patch updates.
 *
 * Now returns ParseResult for all deserialization operations, following parse-don't-validate principles.
 */
class SnapshotSerializer(
    moshi: Moshi = defaultMoshi()
) {
    private val snapshotAdapter = moshi.adapter(SerializableSnapshot::class.java).indent("  ")
    private val patchAdapter = moshi.adapter(SerializablePatch::class.java).indent("  ")

    /**
     * Serializes a SingletonFlagRegistry.Konfig to a JSON string.
     *
     * @param konfig The konfig to serialize
     * @return JSON string representation
     */
    fun serialize(konfig: Konfig): String {
        val serializable = konfig.toSerializable()
        return snapshotAdapter.toJson(serializable)
    }

    /**
     * Deserializes a JSON string to a Konfig.
     *
     * Returns ParseResult for type-safe error handling following parse-don't-validate principles.
     *
     * @param json The JSON string to deserialize
     * @return ParseResult containing either the deserialized Konfig or a structured error
     */
    fun deserialize(json: String): ParseResult<Konfig> {
        return try {
            val serializable = snapshotAdapter.fromJson(json)
                ?: return ParseResult.Failure(ParseError.InvalidJson("Failed to parse JSON: null result"))
            serializable.toSnapshot()
        } catch (e: Exception) {
            ParseResult.Failure(ParseError.InvalidJson(e.message ?: "Unknown JSON parsing error"))
        }
    }

    /**
     * Deserializes a JSON string to a SerializablePatch.
     *
     * @param json The JSON string to deserialize
     * @return ParseResult containing either the deserialized patch or an error
     */
    fun deserializePatch(json: String): ParseResult<SerializablePatch> {
        return try {
            val patch = patchAdapter.fromJson(json)
                ?: return ParseResult.Failure(ParseError.InvalidJson("Failed to parse patch JSON: null result"))
            ParseResult.Success(patch)
        } catch (e: Exception) {
            ParseResult.Failure(ParseError.InvalidJson(e.message ?: "Unknown JSON parsing error"))
        }
    }

    /**
     * Applies a patch to an existing snapshot, creating a new snapshot with the updates.
     *
     * @param currentKonfig The current snapshot to patch
     * @param patch The patch to apply
     * @return ParseResult containing either the new Konfig with the patch applied or an error
     */
    fun applyPatch(currentKonfig: Konfig, patch: SerializablePatch): ParseResult<Konfig> {
        return try {
            // Convert current snapshot to serializable form
            val currentSerializable = currentKonfig.toSerializable()

            // Create a mutable map of flags by key
            val flagMap = currentSerializable.flags.associateBy { it.key }.toMutableMap()

            // Remove flags marked for removal
            patch.removeKeys.forEach { key ->
                flagMap.remove(key)
            }

            // Add or update flags from the patch
            patch.flags.forEach { patchFlag ->
                flagMap[patchFlag.key] = patchFlag
            }

            // Convert back to snapshot
            val patchedSerializable = SerializableSnapshot(flagMap.values.toList())
            patchedSerializable.toSnapshot()
        } catch (e: Exception) {
            ParseResult.Failure(ParseError.InvalidSnapshot("Failed to apply patch: ${e.message}"))
        }
    }

    /**
     * Applies a patch from a JSON string to an existing snapshot.
     *
     * @param currentKonfig The current snapshot to patch
     * @param patchJson The JSON string containing the patch
     * @return ParseResult containing either the new Konfig with the patch applied or an error
     */
    fun applyPatchJson(currentKonfig: Konfig, patchJson: String): ParseResult<Konfig> {
        return when (val patchResult = deserializePatch(patchJson)) {
            is ParseResult.Success -> applyPatch(currentKonfig, patchResult.value)
            is ParseResult.Failure -> ParseResult.Failure(patchResult.error)
        }
    }

    /**
     * Serializes a core KonfigPatch to a JSON string.
     *
     * @param patch The KonfigPatch to serialize
     * @return JSON string representation of the patch
     */
    fun serializePatch(patch: KonfigPatch): String {
        val serializable = patch.toSerializable()
        return patchAdapter.toJson(serializable)
    }

    /**
     * Deserializes a JSON string to a core KonfigPatch.
     *
     * @param json The JSON string to deserialize
     * @return ParseResult containing either the deserialized KonfigPatch or an error
     */
    fun deserializePatchToCore(json: String): ParseResult<KonfigPatch> {
        return when (val serializableResult = deserializePatch(json)) {
            is ParseResult.Success -> serializableResult.value.toPatch()
            is ParseResult.Failure -> ParseResult.Failure(serializableResult.error)
        }
    }

    companion object {
        /**
         * Creates the default Moshi instance with all necessary adapters.
         * Registers custom adapters for domain types like VersionRange and FlagValue.
         *
         * Note: Custom adapters (FlagValueAdapter.FACTORY, VersionRangeAdapter) must be added before
         * KotlinJsonAdapterFactory to take precedence over reflection-based serialization.
         */
        fun defaultMoshi(): Moshi {
            // Build Moshi with custom adapters registered BEFORE KotlinJsonAdapterFactory
            // This ensures our custom adapters take precedence over reflection-based serialization
            return Moshi.Builder()
                .add(FlagValueAdapter.FACTORY)
                .add(VersionRangeAdapter(
                    // Create a minimal Moshi for VersionRangeAdapter to use for Version
                    Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                ))
                .add(KotlinJsonAdapterFactory())
                .build()
        }

        /**
         * Default singleton instance for convenience.
         */
        val default = SnapshotSerializer()
    }
}
