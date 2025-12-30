package io.amichne.konditional.internal.builders

import io.amichne.konditional.context.axis.Axis
import io.amichne.konditional.context.axis.AxisValue
import io.amichne.konditional.context.axis.AxisValues
import io.amichne.konditional.core.dsl.AxisValuesScope
import io.amichne.konditional.core.dsl.KonditionalDsl

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
@PublishedApi
internal class AxisValuesBuilder(val map: MutableMap<String, AxisValue<*>> = mutableMapOf<String, AxisValue<*>>()) :
    AxisValuesScope,
    MutableMap<String, AxisValue<*>> by map {

    /**
     * Internal accessor for extracting accumulated values.
     *
     * Used by other builders that need to access the raw map.
     */
    internal fun getValues(): Map<String, AxisValue<*>> = map.toMap()

    /**
     * Sets a value for the given axis.
     */
    override fun <T> set(
        axis: Axis<T>,
        value: T,
    ) where T : AxisValue<T>, T : Enum<T> {
        require(axis.valueClass == value::class) {
            "Axis ${axis.id} expects ${axis.valueClass.simpleName}, got ${value::class.simpleName}"
        }
        map[axis.id] = value
    }

    /**
     * Conditionally sets a value, skipping if null.
     */
    override fun <T> setIfNotNull(
        axis: Axis<T>,
        value: T?,
    ) where T : AxisValue<T>, T : Enum<T> {
        if (value != null) set(axis, value)
    }

    /**
     * Builds an immutable [AxisValues] instance from the accumulated values.
     *
     * @return AxisValues.EMPTY if no values were set, otherwise a new AxisValues instance
     */
    @PublishedApi
    internal fun build(): AxisValues =
        if (map.isEmpty()) AxisValues.EMPTY else AxisValues(map.toMap())
}
