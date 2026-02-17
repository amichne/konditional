package io.amichne.konditional.core.registry

import io.amichne.konditional.context.axis.Axis
import io.amichne.konditional.context.axis.AxisValue
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * Scoped registry that maps axis IDs and value classes to [Axis] descriptors.
 *
 * This catalog is explicitly owned by a caller (for example, a namespace) and is never process-global.
 * The catalog enforces a one-to-one mapping between axis id and value class within its own scope.
 *
 * ## Thread safety
 *
 * Registrations are synchronized to keep id/type invariants linearizable.
 * Reads are lock-free through concurrent maps.
 */
class AxisCatalog {
    private val byId: MutableMap<String, Axis<*>> = ConcurrentHashMap()
    private val byValueClass: MutableMap<KClass<*>, Axis<*>> = ConcurrentHashMap()

    /**
     * Registers [axis] in this catalog.
     *
     * @throws IllegalArgumentException when [axis] conflicts by id or value type within this catalog.
     */
    @Synchronized
    fun register(axis: Axis<*>) {
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
            "Axis already registered for type ${valueClass.simpleName}: " +
                "existing=$existingByTypeDescription, attempted=$attempted"
        }

        if (existingById == null) {
            byId[axisId] = axis
        }
        if (existingByType == null) {
            byValueClass[valueClass] = axis
        }
    }

    /**
     * Returns the axis registered for [type], or null when not present.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> axisFor(type: KClass<out T>): Axis<T>? where T : AxisValue<T>, T : Enum<T> =
        byValueClass[type] as Axis<T>?

    /**
     * Returns the axis registered for [type], or throws when absent.
     */
    fun <T> axisForOrThrow(type: KClass<out T>): Axis<T> where T : AxisValue<T>, T : Enum<T> =
        axisFor(type)
            ?: throw IllegalArgumentException(
                "No axis registered for type ${type.qualifiedName ?: type.simpleName}. " +
                    "Declare one with Axis.of(\"<stable-id>\", ${type.simpleName}::class, axisCatalog).",
            )
}
