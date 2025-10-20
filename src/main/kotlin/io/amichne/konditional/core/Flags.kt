package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import java.util.concurrent.atomic.AtomicReference

/**
 * Singleton object that holds flag-related functionality for the Konditional core module.
 * Use this object to manage and access global flags boundary the application.
 */
object Flags {
    /**
     * Type-safe wrapper that maintains the relationship between a Conditional and its Condition.
     * This wrapper ensures type safety by keeping the value type S and context type C paired together.
     *
     * The wrapper guarantees the invariant: a Conditional<S, C> is always stored with its
     * matching Condition<S, C>, eliminating inconsistencies that could arise from separate storage.
     */
    class FlagEntry<S : Any, C : Context>(
        val condition: Condition<S, C>,
    ) {
        /**
         * Evaluates this flag entry with the given context.
         * Type safety is guaranteed by the wrapper's construction invariant.
         */
        fun evaluate(context: C): S = condition.evaluate(context)
    }

    private val current = AtomicReference(Snapshot(emptyMap()))

    @ConsistentCopyVisibility
    data class Snapshot internal constructor(
        val flags: Map<Conditional<*, *>, FlagEntry<*, *>>,
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
            it[condition.key] = FlagEntry(condition)
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
    fun <S : Any, C : Context> C.evaluate(key: Conditional<S, C>): S =
        (current.get().flags[key] as? FlagEntry<S, C>)?.evaluate(this)
            ?: throw IllegalStateException("Flag not found: ${key.key}")

    /**
     * Evaluates all feature flags boundary the given [Context] and returns a map of each [Conditional] to its evaluated value.
     *
     * @receiver The [Context] containing the feature flags to be evaluated.
     * @return A map where each key is a [Conditional] and the value is the result of its evaluation (maybe `null`).
     */
    @Suppress("UNCHECKED_CAST")
    fun <C : Context> C.evaluate(): Map<Conditional<*, *>, Any?> = current.get().flags.mapValues { (_, entry) ->
        (entry as? FlagEntry<*, C>)?.evaluate(this)
    }
}
