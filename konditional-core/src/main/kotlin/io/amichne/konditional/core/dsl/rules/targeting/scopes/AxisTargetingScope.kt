package io.amichne.konditional.core.dsl.rules.targeting.scopes

import io.amichne.konditional.context.Context
import io.amichne.konditional.context.axis.Axis
import io.amichne.konditional.context.axis.AxisValue
import io.amichne.konditional.core.dsl.KonditionalDsl

/**
 * Legacy targeting mix-in for axis constraints.
 *
 * Use `variant { ... }` instead of these error-deprecated methods.
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
    @Deprecated(
        message = "Use variant { axis { include(...) } } for axis targeting.",
        replaceWith =
            ReplaceWith(
                "variant { axis { include(values.first(), *values.drop(1).toTypedArray()) } }",
            ),
        level = DeprecationLevel.ERROR,
    )
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
     * @param T The axis value type
     * @param values The values to allow for this axis
     */
    @Deprecated(
        message = "Use variant { axisHandle { include(...) } } with an explicit axis handle.",
        level = DeprecationLevel.ERROR,
    )
    fun <T> axis(
        vararg values: T,
    ) where T : AxisValue<T>, T : Enum<T>
}
