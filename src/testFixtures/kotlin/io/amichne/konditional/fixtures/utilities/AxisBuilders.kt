package io.amichne.konditional.fixtures.utilities

import io.amichne.konditional.context.axis.AxisValues
import io.amichne.konditional.core.dsl.AxisValuesScope
import io.amichne.konditional.internal.builders.AxisValuesBuilder

/**
 * Top-level DSL function to create [AxisValues]
 *
 * Internal API: This function is used internally for testing and should not be called directly.
 *
 * Example:
 * ```kotlin
 * val values = axisValues {
 *     environment(Environment.PROD)
 *     tenant(Tenant.SME)
 * }
 * ```
 */
internal fun axisValues(block: AxisValuesScope.() -> Unit): AxisValues =
    AxisValuesBuilder().apply(block).build()
