package io.amichne.konditional.api

import io.amichne.konditional.context.Context
import io.amichne.konditional.context.axis.Axis
import io.amichne.konditional.context.axis.AxisValue
import io.amichne.konditional.context.axis.AxisValues
import io.amichne.konditional.core.dsl.AxisValuesScope
import io.amichne.konditional.core.registry.AxisRegistry
import io.amichne.konditional.internal.builders.AxisValuesBuilder

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
inline fun <reified T> Context.axis(): T? where T : AxisValue<T>, T : Enum<T> =
    AxisRegistry.axisFor(T::class)?.let { axisValues[it] }

/**
 * Axis-based value getter.
 *
 * Example:
 *   val env = ctx.axis(Axes.Environment)
 */
inline fun <reified T, reified C : Context> C.axis(axis: Axis<T>): T? where T : AxisValue<T>, T : Enum<T> =
    axisValues[axis]

/**
 * Top-level DSL function to create [AxisValues]
 *
 * Example:
 * ```kotlin
 * val values = axisValues {
 *     environment(Environment.PROD)
 *     tenant(Tenant.SME)
 * }
 * ```
 *
 * @param block
 * @receiver
 * @return
 */
inline fun axisValues(block: AxisValuesScope.() -> Unit): AxisValues =
    AxisValuesBuilder().apply(block).build()
