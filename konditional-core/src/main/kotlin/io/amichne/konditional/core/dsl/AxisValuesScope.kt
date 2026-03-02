package io.amichne.konditional.core.dsl

import io.amichne.konditional.context.axis.Axis
import io.amichne.konditional.context.axis.AxisValue

/**
 * DSL scope for configuring axis values.
 *
 * Use [variant] as the primary API to construct
 * [io.amichne.konditional.context.axis.AxisValues] with deterministic, type-safe
 * axis selections.
 *
 * ## Usage
 *
 * Within a builder context:
 * ```kotlin
 * axisValues {
 *     variant {
 *         Axes.Environment { include(Environment.PROD) }
 *         Axes.Tenant { include(Tenant.ENTERPRISE) }
 *         maybeRegion?.let { Axes.Region { include(it) } }
 *     }
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
    @Deprecated(
        message = "Use variant { axis { include(...) } } to express axis values.",
        replaceWith = ReplaceWith("variant { axis { include(value) } }"),
        level = DeprecationLevel.ERROR,
    )
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
    @Deprecated(
        message = "Use variant { axis { include(...) } } and null-aware control flow for optional values.",
        replaceWith = ReplaceWith("value?.let { variant { axis { include(it) } } }"),
        level = DeprecationLevel.ERROR,
    )
    fun <T> setIfNotNull(
        axis: Axis<T>,
        value: T?,
    ) where T : AxisValue<T>, T : Enum<T>
}

/**
 * Unified axis-values DSL entry point.
 *
 * Example:
 * ```kotlin
 * val values = axisValues {
 *     variant {
 *         Axes.Environment { include(Environment.PROD) }
 *         Axes.Tenant { include(Tenant.ENTERPRISE) }
 *     }
 * }
 * ```
 *
 * Invariants and semantics:
 * - `include(first, ...)` enforces non-empty selections at compile time.
 * - Value insertion order is deterministic.
 * - `Axes.X { }` is invalid and throws [IllegalArgumentException].
 */
fun AxisValuesScope.variant(
    block: VariantScope.() -> Unit,
) {
    val host = this as? VariantDispatchHost
        ?: error("Unsupported AxisValuesScope receiver for variant DSL: ${this::class.qualifiedName}")
    VariantScope(host).apply(block)
}
