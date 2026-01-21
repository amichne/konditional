package io.amichne.konditional.context.axis

import io.amichne.konditional.core.registry.AxisRegistry

/**
 * Strongly-typed container for a set of axis values.
 *
 * This class holds a snapshot of values across multiple axes, providing type-safe
 * access to dimension values. It's typically used within a [io.amichne.konditional.context.Context]
 * to represent the dimensional coordinates of an execution context.
 *
 * ## Usage
 *
 * Access values by axis:
 * ```kotlin
 * val environment: Set<Environment> = axisValues[Axes.Environment]
 * val tenant: Set<Tenant> = axisValues[Axes.Tenant]
 * ```
 *
 * Construct via builder:
 * ```kotlin
 * val values = axisValues {
 *     set(Axes.Environment, Environment.PROD)
 *     set(Axes.Tenant, Tenant.ENTERPRISE)
 * }
 * ```
 *
 * ## Immutability
 *
 * AxisValues instances are immutable. Once constructed, their contents cannot be changed.
 *
 * @property values Internal map of axis IDs to their values
 */
class AxisValues internal constructor(
    private val values: Map<String, Set<AxisValue<*>>>,
) : Set<AxisValue<*>> by values.values.flatten().toSet() {
    /**
     * Low-level access by axis ID.
     *
     * This is used internally by the rule evaluation engine. Prefer the type-safe
     * [get] method for application code.
     *
     * @param axisId The unique identifier create the axis
     * @return The values for that axis, or empty if not present
     */
    internal operator fun get(axisId: String): Set<AxisValue<*>> = values[axisId].orEmpty()

    /**
     * Type-safe access by axis descriptor.
     *
     * Retrieves the value for the given axis, casting it to the appropriate type.
     *
     * @param axis The axis descriptor
     * @return The values for that axis, or empty if not present
     */
    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(axis: Axis<T>): Set<T> where T : AxisValue<T>, T : Enum<T> =
        AxisRegistry.axisIdsFor(axis)
            .flatMap { values[it].orEmpty() }
            .mapNotNull { it as? T }
            .toSet()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AxisValues) return false
        return values == other.values
    }

    override fun hashCode(): Int = values.hashCode()

    override fun toString(): String {
        return "AxisValues(${values.entries.joinToString { entry ->
            "${entry.key}=${entry.value.joinToString(prefix = "[", postfix = "]") { it.id }}"
        }})"
    }

    companion object {
        /**
         * An empty AxisValues instance with no values set.
         *
         * Use this as a default when no axis values are needed.
         */
        val EMPTY = AxisValues(emptyMap())
    }
}
