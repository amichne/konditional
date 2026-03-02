package io.amichne.konditional.core.dsl

import io.amichne.konditional.context.axis.Axis
import io.amichne.konditional.context.axis.AxisValue

/**
 * DSL scope for configuring axis values.
 *
 * Use the [axis] extension function to construct
 * [io.amichne.konditional.context.axis.Axes] with deterministic, type-safe
 * axis selections.
 *
 * ## Usage
 *
 * Within a builder context:
 * ```kotlin
 * axes {
 *     axis(Environment.PROD)
 *     axis(Tenant.ENTERPRISE)
 *     maybeRegion?.let { axis(it) }
 * }
 * ```
 *
 * @see io.amichne.konditional.context.axis.Axes
 * @see Axis
 * @see AxisValue
 */
@KonditionalDsl
interface AxisValuesScope
