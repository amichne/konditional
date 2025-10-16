package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import java.util.concurrent.atomic.AtomicReference

/**
 * Singleton object that holds flag-related functionality for the Konditional core module.
 * Use this object to manage and access global flags within the application.
 */
object Flags {
    private val current = AtomicReference(Snapshot(emptyMap()))

    @ConsistentCopyVisibility
    data class Snapshot internal constructor(val flags: Map<FeatureFlag<*>, Flag<*>>)

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
    fun <S : Any> update(
        flag: Flag<S>,
    ) {
        current.get().flags.toMutableMap().let {
            it[flag.key] = flag
            current.set(Snapshot(it))
        }
    }

    @Suppress("UNCHECKED_CAST")
    /**
     * Evaluates the value of the specified [FeatureFlag] within this [Context].
     *
     * @param key The feature flag to evaluate.
     * @return The evaluated value of type [S] associated with the feature flag.
     */
    fun <S : Any> Context.evaluate(key: FeatureFlag<S>): S =
        (current.get().flags[key] as? Flag<S>)?.evaluate(this)!!

    /**
     * Evaluates all feature flags within the given [Context] and returns a map of each [FeatureFlag] to its evaluated value.
     *
     * @receiver The [Context] containing the feature flags to be evaluated.
     * @return A map where each key is a [FeatureFlag] and the value is the result of its evaluation (maybe `null`).
     */
    fun Context.evaluate(): Map<FeatureFlag<*>, Any?> =
        current.get().flags.mapValues { (_, f) -> f.evaluate(this) }
}
