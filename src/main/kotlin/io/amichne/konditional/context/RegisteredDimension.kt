package io.amichne.konditional.context

import io.amichne.konditional.core.registry.DimensionRegistry
import kotlin.reflect.KClass

/**
 * Convenience base for axes that auto-register themselves with [DimensionRegistry].
 */
abstract class RegisteredDimension<T>(
    final override val id: String,
    final override val valueClass: KClass<out T>,
) : Dimension<T> where T : DimensionKey, T : Enum<T>  {

    init {
        @Suppress("LeakingThis")
        DimensionRegistry.register(this)
    }
}
