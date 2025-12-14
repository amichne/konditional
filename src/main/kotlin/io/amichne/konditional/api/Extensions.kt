package io.amichne.konditional.api

import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Context.Companion.getAxisValue
import io.amichne.konditional.context.ContextAware
import io.amichne.konditional.context.axis.Axis
import io.amichne.konditional.context.axis.AxisValue
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.KonditionalDsl
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.features.FeatureAware
import io.amichne.konditional.core.features.evaluate
import io.amichne.konditional.core.registry.AxisRegistry
import io.amichne.konditional.core.types.EncodableValue

/**
 * Type-based axis value getter.
 *
 * Example:
 *   val env: Environment? = ctx.axis<Environment>()
 */
inline fun <reified T> Context.axis(): T? where T : AxisValue, T : Enum<T> {
    val axisDescriptor = AxisRegistry.axisFor(T::class) ?: return null
    @Suppress("UNCHECKED_CAST")
    return axisValues[axisDescriptor]
}

/**
 * Axis-based value getter.
 *
 * Example:
 *   val env = ctx.axis(Axes.Environment)
 */
inline fun <reified T, reified C : Context> C.axis(axis: Axis<T>): T? where T : AxisValue, T : Enum<T> =
    getAxisValue(axis.id) as? T

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
