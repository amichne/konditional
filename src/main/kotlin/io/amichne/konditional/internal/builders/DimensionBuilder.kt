package io.amichne.konditional.internal.builders

import io.amichne.konditional.context.Dimension
import io.amichne.konditional.context.DimensionKey
import io.amichne.konditional.context.Dimensions
import io.amichne.konditional.core.dsl.DimensionScope
import io.amichne.konditional.core.dsl.KonditionalDsl
import io.amichne.konditional.core.registry.DimensionRegistry

@KonditionalDsl
internal class DimensionBuilder : DimensionScope {
    private val values = mutableMapOf<String, DimensionKey>()

    /**
     * Set a dimension value for a given axis.
     */
    override fun <T> set(
        axis: Dimension<T>,
        value: T,
    )  where T : DimensionKey, T : Enum<T> {
        values[axis.id] = value
    }

    /**
     * Optional setter – skips when [value] is null.
     */
    override fun <T> setIfNotNull(
        axis: Dimension<T>,
        value: T?,
    ) where T : DimensionKey, T : Enum<T>  {
        if (value != null) set(axis, value)
    }

    /**
     * Type-based dimension setter.
     *
     * Example:
     *   dimensionsBuilder.dimension(Environment.PROD)
     */
    inline fun <reified T> DimensionBuilder.dimension(value: T)  where T : DimensionKey, T : Enum<T> {
        val axis = DimensionRegistry.axisFor(T::class) ?: error("No Dimension registered for type ${T::class}")
        @Suppress("UNCHECKED_CAST")
        set(axis, value)
    }

    internal fun build(): Dimensions =
        if (values.isEmpty()) Dimensions.EMPTY
        else Dimensions(values.toMap())
}
