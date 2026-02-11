file=konditional-serialization/src/main/kotlin/io/amichne/konditional/internal/serialization/models/SerializableSnapshot.kt
package=io.amichne.konditional.internal.serialization.models
imports=com.squareup.moshi.JsonClass,io.amichne.konditional.api.KonditionalInternalApi,io.amichne.konditional.core.FlagDefinition,io.amichne.konditional.core.features.Feature,io.amichne.konditional.core.result.ParseError,io.amichne.konditional.core.result.ParseResult,io.amichne.konditional.serialization.instance.Configuration,io.amichne.konditional.serialization.instance.ConfigurationMetadata,io.amichne.konditional.serialization.options.SnapshotLoadOptions,io.amichne.konditional.serialization.options.SnapshotWarning,io.amichne.konditional.serialization.options.UnknownFeatureKeyStrategy
type=io.amichne.konditional.internal.serialization.models.SerializableSnapshot|kind=class|decl=data class SerializableSnapshot( val meta: SerializableSnapshotMetadata? = null, val flags: List<SerializableFlag>, )
methods:
- fun toConfiguration(): ParseResult<Configuration>
- fun toConfiguration(options: SnapshotLoadOptions): ParseResult<Configuration>
