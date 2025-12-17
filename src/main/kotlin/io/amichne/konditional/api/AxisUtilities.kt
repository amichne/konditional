package io.amichne.konditional.api

import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Context.Companion.getAxisValue
import io.amichne.konditional.context.axis.Axis
import io.amichne.konditional.context.axis.AxisValue
import io.amichne.konditional.core.registry.AxisRegistry

/**
 * Type-based axis value getter.
 *
 * This function retrieves the value of the axis corresponding to the reified type [T], and
 * returns it as an instance of [T] if present in the context. If the axis is not found, it returns null.
 *
 * ```kotlin
 *  val env: Environment? = context.axis<Environment>()
 * ```
 */
inline fun <reified T> Context.axis(): T? where T : AxisValue, T : Enum<T> =
    AxisRegistry.axisFor(T::class)?.let { axisValues[it] }

/**
 * Axis-based value getter.
 *
 * Example:
 *   val env = ctx.axis(Axes.Environment)
 */
inline fun <reified T, reified C : Context> C.axis(axis: Axis<T>): T? where T : AxisValue, T : Enum<T> =
    getAxisValue(axis.id) as? T
