package io.amichne.konditional.context

import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.DimensionScope
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

inline fun <reified T> dimensionAxis(id: String): Dimension<T> where T : DimensionKey, T : Enum<T> =
    object : RegisteredDimension<T>(id, T::class) {}

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

/**
 * Axis-centric getter: Axes.Environment.valueIn(ctx)
 */
fun <T> Dimension<T>.valueIn(ctx: Context): T? where T : DimensionKey, T : Enum<T> =
    ctx.dimensions[this]

/**
 * Axis-centric setter: Axes.Environment.setIn(builder, Environment.PROD)
 */
fun <T> Dimension<T>.setIn(
    builder: DimensionScope,
    value: T,
) where T : DimensionKey, T : Enum<T> {
    builder.set(this, value)
}

/**
 * Operator sugar:
 *   val env: Environment? = Axes.Environment(ctx)
 *   Axes.Environment(builder, Environment.PROD)
 */
operator fun <T> Dimension<T>.invoke(ctx: Context): T? where T : DimensionKey, T : Enum<T> =
    valueIn(ctx)

operator fun <T> Dimension<T>.invoke(
    builder: DimensionScope,
    value: T,
) where T : DimensionKey, T : Enum<T> {
    setIn(builder, value)
}
