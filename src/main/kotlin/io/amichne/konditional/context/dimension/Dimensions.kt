package io.amichne.konditional.context.dimension

/**
 * Strongly-typed wrapper for a set of dimension values.
 *
 * Implements type-safe access to dimension values by their axis descriptors. Under the hood,
 * it stores a map of axis IDs to dimension keys. This class is immutable and intended to be
 * constructed via builders.
 */
class Dimensions internal constructor(
    private val values: Map<String, DimensionKey>,
) {
    /**
     * Low-level access by axis id (used internally by rules).
     */
    internal operator fun get(axisId: String): DimensionKey? = values[axisId]

    /**
     * Strongly-typed access by dimension descriptor.
     */
    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(axis: Dimension<T>): T? where T : DimensionKey, T : Enum<T> =
        values[axis.id] as? T

    companion object {
        val EMPTY = Dimensions(emptyMap())
    }
}
