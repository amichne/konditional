file=konditional-core/src/main/kotlin/io/amichne/konditional/core/features/DoubleFeature.kt
package=io.amichne.konditional.core.features
imports=io.amichne.konditional.context.Context,io.amichne.konditional.core.Namespace
type=io.amichne.konditional.core.features.DoubleFeature|kind=interface|decl=sealed interface DoubleFeature<C : Context, M : Namespace> :
type=io.amichne.konditional.core.features.DoubleFeatureImpl|kind=class|decl=internal data class DoubleFeatureImpl<C : Context, M : Namespace>( override val key: String, override val namespace: M, ) : DoubleFeature<C, M>, Identifiable by Identifiable(key, namespace)
