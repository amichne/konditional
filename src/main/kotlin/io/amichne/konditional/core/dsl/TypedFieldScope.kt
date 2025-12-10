package io.amichne.konditional.core.dsl

interface TypedFieldScope<V : Any> {
    fun default(value: V)
}
