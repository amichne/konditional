package io.amichne.konditional.serialization

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.amichne.konditional.core.snapshot.Snapshot
import io.amichne.konditional.core.snapshot.SnapshotPatch
import io.amichne.konditional.serialization.models.SerializablePatch
import io.amichne.konditional.serialization.models.SerializableSnapshot

/**
 * Main serialization interface for SingletonFlagRegistry.Snapshot configurations.
 * Provides methods to serialize/deserialize snapshots to/from JSON, and apply patch updates.
 */
class SnapshotSerializer(
    moshi: Moshi = defaultMoshi()
) {
    private val snapshotAdapter = moshi.adapter(SerializableSnapshot::class.java).indent("  ")
    private val patchAdapter = moshi.adapter(SerializablePatch::class.java).indent("  ")

    /**
     * Serializes a SingletonFlagRegistry.Snapshot to a JSON string.
     *
     * @param snapshot The snapshot to serialize
     * @return JSON string representation
     */
    fun serialize(snapshot: Snapshot): String {
        val serializable = snapshot.toSerializable()
        return snapshotAdapter.toJson(serializable)
    }

    /**
     * Deserializes a JSON string to a SingletonFlagRegistry.Snapshot.
     *
     * @param json The JSON string to deserialize
     * @return The deserialized SingletonFlagRegistry.Snapshot
     * @throws IllegalArgumentException if JSON is invalid or references unregistered flags
     */
    fun deserialize(json: String): Snapshot {
        val serializable = snapshotAdapter.fromJson(json)
            ?: throw IllegalArgumentException("Failed to parse JSON: null result")
        return serializable.toSnapshot()
    }

    /**
     * Deserializes a JSON string to a SerializablePatch.
     *
     * @param json The JSON string to deserialize
     * @return The deserialized patch
     */
    fun deserializePatch(json: String): SerializablePatch {
        return patchAdapter.fromJson(json)
            ?: throw IllegalArgumentException("Failed to parse patch JSON: null result")
    }

    /**
     * Applies a patch to an existing snapshot, creating a new snapshot with the updates.
     *
     * @param currentSnapshot The current snapshot to patch
     * @param patch The patch to apply
     * @return A new SingletonFlagRegistry.Snapshot with the patch applied
     */
    fun applyPatch(currentSnapshot: Snapshot, patch: SerializablePatch): Snapshot {
        // Convert current snapshot to serializable form
        val currentSerializable = currentSnapshot.toSerializable()

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
        return patchedSerializable.toSnapshot()
    }

    /**
     * Applies a patch from a JSON string to an existing snapshot.
     *
     * @param currentSnapshot The current snapshot to patch
     * @param patchJson The JSON string containing the patch
     * @return A new SingletonFlagRegistry.Snapshot with the patch applied
     */
    fun applyPatchJson(currentSnapshot: Snapshot, patchJson: String): Snapshot {
        val patch = deserializePatch(patchJson)
        return applyPatch(currentSnapshot, patch)
    }

    /**
     * Serializes a core SnapshotPatch to a JSON string.
     *
     * @param patch The SnapshotPatch to serialize
     * @return JSON string representation of the patch
     */
    fun serializePatch(patch: SnapshotPatch): String {
        val serializable = patch.toSerializable()
        return patchAdapter.toJson(serializable)
    }

    /**
     * Deserializes a JSON string to a core SnapshotPatch.
     *
     * @param json The JSON string to deserialize
     * @return The deserialized SnapshotPatch
     * @throws IllegalArgumentException if JSON is invalid or references unregistered flags
     */
    fun deserializePatchToCore(json: String): SnapshotPatch {
        val serializable = deserializePatch(json)
        return serializable.toPatch()
    }

    companion object {
        /**
         * Creates the default Moshi instance with all necessary adapters.
         */
        fun defaultMoshi(): Moshi {
            return Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
        }

        /**
         * Default singleton instance for convenience.
         */
        val default = SnapshotSerializer()
    }
}
