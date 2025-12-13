package io.amichne.konditional.context

import kotlin.reflect.KClass

/**
 * Describes a dimension axis, e.g. "env", "region", "tenant".
 *
 * The type parameter T is the *consumer-defined* enum or class that
 * implements DimensionKey.
 */
interface Dimension<out T> where T : DimensionKey, T : Enum<out T> {
    val id: String   // axis ID, e.g. "env", "region"
    val valueClass: KClass<out T>

    companion object {
        inline operator fun <reified T> invoke(
            id: String,
        ): Dimension<T> where T : DimensionKey, T : Enum<T> = object : Dimension<T> {
            override val id: String = id
            override val valueClass: KClass<out T> = T::class
        }
    }
}
