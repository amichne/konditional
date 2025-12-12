package io.amichne.konditional.context

import io.amichne.konditional.core.Namespace
//import io.amichne.konditional.core.Namespace.Authentication.flag
import io.amichne.konditional.core.dsl.KonditionalDsl
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.features.FeatureAware
import io.amichne.konditional.core.features.evaluate
import io.amichne.konditional.core.types.EncodableValue

fun interface ContextAware<out C : Context> {
    fun factory(): C
    val context: C get() = factory()

    fun interface Factory<out C : Context> {
        fun context(): C
    }
}

inline fun <reified S : EncodableValue<T>, reified T : Any, reified C : Context, M : Namespace, D> D.feature(
    block: D.() -> Feature<S, T, C, M>,
): T where D : ContextAware<C>, D : FeatureAware<M> =
    block().evaluate(context)

/**
 * Lazily obtain a context from a lambda, then evaluate the feature in that context.
 */
inline fun <reified S : EncodableValue<T>, reified T : Any, reified C : Context, D> D.feature(
    @KonditionalDsl crossinline context: () -> C,
    block: ContextAware<C>.() -> Feature<S, T, C, *>,
): T where D : ContextAware<C>, D : FeatureAware<*> {

    @Suppress("UNCHECKED_CAST")
    return ContextAware { context() }.block().evaluate(context())
}

/**
 * Evaluate with an explicit context instance.
 */
inline fun <reified S : EncodableValue<T>, reified T : Any, reified C : Context, D> D.feature(
    context: C,
    @KonditionalDsl block: D.() -> Feature<S, T, C, *>,
): T where D : FeatureAware<*>, D : ContextAware<*> = block().evaluate(context)
