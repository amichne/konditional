package io.amichne.konditional.core

import io.amichne.konditional.builders.FlagBuilder

interface FeatureFlag<T : Flaggable<S>, S : Any> {
    val key: String

    fun withRules(build: FlagBuilder<S, T>.() -> Unit)

    fun update(flag: Flag<T, S>) = Flags.update(flag)
}
