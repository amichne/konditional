package io.amichne.konditional.api

import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Context.Companion.getDimension
import io.amichne.konditional.context.ContextAware
import io.amichne.konditional.context.dimension.Dimension
import io.amichne.konditional.context.dimension.DimensionKey
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.KonditionalDsl
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.features.FeatureAware
import io.amichne.konditional.core.features.evaluate
import io.amichne.konditional.core.registry.DimensionRegistry
import io.amichne.konditional.core.types.EncodableValue

inline fun <reified T, reified C : Context> C.dimension(axis: Dimension<T>): T? where T : DimensionKey, T : Enum<T> =
    getDimension(axis.id) as? T

/**
 * Type-based dimension getter.
 *
 * Example:
 *   val env: Environment? = ctx.dimension<Environment>()
 */
inline fun <reified T> Context.dimension(): T? where T : DimensionKey, T : Enum<T> {
    val axis = DimensionRegistry.axisFor(T::class) ?: return null
    @Suppress("UNCHECKED_CAST")
    return dimensions[axis]
}



inline fun <reified T> dimensionAxis(id: String): Dimension<T> where T : DimensionKey, T : Enum<T> = Dimension<T>(id)

inline fun <reified S : EncodableValue<T>, reified T : Any, reified C : Context, M : Namespace, D> D.feature(
    block: D.() -> Feature<S, T, C, M>,
): T where D : ContextAware<C>, D : FeatureAware<out M> = block().evaluate(context)

/**
 * Evaluate with an explicit context instance.
 */
inline fun <reified S : EncodableValue<T>, reified T : Any, reified C : Context, D> D.feature(
    context: C,
    @KonditionalDsl block: D.() -> Feature<S, T, C, *>,
): T where D : FeatureAware<*>, D : ContextAware<*> = block().evaluate(context)
