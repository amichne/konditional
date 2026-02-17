package io.amichne.konditional.core.dsl.rules.targeting.scopes

import io.amichne.konditional.context.Context
import io.amichne.konditional.context.axis.Axis
import io.amichne.konditional.context.axis.AxisValue
import io.amichne.konditional.core.dsl.KonditionalDsl

/**
 * Targeting mix-in for axis constraints.
 */
@KonditionalDsl
interface AxisTargetingScope<C : Context> {
    /**
     * Explicit axis targeting.
     *
     * Use this overload when you already have an [Axis] handle; this is the preferred
     * form because it avoids type-inference indirection.
     *
     * @param axis Axis descriptor to constrain
     * @param values Allowed values for [axis]
     */
    fun <T> axis(
        axis: Axis<T>,
        vararg values: T,
    ) where T : AxisValue<T>, T : Enum<T>

    /**
     * Type-inferred axis targeting.
     *
     * Example:
     * ```kotlin
     * axis(Environment.PROD, Environment.STAGE)
     * axis(Tenant.ENTERPRISE)
     * ```
     *
     * Requires that an axis is already registered for the value type [T]
     * in the active scoped [io.amichne.konditional.core.registry.AxisCatalog].
     *
     * @param T The axis value type
     * @param values The values to allow for this axis
     */
    fun <T> axis(
        vararg values: T,
    ) where T : AxisValue<T>, T : Enum<T>
}
