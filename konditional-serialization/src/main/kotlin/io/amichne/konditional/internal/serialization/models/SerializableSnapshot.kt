package io.amichne.konditional.internal.serialization.models

import com.squareup.moshi.JsonClass
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.instance.Configuration
import io.amichne.konditional.core.instance.ConfigurationMetadata
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.serialization.options.SnapshotLoadOptions
import io.amichne.konditional.serialization.options.SnapshotWarning
import io.amichne.konditional.serialization.options.UnknownFeatureKeyStrategy

/**
 * Serializable representation of a Configuration configuration.
 * This is the top-level object that gets serialized to/from JSON.
 */
@JsonClass(generateAdapter = true)
internal data class SerializableSnapshot(
    val meta: SerializableSnapshotMetadata? = null,
    val flags: List<SerializableFlag>,
) {
    internal fun toConfiguration(): ParseResult<Configuration> = toConfiguration(SnapshotLoadOptions.strict())

    internal fun toConfiguration(options: SnapshotLoadOptions): ParseResult<Configuration> =
        runCatching {
            val initial: ParseResult<MutableMap<Feature<*, *, *>, FlagDefinition<*, *, *>>> =
                ParseResult.Success(linkedMapOf())

            val flagsResult =
                flags.fold(initial) { acc, serializableFlag ->
                    when (acc) {
                        is ParseResult.Failure -> acc
                        is ParseResult.Success -> {
                            when (val pairResult = serializableFlag.toFlagPair()) {
                                is ParseResult.Success -> {
                                    acc.value[pairResult.value.first] = pairResult.value.second
                                    acc
                                }

                                is ParseResult.Failure -> {
                                    if (pairResult.error is ParseError.FeatureNotFound &&
                                        options.unknownFeatureKeyStrategy is UnknownFeatureKeyStrategy.Skip
                                    ) {
                                        options.onWarning(SnapshotWarning.unknownFeatureKey(pairResult.error.key))
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
                is ParseResult.Success -> ParseResult.Success(
                    Configuration(
                        flagsResult.value.toMap(),
                        meta?.toConfigurationMetadata() ?: ConfigurationMetadata.of(),
                    )
                )
                is ParseResult.Failure -> flagsResult
            }
        }.getOrElse { ParseResult.Failure(ParseError.InvalidSnapshot(it.message ?: "Unknown error")) }

    internal companion object {
        fun from(configuration: Configuration): SerializableSnapshot =
            SerializableSnapshot(
                meta = SerializableSnapshotMetadata.from(configuration.metadata),
                flags = configuration.flags.map { (feature, flag) ->
                    SerializableFlag.from(flag, feature.id)
                },
            )
    }
}
