file=konditional-core/src/main/kotlin/io/amichne/konditional/core/instance/ConfigurationView.kt
package=io.amichne.konditional.core.instance
imports=io.amichne.konditional.api.KonditionalInternalApi,io.amichne.konditional.core.FlagDefinition,io.amichne.konditional.core.features.Feature
type=io.amichne.konditional.core.instance.ConfigurationView|kind=interface|decl=interface ConfigurationView
type=io.amichne.konditional.core.instance.ConfigurationMetadataView|kind=interface|decl=interface ConfigurationMetadataView
fields:
- val flags: Map<Feature<*, *, *>, FlagDefinition<*, *, *>>
- val metadata: ConfigurationMetadataView
- val version: String?
- val generatedAtEpochMillis: Long?
- val source: String?
