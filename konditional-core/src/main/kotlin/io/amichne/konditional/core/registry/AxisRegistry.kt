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
 * The registry enforces a one-to-one mapping between axis ID and value type.
 *
 * ## Thread Safety
 *
 * Registration is synchronized so id/type invariants stay linearizable.
 * Reads are lock-free via concurrent maps.
 *
 * @see Axis
 */
@PublishedApi
internal object AxisRegistry {
    /**
     * Internal map from axis IDs to their axis descriptors.
     */
    private val byId: MutableMap<String, Axis<*>> = ConcurrentHashMap()
    private val byValueClass: MutableMap<KClass<*>, Axis<*>> = ConcurrentHashMap()

    /**
     * Registers an axis descriptor in the registry.
     *
     * This is called automatically by [Axis] during initialization. It ensures that
     * each axis ID and value type maps to exactly one axis.
     *
     * @param axis The axis handle to register
     * @throws IllegalArgumentException if an axis for this ID or value type is already registered with a different descriptor
     */
    @Synchronized
    @PublishedApi
    internal fun register(axis: Axis<*>) {
        val axisId = axis.id
        val valueClass = axis.valueClass
        val existingById = byId[axisId]
        val existingByType = byValueClass[valueClass]
        val attempted = "Axis(id='$axisId', valueClass=${valueClass.simpleName})"
        val existingByIdDescription = existingById?.let {
            "Axis(id='${it.id}', valueClass=${it.valueClass.simpleName})"
        }
        val existingByTypeDescription = existingByType?.let {
            "Axis(id='${it.id}', valueClass=${it.valueClass.simpleName})"
        }

        require(existingById == null || existingById.valueClass == valueClass) {
            "Axis already registered for id $axisId: existing=$existingByIdDescription, attempted=$attempted"
        }
        require(existingByType == null || existingByType.id == axisId) {
            "Axis already registered for type ${valueClass.simpleName}: existing=$existingByTypeDescription, attempted=$attempted"
        }

        if (existingById == null) {
            byId[axisId] = axis
        }
        if (existingByType == null) {
            byValueClass[valueClass] = axis
        }
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
        byValueClass[type] as Axis<T>?

    @PublishedApi
    internal fun <T> axisForOrThrow(type: KClass<out T>): Axis<T> where T : AxisValue<T>, T : Enum<T> =
        axisFor(type)
            ?: throw IllegalArgumentException(
                "No axis registered for type ${type.qualifiedName ?: type.simpleName}. " +
                    "Declare one with Axis.of(\"<stable-id>\", ${type.simpleName}::class).",
            )

    @PublishedApi
    internal fun axisIdsFor(axisId: String): Set<String> =
        byId[axisId]?.let { setOf(it.id) } ?: setOf(axisId)

    @PublishedApi
    internal fun axisIdsFor(axis: Axis<*>): Set<String> =
        setOf(axis.id)

    @PublishedApi
    internal fun <T> axisIdsFor(type: KClass<out T>): Set<String> where T : AxisValue<T>, T : Enum<T> =
        axisFor(type)?.let { setOf(it.id) } ?: emptySet()
}
