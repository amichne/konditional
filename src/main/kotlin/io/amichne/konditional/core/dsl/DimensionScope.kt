package io.amichne.konditional.core.dsl

import io.amichne.konditional.context.Dimensions
import io.amichne.konditional.context.Dimension
import io.amichne.konditional.context.DimensionKey

/**
 * DSL scope for configuring [Dimensions].
 *
 * This interface provides type-safe methods to set [Dimension] values, allowing users to define
 * dimensions in a fluent and expressive manner within a [RuleScope] context.
 *
 * @see Dimensions, [Dimension], [DimensionKey]
 */
@KonditionalDsl
interface DimensionScope {
    /**
     * Set a dimension value for a given axis.
     */
    fun <T : DimensionKey> set(
        axis: Dimension<T>,
        value: T,
    )

    /**
     * Optional setter – skips when [value] is null.
     */
    fun <T : DimensionKey> setIfNotNull(
        axis: Dimension<T>,
        value: T?,
    )
}
