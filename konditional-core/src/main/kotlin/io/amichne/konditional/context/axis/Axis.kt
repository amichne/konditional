package io.amichne.konditional.context.axis

import kotlin.reflect.KClass

/**
 * Describes an axis along which values can vary (e.g., "environment", "region", "tenant").
 *
 * An Axis is a descriptor for a dimension of variation in your system. It pairs with
 * an enum type T that implements [AxisValue] to define the possible values along that axis.
 *
 * ## Auto-Registration
 *
 * Axes automatically register themselves with the [AxisRegistry] upon creation. This enables
 * ID-based lookup, with type-based APIs resolved by matching the value type to a registered axis.
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
 * The axis automatically registers on initialization, allowing you to use type-based APIs:
 * ```kotlin
 * // In rules
 * axis(Environment.PROD)  // Type infers the axis
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
    autoRegister: Boolean = true,
) where T : AxisValue<T>, T : Enum<T> {
    init {
        // Auto-register this axis upon creation.
        if (autoRegister) {
            io.amichne.konditional.core.registry.AxisRegistry.register(this)
        }
    }

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
            autoRegister: Boolean = true,
        ): Axis<T> where T : AxisValue<T>, T : Enum<T> =
            Axis(id = id, valueClass = valueClass, autoRegister = autoRegister)

        /**
         * Reified helper for [of] with an explicit id.
         */
        inline fun <reified T> of(id: String): Axis<T> where T : AxisValue<T>, T : Enum<T> =
            of(id = id, valueClass = T::class)
    }
}
