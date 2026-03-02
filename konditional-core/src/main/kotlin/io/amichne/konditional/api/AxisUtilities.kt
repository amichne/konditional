package io.amichne.konditional.api

import io.amichne.konditional.context.Context
import io.amichne.konditional.context.axis.Axis
import io.amichne.konditional.context.axis.AxisValue
import io.amichne.konditional.context.axis.Axes
import io.amichne.konditional.core.dsl.AxisValuesScope
import io.amichne.konditional.internal.builders.AxisValuesBuilder

/**
 * Type-based axis value getter.
 *
 * This function retrieves the value of the axis corresponding to the reified type [T], and
 * returns it as a set of [T] values if present in the context. If the axis is not found, it returns an empty set.
 *
 * ```kotlin
 *  val env: Set<Environment> = context.axis<Environment>()
 * ```
 */
inline fun <reified T> Context.axis(): Set<T> where T : AxisValue<T>, T : Enum<T> =
    axes.valuesFor(T::class)

/**
 * Axis-based value getter.
 *
 * Example:
 *   val env = ctx.axis(Axes.Environment)
 */
@Deprecated("Use type-based axis() instead for more concise syntax", ReplaceWith("axis<T>()"))
inline fun <reified T, reified C : Context> C.axis(axis: Axis<T>): Set<T> where T : AxisValue<T>, T : Enum<T> =
    axes[axis]

/**
 * Top-level DSL function to create [AxisValues].
 *
 * Example:
 * ```kotlin
 * val values = axisValues {
 *     variant {
 *         Axes.Environment { include(Environment.PROD) }
 *         Axes.Tenant { include(Tenant.SME) }
 *     }
 * }
 * ```
 */
@Deprecated("Use axes(axis1, axis2, ...) instead for more concise syntax", ReplaceWith("axes()"))
inline fun axisValues(
    block: AxisValuesScope.() -> Unit,
): Axes = AxisValuesBuilder().apply(block).build()
