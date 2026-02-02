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
    private val byId: MutableMap<String, AxisEntry> = ConcurrentHashMap()

    private val byValueClass: MutableMap<KClass<*>, AxisEntry> = ConcurrentHashMap()

    private val idsByValueClass: MutableMap<KClass<*>, MutableSet<String>> = ConcurrentHashMap()

    private data class AxisEntry(
        val id: String,
        val valueClass: KClass<*>,
        val isImplicit: Boolean,
    )

    /**
     * Registers an axis descriptor in the registry.
     *
     * This is called automatically by [Axis] during initialization. It ensures that
     * each axis ID and value type maps to exactly one axis.
     *
     * @param axis The axis handle to register
     * @throws IllegalArgumentException if an axis for this ID or value type is already registered with a different descriptor
     */
    @PublishedApi
    internal fun register(axis: Axis<*>) {
        val axisId = axis.id
        val valueClass = axis.valueClass
        val isImplicit = axis.isImplicit
        val entry = AxisEntry(axisId, valueClass, isImplicit)
        val entryDescription = "Axis(id='$axisId', valueClass=${valueClass.simpleName})"
        val existingById = byId[axisId]
        val existingByType = byValueClass[valueClass]
        val existingByIdDescription = existingById?.let {
            "Axis(id='${it.id}', valueClass=${it.valueClass.simpleName})"
        }
        val existingByTypeDescription = existingByType?.let {
            "Axis(id='${it.id}', valueClass=${it.valueClass.simpleName})"
        }

        require(existingById == null || existingById.valueClass == valueClass) {
            "Axis already registered for id $axisId: " +
                "existing=$existingByIdDescription, attempted=$entryDescription"
        }

        if (existingByType != null) {
            val replacesImplicit = !isImplicit && existingByType.isImplicit
            val defersToExplicit = isImplicit && !existingByType.isImplicit

            require(replacesImplicit || defersToExplicit) {
                "Axis already registered for type ${valueClass.simpleName}: " +
                    "existing=$existingByTypeDescription, attempted=$entryDescription"
            }

            if (replacesImplicit) {
                byValueClass[valueClass] = entry
                byId[axisId] = entry
                rememberAxisId(valueClass, axisId)
                rememberAxisId(valueClass, existingByType.id)
            } else {
                byId.putIfAbsent(axisId, entry)
                rememberAxisId(valueClass, axisId)
            }
        } else if (existingById != null) {
            val replacesImplicit = !isImplicit && existingById.isImplicit
            val defersToExplicit = isImplicit && !existingById.isImplicit

            require(replacesImplicit || defersToExplicit) {
                "Axis already registered for id $axisId: " +
                    "existing=$existingByIdDescription, attempted=$entryDescription"
            }

            if (replacesImplicit) {
                byValueClass[valueClass] = entry
                byId[axisId] = entry
                rememberAxisId(valueClass, axisId)
                rememberAxisId(valueClass, existingById.id)
            } else {
                rememberAxisId(valueClass, axisId)
            }
        } else {
            byValueClass[valueClass] = entry
            byId[axisId] = entry
            rememberAxisId(valueClass, axisId)
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
    fun <T> axisFor(type: KClass<out T>): Axis<T>? where T : AxisValue<T>, T : Enum<T> =
        byValueClass[type]?.let { entry ->
            Axis.fromRegistry(entry.id, type, entry.isImplicit)
        }

    @PublishedApi
    internal fun <T> axisForOrRegister(type: KClass<out T>): Axis<T> where T : AxisValue<T>, T : Enum<T> =
        axisFor(type) ?: registerImplicit(type)

    @PublishedApi
    internal fun axisIdsFor(axisId: String): Set<String> =
        byId[axisId]?.let { axisIdsForValueClass(it.valueClass) }
            ?.takeIf { it.isNotEmpty() } ?: setOf(axisId)

    @PublishedApi
    internal fun axisIdsFor(axis: Axis<*>): Set<String> =
        axisIdsForValueClass(axis.valueClass)
            .takeIf { it.isNotEmpty() } ?: setOf(axis.id)

    @PublishedApi
    internal fun <T> axisIdsFor(type: KClass<out T>): Set<String> where T : AxisValue<T>, T : Enum<T> =
        axisIdsForValueClass(type).takeIf { it.isNotEmpty() } ?: emptySet()

    private fun <T> implicitAxisId(type: KClass<out T>): String where T : AxisValue<T>, T : Enum<T> =
        (type.qualifiedName ?: type.simpleName)
            ?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Cannot derive axis id for ${type.qualifiedName ?: type}")

    private fun <T> registerImplicit(type: KClass<out T>): Axis<T> where T : AxisValue<T>, T : Enum<T> =
        implicitAxisId(type).let { axisId ->
            Axis.implicit(axisId, type)
        }

    private fun axisIdsForValueClass(valueClass: KClass<*>): Set<String> =
        idsByValueClass[valueClass]?.toSet() ?: emptySet()

    private fun rememberAxisId(
        valueClass: KClass<*>,
        axisId: String,
    ) {
        val ids = idsByValueClass.computeIfAbsent(valueClass) { ConcurrentHashMap.newKeySet() }
        ids.add(axisId)
    }
}
