package io.amichne.konditional.context.axis

import io.amichne.konditional.core.registry.AxisRegistry
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
 * type-based APIs where the axis can be inferred from the value type.
 *
 * ## Usage
 *
 * Define an axis by extending this abstract class:
 * ```kotlin
 * enum class Environment(override val id: String) : AxisValue {
 *     PROD("prod"), STAGE("stage"), DEV("dev")
 * }
 *
 * object Axes {
 *     object Environment : Axis<Environment>("environment")
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
 * @param T The enum type that represents values along this axis
 * @property id A stable, unique identifier for this axis
 */
abstract class Axis<T>(
    final override val id: String,
) : AxisValue where T : AxisValue, T : Enum<T> {

    /**
     * The runtime class of the value type T.
     *
     * This is extracted via reflection on the class's supertypes to find the
     * type argument to Axis<T>.
     */
    @Suppress("UNCHECKED_CAST")
    val valueClass: KClass<out T> = run {
        // Find the Axis<T> supertype and extract T
        val axisSupertype = this::class.supertypes
            .first { type ->
                (type.classifier as? KClass<*>)?.qualifiedName ==
                    "io.amichne.konditional.context.axis.Axis"
            }

        // Extract the type argument T from Axis<T>
        val typeArgument = axisSupertype.arguments.firstOrNull()?.type
                           ?: error("Could not extract type argument from Axis<T> for ${this::class}")

        typeArgument.classifier as KClass<out T>
    }

    init {
        // Auto-register this axis upon creation
        @Suppress("LeakingThis")
        AxisRegistry.register(this)
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
}
