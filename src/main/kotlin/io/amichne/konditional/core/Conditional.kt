package io.amichne.konditional.core

import io.amichne.konditional.builders.FlagBuilder

/**
 * Represents a feature flag that can be used to enable or disable specific functionality
 * in an application based on a given state or condition.
 *
 * @param S The type of the state or context that the feature flag depends on.
 */
interface Conditional<S : Any> {
    val key: String

    fun with(build: FlagBuilder<S>.() -> Unit)

    fun update(condition: Condition<S>) = Flags.update(condition)
}
