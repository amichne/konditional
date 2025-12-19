package io.amichne.konditional.core.dsl

import io.amichne.konditional.context.axis.AxisValue
import io.amichne.konditional.core.registry.AxisRegistry.axisFor

/**
 * Axis
 *
 * @param T
 * @param value
 */
inline fun <reified T> RuleScope<*>.axis(vararg value: T) where T : AxisValue, T : Enum<T> {
    requireNotNull(axisFor(T::class))
    this.axis(*value)
}
