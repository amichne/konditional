package io.amichne.konditional.serialization

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.amichne.konditional.core.Flags

/**
 * Main serialization interface for Flags.Snapshot configurations.
 * Provides methods to serialize/deserialize snapshots to/from JSON, and apply patch updates.
 */
class SnapshotSerializer(
    private val moshi: Moshi = defaultMoshi()
) {
    private val snapshotAdapter = moshi.adapter(SerializableSnapshot::class.java).indent("  ")
    private val patchAdapter = moshi.adapter(SerializablePatch::class.java).indent("  ")

    /**
     * Serializes a Flags.Snapshot to a JSON string.
     *
     * @param snapshot The snapshot to serialize
     * @return JSON string representation
     */
    fun serialize(snapshot: Flags.Snapshot): String {
        val serializable = snapshot.toSerializable()
        return snapshotAdapter.toJson(serializable)
    }

    /**
     * Deserializes a JSON string to a Flags.Snapshot.
     *
     * @param json The JSON string to deserialize
     * @return The deserialized Flags.Snapshot
     * @throws IllegalArgumentException if JSON is invalid or references unregistered flags
     */
    fun deserialize(json: String): Flags.Snapshot {
        val serializable = snapshotAdapter.fromJson(json)
            ?: throw IllegalArgumentException("Failed to parse JSON: null result")
        return serializable.toSnapshot()
    }

    /**
     * Serializes a patch update to a JSON string.
     *
     * @param patch The patch to serialize
     * @return JSON string representation
     */
    fun serializePatch(patch: SerializablePatch): String {
        return patchAdapter.toJson(patch)
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
     * @return A new Flags.Snapshot with the patch applied
     */
    fun applyPatch(currentSnapshot: Flags.Snapshot, patch: SerializablePatch): Flags.Snapshot {
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
     * @return A new Flags.Snapshot with the patch applied
     */
    fun applyPatchJson(currentSnapshot: Flags.Snapshot, patchJson: String): Flags.Snapshot {
        val patch = deserializePatch(patchJson)
        return applyPatch(currentSnapshot, patch)
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

/**
 * Extension function to serialize a Flags.Snapshot directly.
 */
fun Flags.Snapshot.toJson(serializer: SnapshotSerializer = SnapshotSerializer.default): String {
    return serializer.serialize(this)
}

/**
 * Companion object extension to deserialize JSON to a Flags.Snapshot.
 */
object SnapshotJsonParser {
    fun fromJson(json: String, serializer: SnapshotSerializer = SnapshotSerializer.default): Flags.Snapshot {
        return serializer.deserialize(json)
    }
}
