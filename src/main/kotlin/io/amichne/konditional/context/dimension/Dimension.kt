package io.amichne.konditional.context.dimension

import kotlin.reflect.KClass

/**
 * Describes a dimension, e.g. "env", "region", "tenant".
 *
 * The type parameter T is the *consumer-defined* enum or class that
 * implements DimensionKey.
 */
interface Dimension<out T> : DimensionKey where T : Enum<out T> {
    val valueClass: KClass<out T>

    companion object {
//        inline fun <reified T> register(
//            id: String,
//        ): Dimension<T> where T : DimensionKey, T : Enum<T> = object : Dimension<T> {
//            override val id: String = id
//            override val valueClass: KClass<out T> = T::class
//        }
    }
}
