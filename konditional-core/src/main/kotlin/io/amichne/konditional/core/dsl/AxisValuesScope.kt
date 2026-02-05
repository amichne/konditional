package io.amichne.konditional.core.dsl

import io.amichne.konditional.context.axis.Axis
import io.amichne.konditional.context.axis.AxisValue

/**
 * DSL scope for configuring axis values.
 *
 * This interface provides type-safe methods to add values along defined axes,
 * allowing users to construct [io.amichne.konditional.context.axis.AxisValues]
 * instances in a fluent and expressive manner.
 *
 * ## Usage
 *
 * Within a builder context:
 * ```kotlin
 * axisValues {
 *     set(Axes.Environment, Environment.PROD)
 *     set(Axes.Tenant, Tenant.ENTERPRISE)
 *
 *     // Optional setter - skips if null
 *     setIfNotNull(Axes.Region, maybeRegion)
 * }
 * ```
 *
 * Operator syntax:
 * ```kotlin
 * axisValues {
 *     this[Axes.Environment] = Environment.PROD
 * }
 * ```
 *
 * @see io.amichne.konditional.context.axis.AxisValues
 * @see Axis
 * @see AxisValue
 */
@KonditionalDsl
interface AxisValuesScope {
    /**
     * Adds a value for the given axis.
     *
     * @param axis The axis descriptor
     * @param value The value to add
     */
    operator fun <T> set(
        axis: Axis<T>,
        value: T,
    ) where T : AxisValue<T>, T : Enum<T>

    /**
     * Conditionally sets a value for the given axis, skipping if the value is null.
     *
     * This is useful when you have optional axis values that may not always be present.
     *
     * @param axis The axis descriptor
     * @param value The value to set, or null to skip
     */
    fun <T> setIfNotNull(
        axis: Axis<T>,
        value: T?,
    ) where T : AxisValue<T>, T : Enum<T>
}
