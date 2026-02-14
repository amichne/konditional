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
    ): Result<MaterializedConfiguration> {
        val resolvedFlags = linkedMapOf<Feature<*, *, *>, FlagDefinition<*, *, *>>()

        flags.forEach { serializableFlag ->
            val pairResult = serializableFlag.toFlagPair(schema)
            if (pairResult.isSuccess) {
                val pair = pairResult.getOrThrow()
                resolvedFlags[pair.first] = pair.second
                return@forEach
            }

            val error = pairResult.parseErrorOrNull()
            val featureNotFound = error as? ParseError.FeatureNotFound
            if (featureNotFound != null && options.unknownFeatureKeyStrategy is UnknownFeatureKeyStrategy.Skip) {
                options.onWarning(SnapshotWarning.unknownFeatureKey(featureNotFound.key))
                return@forEach
            }

            return parseFailure(
                error
                    ?: ParseError.InvalidSnapshot(
                        pairResult.exceptionOrNull()?.message ?: "Unknown materialization failure",
                    ),
            )
        }

        val missingDeclared = schema.entriesInDeterministicOrder.filter { entry ->
            resolvedFlags[entry.feature] == null
        }

        when {
            missingDeclared.isEmpty() -> {
                // no-op
            }

            options.missingDeclaredFlagStrategy is MissingDeclaredFlagStrategy.FillFromDeclaredDefaults -> {
                missingDeclared.forEach { entry ->
                    resolvedFlags[entry.feature] = entry.declaredDefinition
                }
            }

            else -> {
                val missingIds = missingDeclared.joinToString(", ") { entry -> entry.featureId.toString() }
                return parseFailure(
                    ParseError.invalidSnapshot(
                        "Missing declared flags for namespace '${schema.namespaceId}': $missingIds",
                    ),
                )
            }
        }

        return Result.success(
            MaterializedConfiguration.of(
                schema = schema,
                configuration = Configuration(
                    flags = resolvedFlags.toMap(),
                    metadata = meta?.toConfigurationMetadata() ?: ConfigurationMetadata(),
                ),
            ),
        )
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
