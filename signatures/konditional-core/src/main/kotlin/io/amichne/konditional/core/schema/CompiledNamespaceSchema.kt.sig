file=konditional-core/src/main/kotlin/io/amichne/konditional/core/schema/CompiledNamespaceSchema.kt
package=io.amichne.konditional.core.schema
imports=io.amichne.konditional.api.KonditionalInternalApi,io.amichne.konditional.core.FlagDefinition,io.amichne.konditional.core.Namespace,io.amichne.konditional.core.features.Feature,io.amichne.konditional.values.FeatureId
type=io.amichne.konditional.core.schema.CompiledNamespaceSchema|kind=class|decl=data class CompiledNamespaceSchema( val namespaceId: String, val entriesById: Map<FeatureId, Entry>, )
type=io.amichne.konditional.core.schema.Entry|kind=class|decl=data class Entry( val featureId: FeatureId, val feature: Feature<*, *, *>, val declaredDefinition: FlagDefinition<*, *, *>, )
fields:
- val entriesInDeterministicOrder: List<Entry>
