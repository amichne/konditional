file=konditional-core/src/main/kotlin/io/amichne/konditional/core/features/StringFeature.kt
package=io.amichne.konditional.core.features
imports=io.amichne.konditional.context.Context,io.amichne.konditional.core.Namespace
type=io.amichne.konditional.core.features.StringFeature|kind=interface|decl=sealed interface StringFeature<C : Context, M : Namespace> : Feature<String, C, M>
type=io.amichne.konditional.core.features.StringFeatureImpl|kind=class|decl=internal data class StringFeatureImpl<C : Context, M : Namespace>( override val key: String, override val namespace: M, ) : StringFeature<C, M>, Identifiable by Identifiable(key, namespace)
