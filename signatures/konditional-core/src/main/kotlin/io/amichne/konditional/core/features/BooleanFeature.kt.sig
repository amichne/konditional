file=konditional-core/src/main/kotlin/io/amichne/konditional/core/features/BooleanFeature.kt
package=io.amichne.konditional.core.features
imports=io.amichne.konditional.context.Context,io.amichne.konditional.core.Namespace
type=io.amichne.konditional.core.features.BooleanFeature|kind=interface|decl=sealed interface BooleanFeature<C : Context, M : Namespace> : Feature<Boolean, C, M>
type=io.amichne.konditional.core.features.BooleanFeatureImpl|kind=class|decl=internal data class BooleanFeatureImpl<C : Context, M : Namespace>( override val key: String, override val namespace: M, ) : BooleanFeature<C, M>, Identifiable by Identifiable(key, namespace)
