package io.amichne.konditional.core.dsl

import io.amichne.konditional.context.axis.Axis
import io.amichne.konditional.context.axis.AxisValue
import io.amichne.konditional.core.registry.AxisRegistry

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
 * If no axis is registered for type T, an implicit axis is created using the
 * value type's qualified name as its id.
 */
inline fun <reified T> AxisValuesScope.axis(value: T) where T : AxisValue<T>, T : Enum<T> {
    AxisRegistry.axisForOrRegister(T::class).let { set(it, value) }
}

context(scope: AxisValuesScope)
inline operator fun <reified T> T.unaryPlus() where T : AxisValue<T>, T : Enum<T> = scope.axis(this)
