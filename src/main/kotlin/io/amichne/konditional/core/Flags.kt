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
        val flags: Map<Conditional<*>, Condition<*>>,
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
     * Updates the current state of type [S].
     *
     * @param S the type of the state to be updated.
     * @return the updated state.
     */
    fun <S : Any> update(condition: Condition<S>) {
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
    fun <S : Any> Context.evaluate(key: Conditional<S>): S = (current.get().flags[key] as? Condition<S>)?.evaluate(this)!!

    /**
     * Evaluates all feature flags boundary the given [Context] and returns a map of each [Conditional] to its evaluated value.
     *
     * @receiver The [Context] containing the feature flags to be evaluated.
     * @return A map where each key is a [Conditional] and the value is the result of its evaluation (maybe `null`).
     */
    fun Context.evaluate(): Map<Conditional<*>, Any?> = current.get().flags.mapValues { (_, f) -> f.evaluate(this) }
}
