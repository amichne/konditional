package io.amichne.konditional.core

import io.amichne.konditional.builders.FlagBuilder

/**
 * Represents a feature flag that can be used to enable or disable specific functionality
 * in an application based on a given state or condition.
 *
 * @param S The type of the state or context that the feature flag depends on.
 */
interface FeatureFlag<S : Any> {
    val key: String

    fun withRules(build: FlagBuilder<S>.() -> Unit)

    fun update(flag: Flag<S>) = Flags.update(flag)
}
