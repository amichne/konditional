file=konditional-core/src/main/kotlin/io/amichne/konditional/core/features/KotlinClassFeature.kt
package=io.amichne.konditional.core.features
imports=io.amichne.konditional.context.Context,io.amichne.konditional.core.Namespace,io.amichne.konditional.core.types.Konstrained
type=io.amichne.konditional.core.features.KotlinClassFeature|kind=interface|decl=sealed interface KotlinClassFeature<T : Konstrained<*>, C : Context, M : Namespace> :
type=io.amichne.konditional.core.features.KotlinClassFeatureImpl|kind=class|decl=internal data class KotlinClassFeatureImpl<T : Konstrained<*>, C : Context, M : Namespace>( override val key: String, override val namespace: M, ) : KotlinClassFeature<T, C, M>, Identifiable by Identifiable(key, namespace)
