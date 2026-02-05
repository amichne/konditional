package io.amichne.konditional.core.dsl.rules.targeting.scopes

import io.amichne.konditional.context.Context
import io.amichne.konditional.context.axis.AxisValue
import io.amichne.konditional.core.dsl.KonditionalDsl

/**
 * Targeting mix-in for axis constraints.
 */
@KonditionalDsl
interface AxisTargetingScope<C : Context> {
    /**
     * Specifies axis targeting for this rule.
     *
     * Example:
     * ```kotlin
     * axis(Environment.PROD, Environment.STAGE)
     * axis( Tenant.ENTERPRISE)
     * ```
     *
     * Adds targeting criteria based on custom axes defined in the context,
     * allowing for more granular control over rule applicability beyond
     * standard locale, platform, and version criteria.
     *
     * @param T The axis value type
     * @param values The values to allow for this axis
     */
    fun <T> axis(
        vararg values: T,
    ) where T : AxisValue<T>, T : Enum<T>
}
