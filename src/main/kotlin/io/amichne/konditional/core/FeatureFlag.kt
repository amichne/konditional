package io.amichne.konditional.core

import io.amichne.konditional.builders.FlagBuilder

interface FeatureFlag<E : Enum<E>> {
    val key: String

    fun withRules(build: FlagBuilder.() -> Unit)

    fun update(flag: Flag) = Flags.update(flag)
}
