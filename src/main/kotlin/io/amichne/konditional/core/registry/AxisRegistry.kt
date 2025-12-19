package io.amichne.konditional.core.registry

import io.amichne.konditional.context.axis.Axis
import io.amichne.konditional.context.axis.AxisValue
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * Internal registry that maps axis IDs to their corresponding [Axis] descriptors.
 *
 * This registry enables both ID-based and type-based APIs where the axis can be inferred
 * from the value type. For example, given `Environment.PROD`, the registry can look up
 * the `Axes.Environment` axis by matching the value type.
 *
 * ## Registration
 *
 * Axes are automatically registered when they are instantiated (via their init block).
 * The registry ensures that each axis ID maps to exactly one axis.
 *
 * ## Thread Safety
 *
 * This registry is thread-safe and can be safely accessed from multiple threads.
 *
 * @see Axis
 */
@PublishedApi
internal object AxisRegistry {
    /**
     * Internal map from axis IDs to their axis descriptors.
     */
    @PublishedApi
    internal val byId: MutableMap<String, Axis<*>> =
        ConcurrentHashMap()

    /**
     * Registers an axis in the registry.
     *
     * This is called automatically by [Axis] during initialization. It ensures that
     * each axis ID maps to exactly one axis.
     *
     * @param axis The axis to register
     * @throws IllegalArgumentException if an axis for this ID is already registered with a different instance
     */
    fun <T> register(axis: Axis<T>) where T : AxisValue, T : Enum<T> {
        val existing = byId.putIfAbsent(axis.id, axis)
        require(existing == null || existing === axis) {
            "Axis already registered for id ${axis.id}: existing=$existing, attempted=$axis"
        }
    }

    /**
     * Looks up the axis for a given value type.
     *
     * This enables type-based APIs where the axis can be inferred from the value type.
     *
     * @param type The runtime class create the value type
     * @return The axis for that type, or null if not registered
     * @throws IllegalStateException if multiple axes share the same value type
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> axisFor(type: KClass<out T>): Axis<T>? where T : AxisValue, T : Enum<T> =
        byId.values
            .filter { it.valueClass == type }
            .let { matches ->
                when {
                    matches.isEmpty() -> null
                    matches.size == 1 -> matches.first() as Axis<T>
                    else -> error("Multiple axes registered for type ${type.simpleName}: ${matches.joinToString { it.id }}")
                }
            }
}
