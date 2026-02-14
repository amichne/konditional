@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.serialization.snapshot

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.core.instance.ConfigurationMetadataView
import io.amichne.konditional.core.instance.ConfigurationView
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.parseFailure
import io.amichne.konditional.core.schema.CompiledNamespaceSchema
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
import io.amichne.konditional.serialization.instance.Configuration
import io.amichne.konditional.serialization.instance.ConfigurationMetadata
import io.amichne.konditional.serialization.instance.MaterializedConfiguration
import io.amichne.konditional.serialization.options.SnapshotLoadOptions

/**
 * Schema-aware configuration snapshot JSON codec.
 *
 * Decoding is pure and returns Kotlin [Result]. Successful decodes always return a
 * [MaterializedConfiguration] that has been validated against the compile-time schema plane.
 */
object ConfigurationSnapshotCodec : FeatureAwareSnapshotCodec<MaterializedConfiguration> {
    private val moshi = defaultMoshi()
    private val snapshotAdapter = moshi.adapter(SerializableSnapshot::class.java).indent("  ")
    private val patchAdapter = moshi.adapter(SerializablePatch::class.java).indent("  ")

    /**
     * Encodes raw [Configuration] snapshots.
     */
    fun encodeRaw(value: Configuration): String = snapshotAdapter.toJson(SerializableSnapshot.from(value))

    /**
     * Encodes a trusted [MaterializedConfiguration].
     */
    override fun encode(value: MaterializedConfiguration): String = encodeRaw(value.configuration)

    /**
     * Encodes a [ConfigurationView] by converting it to the concrete serialization model.
     */
    fun encode(value: ConfigurationView): String = encodeRaw(value.toConcrete())

    /**
     * Decode without schema is unsupported by design.
     */
    override fun decode(
        json: String,
        options: SnapshotLoadOptions,
    ): Result<MaterializedConfiguration> =
        parseFailure(
            ParseError.invalidSnapshot(
                "Decoding requires a compile-time schema. Use decode(json, schema, options).",
            ),
        )

    override fun decode(
        json: String,
        schema: CompiledNamespaceSchema,
        options: SnapshotLoadOptions,
    ): Result<MaterializedConfiguration> {
        val serializable =
            runCatching { snapshotAdapter.fromJson(json) }
                .getOrElse { error ->
                    return parseFailure(
                        ParseError.invalidJson(error.message ?: "Unknown JSON parsing error"),
                    )
                }

        return serializable?.toConfiguration(schema = schema, options = options)
            ?: parseFailure(ParseError.invalidJson("Failed to parse JSON: null snapshot"))
    }

    /**
     * Applies a patch from JSON to an existing trusted snapshot.
     */
    fun applyPatchJson(
        currentConfiguration: MaterializedConfiguration,
        patchJson: String,
        options: SnapshotLoadOptions = SnapshotLoadOptions.strict(),
    ): Result<MaterializedConfiguration> =
        applyPatchJson(
            currentConfiguration = currentConfiguration.configuration,
            schema = currentConfiguration.schema,
            patchJson = patchJson,
            options = options,
        )

    /**
     * Applies a patch from JSON to an existing snapshot with explicit schema scope.
     */
    fun applyPatchJson(
        currentConfiguration: ConfigurationView,
        schema: CompiledNamespaceSchema,
        patchJson: String,
        options: SnapshotLoadOptions = SnapshotLoadOptions.strict(),
    ): Result<MaterializedConfiguration> =
        runCatching { patchAdapter.fromJson(patchJson) }
            .fold(
                onSuccess = { parsedPatch ->
                    parsedPatch
                        ?.let { patch ->
                            val currentSerializable = SerializableSnapshot.from(currentConfiguration.toConcrete())
                            val flagMap = currentSerializable.flags.associateBy { it.key }.toMutableMap()

                            patch.removeKeys.forEach(flagMap::remove)
                            patch.flags.forEach { patchFlag -> flagMap[patchFlag.key] = patchFlag }

                            SerializableSnapshot(
                                meta = patch.meta ?: currentSerializable.meta,
                                flags = flagMap.values.toList(),
                            ).toConfiguration(
                                schema = schema,
                                options = options,
                            )
                        }
                        ?: parseFailure(ParseError.invalidJson("Failed to parse patch JSON: null result"))
                },
                onFailure = { error ->
                    parseFailure(
                        ParseError.invalidJson(error.message ?: "Unknown patch JSON parsing error"),
                    )
                },
            )

    /**
     * Creates the default Moshi instance with all necessary adapters.
     *
     * Custom adapters must be added before KotlinJsonAdapterFactory to take precedence.
     */
    internal fun defaultMoshi(): Moshi =
        Moshi.Builder()
            .add(IdentifierJsonAdapter)
            .add(ValueClassAdapterFactory)
            .add(FlagValueAdapterFactory)
            .add(
                VersionRangeAdapter(
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

private fun ConfigurationView.toConcrete(): Configuration =
    (this as? Configuration)
        ?: Configuration(
            flags = flags.toMap(),
            metadata = metadata.toConcrete(),
        )

private fun ConfigurationMetadataView.toConcrete(): ConfigurationMetadata =
    (this as? ConfigurationMetadata)
        ?: ConfigurationMetadata(
            version = version,
            generatedAtEpochMillis = generatedAtEpochMillis,
            source = source,
        )
