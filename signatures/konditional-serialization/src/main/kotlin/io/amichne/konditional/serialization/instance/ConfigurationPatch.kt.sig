file=konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/instance/ConfigurationPatch.kt
package=io.amichne.konditional.serialization.instance
imports=io.amichne.konditional.api.KonditionalInternalApi,io.amichne.konditional.context.Context,io.amichne.konditional.core.FlagDefinition,io.amichne.konditional.core.features.Feature
type=io.amichne.konditional.serialization.instance.ConfigurationPatch|kind=class|decl=data class ConfigurationPatch( val flags: Map<Feature<*, *, *>, FlagDefinition<*, *, *>>, val removeKeys: Set<Feature<*, *, *>> = emptySet(), )
type=io.amichne.konditional.serialization.instance.PatchBuilder|kind=class|decl=class PatchBuilder
methods:
- fun applyTo(configuration: Configuration): Configuration
- fun <T : Any, C : Context> add( entry: FlagDefinition<T, C, *>, )
- fun remove(key: Feature<*, *, *>)
