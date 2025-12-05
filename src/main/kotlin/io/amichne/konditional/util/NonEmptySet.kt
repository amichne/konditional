package io.amichne.konditional.util

import java.util.function.IntFunction

@JvmInline
value class NonEmptySet<T : Any>(val set: Set<T>) : Set<T> by set {
    @Deprecated("Use the standard toArray() method", ReplaceWith("toArray(generator)"))
    override fun <T : Any> toArray(generator: IntFunction<Array<out T>>): Array<out T> {
        return toArray(generator)
    }

    init {
        require(set.isNotEmpty()) { "NonEmptySet cannot be empty" }
    }

    companion object {
        fun <T : Any> nonEmptySetOf(vararg elements: T): NonEmptySet<T> = NonEmptySet(elements.toSet())
    }
}
