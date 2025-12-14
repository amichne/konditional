package io.amichne.konditional.core.registry

import io.amichne.konditional.context.dimension.Dimension
import io.amichne.konditional.context.dimension.DimensionKey
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * Internal registry that maps a [DimensionKey] type to its corresponding [Dimension].
 *
 * This powers the reified `dimension<T>()` accessors.
 */
@PublishedApi
internal object DimensionRegistry {

    @PublishedApi
    internal val byType: MutableMap<KClass<out DimensionKey>, Dimension<*>> =
        ConcurrentHashMap()

    inline fun <reified T> register(
        id: String,
    ): Unit where T : DimensionKey, T : Enum<T> = object : Dimension<T> {
        override val id: String = id
        override val valueClass: KClass<out T> = T::class
    }.let { byType.putIfAbsent(it.valueClass, it) }

//    fun <T> register(axis: Dimension<T>) where T : DimensionKey, T : Enum<T> {
//    val existing = byType.putIfAbsent(axis.valueClass, axis)
//    require(existing == null || existing === axis) {
//        "Dimension already registered for type ${axis.valueClass}: " +
//        "existing=$existing, new=$axis"
//    }
//}
//    }

    @Suppress("UNCHECKED_CAST")
    fun <T> axisFor(type: KClass<T>): Dimension<T>? where T : DimensionKey, T : Enum<T> =
        byType[type] as? Dimension<T>
}
