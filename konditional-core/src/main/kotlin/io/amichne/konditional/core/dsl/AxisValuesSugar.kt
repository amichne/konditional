package io.amichne.konditional.core.dsl

import io.amichne.konditional.context.axis.Axis
import io.amichne.konditional.context.axis.AxisValue

/**
 * Declares axis values for context construction.
 *
 * Expresses that the context is aligned along the given axes with the specified values.
 * Each value carries its own axis information, enabling heterogeneous axis
 * declarations in a single call.
 *
 * ## Usage
 *
 * ```kotlin
 * axes {
 *     axis(Environment.PROD)
 *     axis(Environment.STAGE, Tenant.ENTERPRISE)  // Multiple axes in one call
 * }
 * ```
 *
 * This replaces the verbose `variant { axisHandle { include(...) } }` nesting:
 * ```kotlin
 * // Before:
 * axes {
 *     variant {
 *         Axes.Environment { include(Environment.PROD) }
 *         Axes.Tenant { include(Tenant.ENTERPRISE) }
 *     }
 * }
 *
 * // After:
 * axes {
 *     axis(Environment.PROD, Tenant.ENTERPRISE)
 * }
 * ```
 *
 * ## Semantics
 *
 * - Each value's axis is derived from its enum class via `Axis.axes()`.
 * - Values are grouped by axis automatically.
 * - Multiple calls for the same axis accumulate values (union semantics).
 * - Requires at least one value (`first` parameter) for non-empty guarantee.
 *
 * @param first First value to include (required for non-empty guarantee)
 * @param rest Additional values to include (can be from different axes)
 */
fun AxisValuesScope.axis(
    first: AxisValue<*>,
    vararg rest: AxisValue<*>,
) {
    val host = this as? VariantDispatchHost
        ?: error("Unsupported AxisValuesScope receiver: ${this::class.qualifiedName}")

    // Group values by their enum class (which determines the axis)
    val allValues = listOf(first) + rest
    val grouped = allValues.groupBy { (it as Enum<*>).javaClass.kotlin }

    // Dispatch each axis group
    grouped.forEach { (enumClass, values) ->
        // Derive the axis from the enum class
        @Suppress("UNCHECKED_CAST")
        val axis = Axis.of(enumClass as kotlin.reflect.KClass<Nothing>)
        @Suppress("UNCHECKED_CAST")
        host.onAxisSelection(axis, values.toSet() as Set<Nothing>)
    }
}
