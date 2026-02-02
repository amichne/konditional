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
 * Or define via [AxisDefinition]:
 * ```kotlin
 * object EnvironmentAxis : AxisDefinition<Environment>("environment", Environment::class)
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
 * @property isImplicit Whether this axis descriptor was derived implicitly from the value type
 *
 * If you use code shrinking/obfuscation, avoid relying on implicitly derived ids.
 * Provide explicit, stable ids that are independent of class names.
 */
class Axis<T> private constructor(
    val id: String,
    val valueClass: KClass<out T>,
    val isImplicit: Boolean = false,
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

    /**
     * Delegate interface for customizing axis creation without inheriting [Axis].
     *
     * This allows library consumers to define metadata or policies while the axis
     * handle itself remains factory-only and final.
     */
    interface Delegate<T> where T : AxisValue<T>, T : Enum<T> {
        val id: String
        val valueClass: KClass<out T>
        val isImplicit: Boolean
            get() = false
        val autoRegister: Boolean
            get() = true
    }

    companion object {
        /**
         * Creates a new axis handle with a stable explicit id.
         *
         * Prefer this factory for production code to avoid obfuscation issues.
         */
        fun <T> of(
            id: String,
            valueClass: KClass<out T>,
            autoRegister: Boolean = true,
        ): Axis<T> where T : AxisValue<T>, T : Enum<T> =
            Axis(id = id, valueClass = valueClass, isImplicit = false, autoRegister = autoRegister)

        /**
         * Reified helper for [of] with an explicit id.
         */
        inline fun <reified T> of(id: String): Axis<T> where T : AxisValue<T>, T : Enum<T> =
            of(id = id, valueClass = T::class)

        /**
         * Creates an axis handle from a delegate definition.
         */
        fun <T> create(delegate: Delegate<T>): Axis<T> where T : AxisValue<T>, T : Enum<T> =
            Axis(
                id = delegate.id,
                valueClass = delegate.valueClass,
                isImplicit = delegate.isImplicit,
                autoRegister = delegate.autoRegister,
            )

        @PublishedApi
        internal fun <T> fromRegistry(
            id: String,
            valueClass: KClass<out T>,
            isImplicit: Boolean,
        ): Axis<T> where T : AxisValue<T>, T : Enum<T> =
            Axis(id = id, valueClass = valueClass, isImplicit = isImplicit, autoRegister = false)

        @PublishedApi
        internal fun <T> implicit(
            id: String,
            valueClass: KClass<out T>,
        ): Axis<T> where T : AxisValue<T>, T : Enum<T> =
            Axis(id = id, valueClass = valueClass, isImplicit = true, autoRegister = true)
    }
}
