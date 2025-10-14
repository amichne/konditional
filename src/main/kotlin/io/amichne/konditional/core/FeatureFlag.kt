package io.amichne.konditional.core

import io.amichne.konditional.builders.FlagBuilder

interface FeatureFlag<T : Flaggable<T>> {
    val key: String

    fun withRules(build: FlagBuilder<T>.() -> Unit)

    fun update(flag: Flag<T>) = Flags.update(flag)
}
