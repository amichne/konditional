package io.amichne.konditional.serialization.snapshot

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.core.instance.ConfigurationMetadataView
import io.amichne.konditional.core.instance.ConfigurationView
import io.amichne.konditional.internal.serialization.adapters.FlagValueAdapterFactory
import io.amichne.konditional.internal.serialization.adapters.IdentifierJsonAdapter
import io.amichne.konditional.internal.serialization.adapters.ValueClassAdapterFactory
import io.amichne.konditional.internal.serialization.adapters.VersionRangeAdapter
import io.amichne.konditional.internal.serialization.models.SerializablePatch
import io.amichne.konditional.internal.serialization.models.SerializableSnapshot
import io.amichne.konditional.rules.versions.FullyBound
import io.amichne.konditional.rules.versions.LeftBound
import io.amichne.konditional.rules.versions.RightBound
import io.amichne.konditional.rules.versions.Unbounded
import io.amichne.konditional.rules.versions.VersionRange
import io.amichne.konditional.serialization.options.SnapshotLoadOptions
import io.amichne.konditional.serialization.instance.Configuration
import io.amichne.konditional.serialization.instance.ConfigurationMetadata

/**
 * Configuration snapshot JSON codec.
 *
 * Provides pure encoding/decoding for full configuration snapshots and patch application.
 *
 * Returns ParseResult for all deserialization operations, following parseUnsafe-don't-validate principles.
 *
 * This codec is storage-agnostic - it only handles JSON conversion, allowing callers
 * to choose their storage solution (files, databases, cloud storage, etc.).
 *
 * For side-effecting loading into a namespace, use [NamespaceSnapshotLoader].
 *
 * ## Usage
 *
 * ```kotlin
 * // Encode
 * val json = ConfigurationSnapshotCodec.encode(configuration)
 *
 * // Decode
 * when (val result = ConfigurationSnapshotCodec.decode(json)) {
 *     is ParseResult.Success -> println("Loaded: ${result.value}")
 *     is ParseResult.Failure -> println("Error: ${result.error}")
 * }
 * ```
 */
object ConfigurationSnapshotCodec : SnapshotCodec<Configuration> {
    private val moshi = defaultMoshi()
    private val snapshotAdapter = moshi.adapter(SerializableSnapshot::class.java).indent("  ")
    private val patchAdapter = moshi.adapter(SerializablePatch::class.java).indent("  ")

    override fun encode(value: Configuration): String = snapshotAdapter.toJson(SerializableSnapshot.from(value))

    /**
     * Encodes a [ConfigurationView] by converting it to the concrete serialization model.
     *
     * This keeps consumer ergonomics intact when working with [io.amichne.konditional.core.Namespace.configuration],
     * which is exposed as a read-only view in `:konditional-core`.
     */
    fun encode(value: ConfigurationView): String = encode(value.toConcrete())

    /**
     * Deserializes a JSON string to a Configuration.
     *
     * Returns ParseResult for type-safe error handling following parseUnsafe-don't-validate principles.
     *
     * Note: This does NOT automatically load the configuration into any registry.
     * Callers must explicitly load the result if desired:
     *
     * ```kotlin
     * when (val result = ConfigurationSnapshotCodec.decode(json)) {
     *     is ParseResult.Success -> namespace.load(result.value)
     *     is ParseResult.Failure -> handleError(result.error)
     * }
     * ```
     *
     * @param json The JSON string to deserialize
     * @return ParseResult containing either the deserialized Configuration or a structured error
     */
    override fun decode(
        json: String,
        options: SnapshotLoadOptions,
    ): ParseResult<Configuration> =
        runCatching {
            snapshotAdapter.fromJson(json)?.toConfiguration(options)
                ?: ParseResult.failure(ParseError.invalidJson("Failed to parseUnsafe JSON: null result"))
        }.getOrElse { e ->
            ParseResult.failure(ParseError.invalidJson(e.message ?: "Unknown JSON parsing error"))
        }

    /**
     * Applies a patch from a JSON string to an existing snapshot.
     *
     * This is a pure operation: it returns the patched [Configuration] without loading it into a namespace.
     */
    fun applyPatchJson(
        currentConfiguration: ConfigurationView,
        patchJson: String,
        options: SnapshotLoadOptions = SnapshotLoadOptions.strict(),
    ): ParseResult<Configuration> =
        runCatching {
            val patch =
                patchAdapter.fromJson(patchJson)
                    ?: return@runCatching ParseResult.failure(
                        ParseError.invalidJson("Failed to parse patch JSON: null result"),
                    )

            val currentSerializable = SerializableSnapshot.from(currentConfiguration.toConcrete())
            val flagMap = currentSerializable.flags.associateBy { it.key }.toMutableMap()

            patch.removeKeys.forEach(flagMap::remove)
            patch.flags.forEach { patchFlag -> flagMap[patchFlag.key] = patchFlag }

            SerializableSnapshot(
                meta = patch.meta ?: currentSerializable.meta,
                flags = flagMap.values.toList(),
            ).toConfiguration(options)
        }.getOrElse { e ->
            ParseResult.failure(ParseError.invalidSnapshot(e.message ?: "Failed to apply patch"))
        }

    /**
     * Creates the default Moshi instance with all necessary adapters.
     * Registers custom adapters for domain types like VersionRange and FlagValue.
     *
     * Note: Custom adapters (FlagValueAdapter.Factory, VersionRangeAdapter) must be added before
     * KotlinJsonAdapterFactory to take precedence over reflection-based serialization.
     */
    internal fun defaultMoshi(): Moshi {
        // Build Moshi with custom adapters registered before KotlinJsonAdapterFactory so they take precedence.
        return Moshi.Builder()
            .add(IdentifierJsonAdapter)
            .add(ValueClassAdapterFactory)
            .add(FlagValueAdapterFactory)
            .add(
                VersionRangeAdapter(
                    // Create a minimal Moshi for VersionRangeAdapter to use for Version
                    Moshi.Builder().add(KotlinJsonAdapterFactory()).build(),
                ),
            ).add(
                PolymorphicJsonAdapterFactory
                    .of(VersionRange::class.java, "type")
                    .withSubtype(FullyBound::class.java, VersionRange.Type.MIN_AND_MAX_BOUND.name)
                    .withSubtype(Unbounded::class.java, VersionRange.Type.UNBOUNDED.name)
                    .withSubtype(LeftBound::class.java, VersionRange.Type.MIN_BOUND.name)
                    .withSubtype(RightBound::class.java, VersionRange.Type.MAX_BOUND.name),
            ).addLast(KotlinJsonAdapterFactory())
            .build()
    }
}

private fun ConfigurationView.toConcrete(): Configuration =
    (this as? Configuration)
        ?: Configuration(
            flags = flags,
            metadata = metadata.toConcrete(),
        )

private fun ConfigurationMetadataView.toConcrete(): ConfigurationMetadata =
    (this as? ConfigurationMetadata)
        ?: ConfigurationMetadata(
            version = version,
            generatedAtEpochMillis = generatedAtEpochMillis,
            source = source,
        )
