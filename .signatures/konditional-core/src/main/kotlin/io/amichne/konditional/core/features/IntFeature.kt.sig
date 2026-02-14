file=konditional-core/src/main/kotlin/io/amichne/konditional/core/features/IntFeature.kt
package=io.amichne.konditional.core.features
imports=io.amichne.konditional.context.Context,io.amichne.konditional.core.Namespace
type=io.amichne.konditional.core.features.IntFeature|kind=interface|decl=sealed interface IntFeature<C : Context, M : Namespace> : Feature<Int, C, M>
type=io.amichne.konditional.core.features.IntFeatureImpl|kind=class|decl=internal data class IntFeatureImpl<C : Context, M : Namespace>( override val key: String, override val namespace: M, ) : IntFeature<C, M>, Identifiable by Identifiable(key, namespace)
