file=konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/instance/Configuration.kt
package=io.amichne.konditional.serialization.instance
imports=io.amichne.konditional.api.KonditionalInternalApi,io.amichne.konditional.core.FlagDefinition,io.amichne.konditional.core.features.Feature,io.amichne.konditional.core.instance.ConfigurationMetadataView,io.amichne.konditional.core.instance.ConfigurationView
type=io.amichne.konditional.serialization.instance.Configuration|kind=class|decl=data class Configuration( override val flags: Map<Feature<*, *, *>, FlagDefinition<*, *, *>>, override val metadata: ConfigurationMetadata = ConfigurationMetadata(), ) : ConfigurationView
type=io.amichne.konditional.serialization.instance.ConfigurationMetadata|kind=class|decl=data class ConfigurationMetadata( override val version: String? = null, override val generatedAtEpochMillis: Long? = null, override val source: String? = null, ) : ConfigurationMetadataView
methods:
- fun diff(other: Configuration): ConfigurationDiff
- fun withMetadata(metadata: ConfigurationMetadata): Configuration
- fun withMetadata( version: String? = null, generatedAtEpochMillis: Long? = null, source: String? = null, ): Configuration
