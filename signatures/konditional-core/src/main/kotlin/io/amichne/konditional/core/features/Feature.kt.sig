file=konditional-core/src/main/kotlin/io/amichne/konditional/core/features/Feature.kt
package=io.amichne.konditional.core.features
imports=io.amichne.konditional.context.Context,io.amichne.konditional.core.Namespace,io.amichne.konditional.values.FeatureId
type=io.amichne.konditional.core.features.Feature|kind=interface|decl=sealed interface Feature<T : Any, C : Context, out M : Namespace> : Identifiable
fields:
- val key: String
- val namespace: M
- override val id: FeatureId
