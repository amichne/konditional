package io.amichne.konditional.internal.builders

import io.amichne.konditional.context.axis.Axis
import io.amichne.konditional.context.axis.AxisValue
import io.amichne.konditional.context.axis.AxisValues
import io.amichne.konditional.core.dsl.AxisValuesScope
import io.amichne.konditional.core.dsl.KonditionalDsl
import io.amichne.konditional.core.registry.AxisRegistry

/**
 * Internal builder implementation for constructing [AxisValues] instances.
 *
 * This class implements the [AxisValuesScope] interface and accumulates axis values
 * during DSL execution, then builds an immutable AxisValues instance.
 *
 * ## Usage
 *
 * Typically used via the top-level `axisValues { }` builder function:
 * ```kotlin
 * val values = axisValues {
 *     set(Axes.Environment, Environment.PROD)
 *     set(Axes.Tenant, Tenant.ENTERPRISE)
 * }
 * ```
 *
 * @see AxisValues
 * @see AxisValuesScope
 */
@KonditionalDsl
internal class AxisValuesBuilder : AxisValuesScope {
    private val values = mutableMapOf<String, AxisValue>()

    /**
     * Internal accessor for extracting accumulated values.
     *
     * Used by other builders that need to access the raw map.
     */
    internal fun getValues(): Map<String, AxisValue> = values.toMap()

    /**
     * Sets a value for the given axis.
     */
    override fun <T> set(
        axis: Axis<T>,
        value: T,
    ) where T : AxisValue, T : Enum<T> {
        values[axis.id] = value
    }

    /**
     * Conditionally sets a value, skipping if null.
     */
    override fun <T> setIfNotNull(
        axis: Axis<T>,
        value: T?,
    ) where T : AxisValue, T : Enum<T> {
        if (value != null) set(axis, value)
    }

    /**
     * Type-based value setter using the registry.
     *
     * This extension allows setting values by type without explicitly passing the axis:
     * ```kotlin
     * axisValues {
     *     axis(Environment.PROD)  // Axis inferred from type
     * }
     * ```
     *
     * @param value The value to set
     * @throws IllegalStateException if no axis is registered for type T
     */
    inline fun <reified T> AxisValuesBuilder.axis(value: T) where T : AxisValue, T : Enum<T> {
        AxisRegistry.axisFor(T::class)?.let { set(it, value) } ?: error("No Axis registered for type ${T::class.simpleName}")
    }

    /**
     * Builds an immutable [AxisValues] instance from the accumulated values.
     *
     * @return AxisValues.EMPTY if no values were set, otherwise a new AxisValues instance
     */
    internal fun build(): AxisValues =
        if (values.isEmpty()) AxisValues.EMPTY else AxisValues(values.toMap())
}
