@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.serialization.snapshot

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.core.Namespace
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
import io.amichne.konditional.serialization.options.SnapshotLoadOptions

/**
 * Pure JSON codec for configuration snapshots.
 *
 * All operations are side-effect free: encoding does not mutate runtime state,
 * decoding does not load into any namespace registry.
 *
 * There is exactly one way to perform each operation:
 * - [encode]: accepts any [ConfigurationView] (including [Configuration])
 * - [decode]: schema is a required parameter — no schema-less overload exists
 * - [patch]: derives schema from the provided [namespace]
 *
 * To load a decoded snapshot into a namespace registry, use `NamespaceSnapshotLoader` from `:konditional-runtime`.
 */
object ConfigurationSnapshotCodec {
    private val moshi = defaultMoshi()
    private val snapshotAdapter = moshi.adapter(SerializableSnapshot::class.java).indent("  ")
    private val patchAdapter = moshi.adapter(SerializablePatch::class.java).indent("  ")

    /**
     * Encodes a [ConfigurationView] as a JSON snapshot string.
     *
     * Prefer namespace-scoped serialization via `Namespace.dump()` from `:konditional-runtime`.
     * This codec method remains available for infrastructure and tests that only hold a
     * materialized [ConfigurationView]. Accepts any subtype: [Configuration], or a view
     * obtained from a namespace.
     */
    @Deprecated(
        message = "Prefer Namespace.dump() as the serialization entry point.",
        level = DeprecationLevel.WARNING,
    )
    fun encode(value: ConfigurationView): String = snapshotAdapter.toJson(SerializableSnapshot.from(value.toConcrete()))

    /**
     * Decodes a JSON snapshot against a [CompiledNamespaceSchema].
     *
     * Returns a [Result] containing a fully validated [Configuration] on success,
     * or a typed [ParseError] on failure. Never throws.
     *
     * @param json the raw JSON snapshot string
     * @param schema the compile-time schema produced by `Namespace.compiledSchema()`
     * @param options controls unknown-key and missing-declared-flag handling
     */
    fun decode(
        json: String,
        schema: CompiledNamespaceSchema,
        options: SnapshotLoadOptions = SnapshotLoadOptions.strict(),
    ): Result<Configuration> {
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
     * Applies a JSON patch to an existing [Configuration].
     *
     * The schema is derived from [namespace]. Returns a [Result] containing the patched
     * [Configuration] on success, or a typed [ParseError] on failure. Never throws.
     *
     * @param current the current configuration snapshot to patch
     * @param patchJson the raw JSON patch string
     * @param namespace namespace used to derive a compile-time schema for patch materialization
     * @param options controls unknown-key and missing-declared-flag handling
     */
    fun patch(
        current: Configuration,
        patchJson: String,
        namespace: Namespace,
        options: SnapshotLoadOptions = SnapshotLoadOptions.strict(),
    ): Result<Configuration> =
        patchWithSchema(
            current = current,
            patchJson = patchJson,
            schema = namespace.compiledSchema(),
            options = options,
        )

    @KonditionalInternalApi
    internal fun patchWithSchema(
        current: Configuration,
        patchJson: String,
        schema: CompiledNamespaceSchema,
        options: SnapshotLoadOptions = SnapshotLoadOptions.strict(),
    ): Result<Configuration> =
        runCatching { patchAdapter.fromJson(patchJson) }
            .fold(
                onSuccess = { parsedPatch ->
                    parsedPatch
                        ?.let { patch ->
                            val currentSerializable = SerializableSnapshot.from(current)
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

private fun io.amichne.konditional.core.instance.ConfigurationMetadataView.toConcrete(): ConfigurationMetadata =
    (this as? ConfigurationMetadata)
        ?: ConfigurationMetadata(
            version = version,
            generatedAtEpochMillis = generatedAtEpochMillis,
            source = source,
        )
