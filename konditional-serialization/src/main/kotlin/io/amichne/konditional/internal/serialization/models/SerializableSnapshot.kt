package io.amichne.konditional.internal.serialization.models

import com.squareup.moshi.JsonClass
import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.serialization.instance.Configuration
import io.amichne.konditional.serialization.instance.ConfigurationMetadata
import io.amichne.konditional.serialization.options.SnapshotLoadOptions
import io.amichne.konditional.serialization.options.SnapshotWarning
import io.amichne.konditional.serialization.options.UnknownFeatureKeyStrategy
import io.amichne.konditional.values.FeatureId

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
    fun toConfiguration(): ParseResult<Configuration> = toConfiguration(SnapshotLoadOptions.strict())

    fun toConfiguration(
        options: SnapshotLoadOptions,
        featuresById: Map<FeatureId, Feature<*, *, *>> = emptyMap(),
    ): ParseResult<Configuration> =
        runCatching {
            val initial: ParseResult<MutableMap<Feature<*, *, *>, FlagDefinition<*, *, *>>> =
                ParseResult.success(linkedMapOf())

            val flagsResult =
                flags.fold(initial) { acc, serializableFlag ->
                    when (acc) {
                        is ParseResult.Failure -> acc
                        is ParseResult.Success -> {
                            when (val pairResult = serializableFlag.toFlagPair(featuresById)) {
                                is ParseResult.Success -> {
                                    acc.value[pairResult.value.first] = pairResult.value.second
                                    acc
                                }

                                is ParseResult.Failure -> {
                                    val featureNotFound = pairResult.error as? ParseError.FeatureNotFound
                                    if (featureNotFound != null &&
                                        options.unknownFeatureKeyStrategy is UnknownFeatureKeyStrategy.Skip
                                    ) {
                                        options.onWarning(SnapshotWarning.unknownFeatureKey(featureNotFound.key))
                                        acc
                                    } else {
                                        pairResult
                                    }
                                }
                            }
                        }
                    }
                }

            when (flagsResult) {
                is ParseResult.Success -> ParseResult.success(
                    Configuration(
                        flagsResult.value.toMap(),
                        meta?.toConfigurationMetadata() ?: ConfigurationMetadata(),
                    )
                )
                is ParseResult.Failure -> flagsResult
            }
        }.getOrElse { ParseResult.failure(ParseError.InvalidSnapshot(it.message ?: "Unknown error")) }

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
