package io.amichne.konditional.internal.builders

import io.amichne.konditional.context.axis.Axis
import io.amichne.konditional.context.axis.AxisValue
import io.amichne.konditional.context.axis.Axes
import io.amichne.konditional.core.dsl.AxisValuesScope
import io.amichne.konditional.core.dsl.KonditionalDsl
import io.amichne.konditional.core.dsl.VariantDispatchHost

/**
 * Internal builder implementation for constructing [Axes] instances.
 *
 * This class implements the [AxisValuesScope] interface and accumulates axis values
 * during DSL execution, then builds an immutable Axes instance.
 *
 * ## Usage
 *
 * Typically used via the top-level `axes { }` builder function:
 * ```kotlin
 * val values = axes {
 *     variant {
 *         Axes.Environment { include(Environment.PROD) }
 *         Axes.Tenant { include(Tenant.ENTERPRISE) }
 *     }
 * }
 * ```
 *
 * @see Axes
 * @see AxisValuesScope
 */
@KonditionalDsl
@PublishedApi
@Suppress("OVERRIDE_DEPRECATION")
internal class AxisValuesBuilder(
    val map: MutableMap<String, MutableSet<AxisValue<*>>> = mutableMapOf(),
) :
    AxisValuesScope,
    VariantDispatchHost,
    MutableMap<String, MutableSet<AxisValue<*>>> by map {

    /**
     * Internal accessor for extracting accumulated values.
     *
     * Used by other builders that need to access the raw map.
     */
    internal fun getValues(): Map<String, Set<AxisValue<*>>> =
        map.mapValues { (_, values) -> values.toSet() }

    /**
     * Adds a value for the given axis.
     */
    private fun <T> append(
        axis: Axis<T>,
        value: T,
    ) where T : AxisValue<T>, T : Enum<T> {
        require(axis.valueClass == value::class) {
            "Axis ${axis.id} expects ${axis.valueClass.simpleName}, got ${value::class.simpleName}"
        }
        map.getOrPut(axis.id) { linkedSetOf() }.add(value)
    }


    override fun <V> onAxisSelection(
        axis: Axis<V>,
        values: Set<V>,
    ) where V : AxisValue<V>, V : Enum<V> {
        values.forEach { append(axis, it) }
    }

    /**
     * Builds an immutable [Axes] instance from the accumulated values.
     *
     * @return Axes.EMPTY if no values were set, otherwise a new Axes instance
     */
    @PublishedApi
    internal fun build(): Axes =
        if (map.isEmpty()) {
            Axes.EMPTY
        } else {
            Axes(
                map.mapValues { (_, values) -> values.toSet() },
            )
        }
}
