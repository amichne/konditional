package io.amichne.konditional.context.axis

import io.amichne.konditional.core.registry.AxisCatalog
import kotlin.reflect.KClass

/**
 * Describes an axis along which values can vary (e.g., "environment", "region", "tenant").
 *
 * An Axis is a descriptor for a dimension of variation in your system. It pairs with
 * an enum type T that implements [AxisValue] to define the possible values along that axis.
 *
 * ## Usage
 *
 * Define an axis using the factory in [Axis.Companion]:
 * ```kotlin
 * enum class Environment(override val id: String) : AxisValue<Environment> {
 *     PROD("prod"), STAGE("stage"), DEV("dev")
 * }
 *
 * object Axes {
 *     val environment = Axis.of<Environment>("environment")
 * }
 * ```
 *
 * To use type-inferred axis DSL operations, register the axis in an [AxisCatalog]:
 * ```kotlin
 * object Checkout : Namespace("checkout") {
 *     val environmentAxis = axis<Environment>("environment")
 * }
 *
 * // In rules (resolved through Checkout.axisCatalog)
 * axis(Environment.PROD)
 *
 * // In contexts
 * val env = context.axis<Environment>()
 * ```
 *
 * @param T The enum type that represents values along this axis.
 * @param valueClass The runtime class of the value type [T].
 *      This is intentionally passed explicitly to avoid fragile reflection-based extraction from generic supertypes.
 * @property id A stable, unique identifier for this axis
 *
 * Axis identifiers are explicit-only. Always provide stable ids that are independent of class names.
 */
class Axis<T> private constructor(
    val id: String,
    val valueClass: KClass<out T>,
) where T : AxisValue<T>, T : Enum<T> {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Axis<*>) return false
        return id == other.id && valueClass == other.valueClass
    }

    override fun hashCode(): Int {
        return 31 * id.hashCode() + valueClass.hashCode()
    }

    override fun toString(): String {
        return "Axis(id='$id', valueClass=${valueClass.simpleName})"
    }

    companion object {
        /**
         * Creates a new axis handle with a stable explicit id.
         *
         * Prefer this factory for production code. Axis ids must be explicit and stable.
         */
        fun <T> of(
            id: String,
            valueClass: KClass<out T>,
        ): Axis<T> where T : AxisValue<T>, T : Enum<T> =
            Axis(id = id, valueClass = valueClass)

        /**
         * Creates and registers a new axis handle in [axisCatalog].
         */
        fun <T> of(
            id: String,
            valueClass: KClass<out T>,
            axisCatalog: AxisCatalog,
        ): Axis<T> where T : AxisValue<T>, T : Enum<T> =
            of(id = id, valueClass = valueClass).also(axisCatalog::register)

        /**
         * Reified helper for [of] with an explicit id.
         */
        inline fun <reified T> of(id: String): Axis<T> where T : AxisValue<T>, T : Enum<T> =
            of(id = id, valueClass = T::class)

        /**
         * Reified helper for [of] with explicit [axisCatalog] registration.
         */
        inline fun <reified T> of(id: String, axisCatalog: AxisCatalog): Axis<T> where T : AxisValue<T>, T : Enum<T> =
            of(id = id, valueClass = T::class, axisCatalog = axisCatalog)
    }
}
