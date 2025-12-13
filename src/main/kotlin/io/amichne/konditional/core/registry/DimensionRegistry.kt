package io.amichne.konditional.core.registry

import io.amichne.konditional.context.Dimension
import io.amichne.konditional.context.DimensionKey
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * Internal registry that maps a [io.amichne.konditional.context.DimensionKey] type to its corresponding [io.amichne.konditional.context.Dimension].
 *
 * This powers the reified `dimension<T>()` accessors.
 */
@PublishedApi
internal object DimensionRegistry {

    private val byType: MutableMap<KClass<out DimensionKey>, Dimension<*>> =
        ConcurrentHashMap()

    fun <T> register(axis: Dimension<T>) where T : DimensionKey, T : Enum<T> {
        val existing = byType.putIfAbsent(axis.valueClass, axis)
        require(existing == null || existing === axis) {
            "Dimension already registered for type ${axis.valueClass}: " +
            "existing=$existing, new=$axis"
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> axisFor(type: KClass<T>): Dimension<T>? where T : DimensionKey, T : Enum<T> =
        byType[type] as? Dimension<T>
}
