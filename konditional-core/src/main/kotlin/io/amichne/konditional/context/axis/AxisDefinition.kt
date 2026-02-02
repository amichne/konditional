package io.amichne.konditional.context.axis

import kotlin.reflect.KClass

/**
 * Ergonomic definition wrapper for [Axis] that supports object-style declarations.
 *
 * This allows applications to define axes without inheriting from the [Axis] handle:
 * ```kotlin
 * enum class Environment(override val id: String) : AxisValue<Environment> {
 *     PROD("prod"), STAGE("stage"), DEV("dev")
 * }
 *
 * object EnvironmentAxis : AxisDefinition<Environment>(
 *     id = "environment",
 *     valueClass = Environment::class,
 * )
 * ```
 *
 * The created [axis] is registered when this definition is initialized.
 */
open class AxisDefinition<T>(
    override val id: String,
    override val valueClass: KClass<out T>,
    open override val isImplicit: Boolean = false,
    open override val autoRegister: Boolean = true,
) : Axis.Delegate<T> where T : AxisValue<T>, T : Enum<T> {
    /**
     * Axis handle derived from this definition.
     */
    val axis: Axis<T> = Axis.create(this)

    override fun toString(): String =
        "AxisDefinition(id='$id', valueClass=${valueClass.simpleName})"
}
