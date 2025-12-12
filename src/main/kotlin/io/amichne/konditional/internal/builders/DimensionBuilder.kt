package io.amichne.konditional.internal.builders

import io.amichne.konditional.context.Dimension
import io.amichne.konditional.context.DimensionKey
import io.amichne.konditional.context.Dimensions
import io.amichne.konditional.core.dsl.DimensionScope
import io.amichne.konditional.core.dsl.KonditionalDsl

@KonditionalDsl
internal class DimensionBuilder : DimensionScope {
    private val values = mutableMapOf<String, DimensionKey>()

    /**
     * Set a dimension value for a given axis.
     */
    override fun <T : DimensionKey> set(
        axis: Dimension<T>,
        value: T,
    ) {
        values[axis.id] = value
    }

    /**
     * Optional setter – skips when [value] is null.
     */
    override fun <T : DimensionKey> setIfNotNull(
        axis: Dimension<T>,
        value: T?,
    ) {
        if (value != null) set(axis, value)
    }

    internal fun build(): Dimensions =
        if (values.isEmpty()) Dimensions.EMPTY
        else Dimensions(values.toMap())
}
