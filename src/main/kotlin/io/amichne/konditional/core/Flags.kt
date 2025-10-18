package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import java.util.concurrent.atomic.AtomicReference

/**
 * Singleton object that holds flag-related functionality for the Konditional core module.
 * Use this object to manage and access global flags boundary the application.
 */
object Flags {
    private val current = AtomicReference(Snapshot(emptyMap()))

    @ConsistentCopyVisibility
    data class Snapshot internal constructor(
        val flags: Map<Conditional<*, *>, Condition<*, *>>,
    )

    /**
     * Loads the flag values from the provided [config] snapshot.
     *
     * @param config The [Snapshot] containing the configuration to load.
     */
    fun load(config: Snapshot) {
        current.set(config)
    }

    /**
     * Updates the current state of type [S] with context type [C].
     *
     * @param S the type of the state to be updated.
     * @param C the type of the context.
     * @return the updated state.
     */
    fun <S : Any, C : Context> update(condition: Condition<S, C>) {
        current.get().flags.toMutableMap().let {
            it[condition.key] = condition
            current.set(Snapshot(it))
        }
    }

    /**
     * Evaluates the value of the specified [Conditional] boundary this [Context].
     *
     * @param key The feature flag to evaluate.
     * @return The evaluated value of type [S] associated with the feature flag.
     */
    @Suppress("UNCHECKED_CAST")
    fun <S : Any, C : Context> C.evaluate(key: Conditional<S, C>): S = (current.get().flags[key] as? Condition<S, C>)?.evaluate(this)!!

    /**
     * Evaluates all feature flags boundary the given [Context] and returns a map of each [Conditional] to its evaluated value.
     *
     * @receiver The [Context] containing the feature flags to be evaluated.
     * @return A map where each key is a [Conditional] and the value is the result of its evaluation (maybe `null`).
     */
    fun <C : Context> C.evaluate(): Map<Conditional<*, *>, Any?> = current.get().flags.mapValues { (_, f) -> (f as Condition<*, C>).evaluate(this) }
}
