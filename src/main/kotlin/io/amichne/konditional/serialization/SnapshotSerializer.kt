package io.amichne.konditional.serialization

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.amichne.konditional.core.instance.Konfig
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.internal.serialization.adapters.FlagValueAdapter
import io.amichne.konditional.internal.serialization.adapters.VersionRangeAdapter
import io.amichne.konditional.internal.serialization.models.SerializablePatch
import io.amichne.konditional.internal.serialization.models.SerializableSnapshot
import io.amichne.konditional.rules.versions.FullyBound
import io.amichne.konditional.rules.versions.LeftBound
import io.amichne.konditional.rules.versions.RightBound
import io.amichne.konditional.rules.versions.Unbounded
import io.amichne.konditional.rules.versions.VersionRange

/**
 * Main serialization object for Konfig configurations.
 * Provides methods to serialize/deserialize snapshots to/from JSON, and apply patch updates.
 *
 * Returns ParseResult for all deserialization operations, following parse-don't-validate principles.
 *
 * This serializer is storage-agnostic - it only handles JSON conversion, allowing callers
 * to choose their storage solution (files, databases, cloud storage, etc.).
 *
 * For taxonomy-scoped serialization, use [TaxonomySnapshotSerializer] instead.
 *
 * ## Usage
 *
 * ```kotlin
 * // Serialize
 * val json = SnapshotSerializer.serialize(konfig)
 *
 * // Deserialize
 * when (val result = SnapshotSerializer.fromJson(json)) {
 *     is ParseResult.Success -> println("Loaded: ${result.value}")
 *     is ParseResult.Failure -> println("Error: ${result.error}")
 * }
 * ```
 */
object SnapshotSerializer {
    private val moshi = defaultMoshi()
    private val snapshotAdapter = moshi.adapter(SerializableSnapshot::class.java).indent("  ")
    private val patchAdapter = moshi.adapter(SerializablePatch::class.java).indent("  ")

    /**
     * Serializes a Konfig to a JSON string.
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
     * Note: This does NOT automatically load the configuration into any registry.
     * Callers must explicitly load the result if desired:
     *
     * ```kotlin
     * when (val result = SnapshotSerializer.fromJson(json)) {
     *     is ParseResult.Success -> taxonomy.load(result.value)
     *     is ParseResult.Failure -> handleError(result.error)
     * }
     * ```
     *
     * @param json The JSON string to deserialize
     * @return ParseResult containing either the deserialized Konfig or a structured error
     */
    fun fromJson(json: String): ParseResult<Konfig> {
        return try {
            val serializable = snapshotAdapter.fromJson(json)
                ?: return ParseResult.Failure(ParseError.InvalidJson("Failed to parse JSON: null result"))
            serializable.toSnapshot()
        } catch (e: Exception) {
            ParseResult.Failure(ParseError.InvalidJson(e.message ?: "Unknown JSON parsing error"))
        }
    }

    /**
     * Serializes a KonfigPatch to a JSON string.
     *
     * @param patch The patch to serialize
     * @return JSON string representation
     */
    internal fun serializePatch(patch: SerializablePatch): String {
        return patchAdapter.toJson(patch)
    }

    /**
     * Deserializes a JSON string to a SerializablePatch.
     *
     * @param json The JSON string to deserialize
     * @return ParseResult containing either the deserialized patch or an error
     */
    internal fun fromJsonPatch(json: String): ParseResult<SerializablePatch> {
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
    internal fun applyPatch(currentKonfig: Konfig, patch: SerializablePatch): ParseResult<Konfig> {
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
        return when (val patchResult = fromJsonPatch(patchJson)) {
            is ParseResult.Success -> applyPatch(currentKonfig, patchResult.value)
            is ParseResult.Failure -> ParseResult.Failure(patchResult.error)
        }
    }

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
            .add(
                VersionRangeAdapter(
                    // Create a minimal Moshi for VersionRangeAdapter to use for Version
                    Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                )
            )
            .add(
                PolymorphicJsonAdapterFactory.of(VersionRange::class.java, "type")
                    .withSubtype(FullyBound::class.java, VersionRange.Type.MIN_AND_MAX_BOUND.name)
                    .withSubtype(Unbounded::class.java, VersionRange.Type.UNBOUNDED.name)
                    .withSubtype(LeftBound::class.java, VersionRange.Type.MIN_BOUND.name)
                    .withSubtype(RightBound::class.java, VersionRange.Type.MAX_BOUND.name)
            )
            .add(KotlinJsonAdapterFactory())
            .build()
    }
}
