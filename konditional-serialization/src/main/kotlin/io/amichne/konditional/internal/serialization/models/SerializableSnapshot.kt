package io.amichne.konditional.internal.serialization.models

import com.squareup.moshi.JsonClass
import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.parseErrorOrNull
import io.amichne.konditional.core.result.parseFailure
import io.amichne.konditional.core.schema.CompiledNamespaceSchema
import io.amichne.konditional.serialization.instance.Configuration
import io.amichne.konditional.serialization.instance.ConfigurationMetadata
import io.amichne.konditional.serialization.instance.MaterializedConfiguration
import io.amichne.konditional.serialization.options.MissingDeclaredFlagStrategy
import io.amichne.konditional.serialization.options.SnapshotLoadOptions
import io.amichne.konditional.serialization.options.SnapshotWarning
import io.amichne.konditional.serialization.options.UnknownFeatureKeyStrategy

/**
 * Serializable representation of a Configuration configuration.
 * This is the top-level object that gets serialized to/from JSON.
 */
@KonditionalInternalApi
@JsonClass(generateAdapter = true)
data class SerializableSnapshot(
    val meta: SerializableSnapshotMetadata? = null,
    val flags: List<SerializableFlag>,
) {
    fun toConfiguration(
        schema: CompiledNamespaceSchema,
        options: SnapshotLoadOptions = SnapshotLoadOptions.strict(),
    ): Result<MaterializedConfiguration> =
        materializedFlags(schema, options)
            .map { resolvedFlags ->
                MaterializedConfiguration.of(
                    schema = schema,
                    configuration = Configuration(
                        flags = resolvedFlags.toMap(),
                        metadata = meta?.toConfigurationMetadata() ?: ConfigurationMetadata(),
                    ),
                )
            }

    private fun materializedFlags(
        schema: CompiledNamespaceSchema,
        options: SnapshotLoadOptions,
    ): Result<LinkedHashMap<Feature<*, *, *>, FlagDefinition<*, *, *>>> =
        resolveSerializableFlags(schema, options)
            .fold(
                onSuccess = { resolvedFlags ->
                    mergeMissingDeclaredFlags(
                        resolvedFlags = resolvedFlags,
                        schema = schema,
                        options = options,
                    )
                },
                onFailure = { error -> Result.failure(error) },
            )

    private fun resolveSerializableFlags(
        schema: CompiledNamespaceSchema,
        options: SnapshotLoadOptions,
    ): Result<LinkedHashMap<Feature<*, *, *>, FlagDefinition<*, *, *>>> {
        val resolvedFlags = linkedMapOf<Feature<*, *, *>, FlagDefinition<*, *, *>>()
        for (serializableFlag in flags) {
            val pairResult = serializableFlag.toFlagPair(schema)
            if (pairResult.isSuccess) {
                val (feature, definition) = pairResult.getOrThrow()
                resolvedFlags[feature] = definition
            } else {
                val error = pairResult.parseErrorOrNull()
                val featureNotFound = error as? ParseError.FeatureNotFound
                if (featureNotFound != null && options.unknownFeatureKeyStrategy is UnknownFeatureKeyStrategy.Skip) {
                    options.onWarning(SnapshotWarning.unknownFeatureKey(featureNotFound.key))
                } else {
                    return parseFailure(
                        error
                            ?: ParseError.InvalidSnapshot(
                                pairResult.exceptionOrNull()?.message ?: "Unknown materialization failure",
                            ),
                    )
                }
            }
        }
        return Result.success(resolvedFlags)
    }

    private fun mergeMissingDeclaredFlags(
        resolvedFlags: LinkedHashMap<Feature<*, *, *>, FlagDefinition<*, *, *>>,
        schema: CompiledNamespaceSchema,
        options: SnapshotLoadOptions,
    ): Result<LinkedHashMap<Feature<*, *, *>, FlagDefinition<*, *, *>>> {
        val missingDeclared = schema.entriesInDeterministicOrder.filter { entry ->
            resolvedFlags[entry.feature] == null
        }

        return when {
            missingDeclared.isEmpty() -> Result.success(resolvedFlags)
            options.missingDeclaredFlagStrategy is MissingDeclaredFlagStrategy.FillFromDeclaredDefaults -> {
                missingDeclared.forEach { entry ->
                    resolvedFlags[entry.feature] = entry.declaredDefinition
                }
                Result.success(resolvedFlags)
            }

            else -> {
                val missingIds = missingDeclared.joinToString(", ") { entry -> entry.featureId.toString() }
                parseFailure(
                    ParseError.invalidSnapshot(
                        "Missing declared flags for namespace '${schema.namespaceId}': $missingIds",
                    ),
                )
            }
        }
    }

    companion object {
        fun from(configuration: Configuration): SerializableSnapshot =
            SerializableSnapshot(
                meta = SerializableSnapshotMetadata.from(configuration.metadata),
                flags = configuration.flags.map { (feature, flag) ->
                    SerializableFlag.from(flag, feature.id)
                },
            )
    }
}
