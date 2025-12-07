package io.amichne.konditional.context

import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.Namespace.Authentication.flag
import io.amichne.konditional.core.dsl.KonditionalDsl
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.features.FeatureAware
import io.amichne.konditional.core.features.FeatureContainer

@Deprecated("Use ContextAware instead", ReplaceWith("ContextAware<C>"))
typealias Contextualized<C> = ContextAware<C>

fun interface ContextAware<out C : Context> {
    fun factory(): C
    val context: C get() = factory()

    fun interface Factory<out C : Context> {
        fun context(): C
    }
}

@Suppress("UNCHECKED_CAST")
inline fun <reified C : Context, reified M : Namespace, O> FeatureContainer<M>.contextualize(
    crossinline factory: () -> C,
): O where O : ContextAware<C>, O : FeatureContainer<M> = object : ContextAware<C>, FeatureContainer<M>(namespace) {
    override fun factory(): C = factory()
} as O

inline fun <reified T : Any, reified C : Context, M : Namespace, D> D.feature(
    block: D.() -> Feature<T, M>,
): T where D : ContextAware<C>, D : FeatureAware<M> = flag<T, M>(block()).evaluate(context)

inline fun <reified T : Any, reified C : Context, D : FeatureAware<M>, M : Namespace> D.feature(
    @KonditionalDsl context: () -> C,
    block: D.() -> Feature<T, M>,
): T = feature(context(), block)

inline fun <reified T : Any, reified C : Context, D : FeatureAware<M>, M : Namespace> D.feature(
    context: C,
    @KonditionalDsl block: D.() -> Feature<T, M>,
): T = flag<T, M>(block()).evaluate(context)
