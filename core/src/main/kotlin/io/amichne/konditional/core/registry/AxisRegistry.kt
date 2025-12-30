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
 * The registry ensures that each axis ID and value type maps to exactly one axis.
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
    @PublishedApi
    internal val byValueClass: MutableMap<KClass<out Enum<*>>, Axis<*>> =
        ConcurrentHashMap()

    /**
     * Registers an axis in the registry.
     *
     * This is called automatically by [Axis] during initialization. It ensures that
     * each axis ID and value type maps to exactly one axis.
     *
     * @param axis The axis to register
     * @throws IllegalArgumentException if an axis for this ID or value type is already registered with a different instance
     */
    fun register(axis: Axis<*>) {
        @Suppress("UNCHECKED_CAST")
        val valueClass = axis.valueClass as KClass<out Enum<*>>
        val existingByType = byValueClass[valueClass]
        if (existingByType != null && existingByType !== axis) {
            if (!axis.isImplicit && existingByType.isImplicit && existingByType.id == axis.id) {
                byValueClass[valueClass] = axis
                byId[axis.id] = axis
                return
            }
            throw IllegalArgumentException(
                "Axis already registered for type ${valueClass.simpleName}: existing=$existingByType, attempted=$axis",
            )
        }
        val existingById = byId[axis.id]
        if (existingById != null && existingById !== axis) {
            if (!axis.isImplicit && existingById.isImplicit && existingById.valueClass == axis.valueClass) {
                byValueClass[valueClass] = axis
                byId[axis.id] = axis
                return
            }
            throw IllegalArgumentException("Axis already registered for id ${axis.id}: existing=$existingById, attempted=$axis")
        }
        byValueClass[valueClass] = axis
        byId[axis.id] = axis
    }

    /**
     * Looks up the axis for a given value type.
     *
     * This enables type-based APIs where the axis can be inferred from the value type.
     *
     * @param type The runtime class create the value type
     * @return The axis for that type, or null if not registered
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> axisFor(type: KClass<out T>): Axis<T>? where T : AxisValue<T>, T : Enum<T> =
        byValueClass[type] as? Axis<T>

    @PublishedApi
    internal fun <T> axisForOrRegister(type: KClass<out T>): Axis<T> where T : AxisValue<T>, T : Enum<T> =
        axisFor(type) ?: registerImplicit(type)

    private fun <T> registerImplicit(type: KClass<out T>): Axis<T> where T : AxisValue<T>, T : Enum<T> {
        // Implicit ids derive from class names; avoid this path if names can be obfuscated.
        val axisId = type.qualifiedName ?: type.simpleName
        require(!axisId.isNullOrBlank()) {
            "Cannot derive axis id for ${type.qualifiedName ?: type}"
        }
        @Suppress("UNCHECKED_CAST")
        val typedClass = type as KClass<T>
        val implicitAxis = ImplicitAxis(axisId, typedClass)
        return axisFor(typedClass) ?: implicitAxis
    }

    private class ImplicitAxis<T>(
        id: String,
        valueClass: KClass<T>,
    ) : Axis<T>(id, valueClass) where T : AxisValue<T>, T : Enum<T> {
        override val isImplicit: Boolean = true
    }
}
