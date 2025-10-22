package io.amichne.konditional.core

import io.amichne.konditional.builders.FlagBuilder
import io.amichne.konditional.context.Context

/**
 * Represents a feature flag that can be used to enable or disable specific functionality
 * in an application based on a given state or condition.
 *
 * @param S The type of the state or value that the feature flag produces.
 * @param C The type of the context that the feature flag evaluates against.
 */
interface Conditional<S : Any, C : Context> {
    val key: String

    fun with(build: FlagBuilder<S, C>.() -> Unit)

    fun update(condition: Condition<S, C>) = Flags.update(condition)

    companion object {
        internal inline fun <reified T, S : Any, C : Context> parse(key: String): T where T : Conditional<S, C>, T : Enum<T> =
            enumValues<T>().first { it.key == key }
    }
}
