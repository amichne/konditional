package io.amichne.konditional.context

import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.features.FeatureAware
import io.amichne.konditional.core.features.FeatureContainer
import io.amichne.konditional.core.features.evaluate
import io.amichne.konditional.core.types.EncodableValue

@Deprecated("Use ContextAware instead", ReplaceWith("ContextAware<C>"))
typealias Contextualized<C> = ContextAware<C>

fun interface ContextAware<out C : Context> {
    fun factory(): C
    val context: C get() = factory()

    fun interface Factory<out C : Context> {
        fun context(): C
    }
}

////@Suppress("UNCHECKED_CAST")
//inline fun <C : Context, M : Namespace, reified I : FeatureContainer<M>> I.contextualize(
//    crossinline factory: () -> C,
//): ContextAwareFeatureContainer<C, M, I> = ContextAwareFeatureContainer(this) { factory() }
//
//class ContextAwareFeatureContainer<C : Context, M : Namespace, I : FeatureContainer<M>>(
//    override val container: I,
//    private val factoryFn: () -> C,
//) : ContextAware<C>, FeatureAware<M>, FeatureContainer<M>(container.namespace) {
//    override fun factory(): C = factoryFn.invoke()
//}

//inline fun <reified S : EncodableValue<T>, T : Any, C : Context, M : Namespace, reified D> D.feature(
//    block: D.() -> Feature<S, T, C, M>,
//): T where D : ContextAware<C>, D : FeatureAware<M> = block().evaluate(context)
//
//inline fun <reified S : EncodableValue<T>, reified T : Any, reified C : Context, D> D.feature(
//    context: () -> C,
//    block: ContextAware<C>.() -> Feature<S, T, C, *>,
//): T where D : ContextAware<C>, D : FeatureAware<*> = feature(context(), block)
//
//inline fun <reified S : EncodableValue<T>, reified T : Any, reified C : Context, D> D.feature(
//    context: C,
//    block: ContextAware<C>.() -> Feature<S, T, C, *>,
//): T where D : ContextAware<C>, D : FeatureAware<*> = block().evaluate(context)
//
//inline fun <reified S : EncodableValue<T>, T : Any, C : Context, reified M : Namespace, I> I.ofFeature(
//    block: () -> Feature<S, T, C, M>,
//): T where I : ContextAwareFeatureContainer<C, M, I> =
//    block().evaluate(context)

//fun interface ContextAware<out C : Context> {
//    fun factory(): C
//    val context: C get() = factory()
//
//    fun interface Factory<out C : Context> {
//        fun context(): C
//    }
//}

@Suppress("UNCHECKED_CAST")
inline fun <reified C : Context, reified M : Namespace, O> FeatureContainer<M>.contextualize(
    crossinline factory: () -> C,
): O where O : ContextAware<C>, O : FeatureContainer<M> = object : ContextAware<C>, FeatureContainer<M>(namespace) {
    override fun factory(): C = factory()
} as O

inline fun <reified S : EncodableValue<T>, reified T : Any, reified C : Context, M : Namespace, D> D.feature(
    block: D.() -> Feature<S, T, C, M>,
): T where D : ContextAware<C>, D : FeatureAware<M> = block().evaluate(context)

inline fun <reified S : EncodableValue<T>, reified T : Any, reified C : Context, D> D.feature(
    context: () -> C,
    block: ContextAware<C>.() -> Feature<S, T, C, *>,
): T where D : ContextAware<C>, D : FeatureAware<*> = feature(context(), block)

inline fun <reified S : EncodableValue<T>, reified T : Any, reified C : Context, D> D.feature(
    context: C,
    block: ContextAware<C>.() -> Feature<S, T, C, *>,
): T where D : ContextAware<C>, D : FeatureAware<*> = block().evaluate(context)
