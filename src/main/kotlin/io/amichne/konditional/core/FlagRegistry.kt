package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.snapshot.Snapshot
import io.amichne.konditional.core.snapshot.SnapshotPatch

/**
 * Abstraction for managing feature flag configurations and evaluations.
 *
 * This interface defines the contract for a feature flag registry that can:
 * - Load and manage flag configurations via snapshots
 * - Apply incremental updates via patches
 * - Update individual flag definitions
 * - Retrieve current state
 *
 * Implementations of this interface can provide different backing stores
 * (in-memory, persistent, distributed, etc.) while maintaining the same API.
 *
 * ## Core Operations
 *
 * ### Loading Configuration
 * ```kotlin
 * val snapshot = ConfigBuilder.buildSnapshot {
 *     MY_FLAG with { default(true) }
 * }
 * registry.load(snapshot)
 * ```
 *
 * ### Applying Patches
 * ```kotlin
 * val patch = SnapshotPatch.from(registry.getCurrentSnapshot()) {
 *     add(MY_FLAG to newDefinition)
 *     remove(OLD_FLAG)
 * }
 * registry.applyPatch(patch)
 * ```
 *
 * ### Updating Individual SingletonFlagRegistry
 * ```kotlin
 * registry.update(flagDefinition)
 * ```
 *
 * ### Evaluating SingletonFlagRegistry
 * Use the extension functions on [Context] for evaluation:
 * ```kotlin
 * val value = context.evaluate(MY_FLAG, registry)
 * val allValues = context.evaluate(registry)
 * ```
 *
 * ## Implementations
 *
 * The primary implementation is [SingletonFlagRegistry], which provides a thread-safe,
 * singleton registry backed by [java.util.concurrent.atomic.AtomicReference].
 *
 * @see SingletonFlagRegistry
 * @see Snapshot
 * @see SnapshotPatch
 */
interface FlagRegistry {
    /**
     * Loads a complete flag configuration from the provided snapshot.
     *
     * This operation replaces the entire current configuration atomically.
     *
     * @param config The [Snapshot] containing the flag configuration to load
     */
    fun load(config: Snapshot)

    /**
     * Applies an incremental patch to the current configuration.
     *
     * This operation atomically updates only the flags specified in the patch,
     * leaving other flags unchanged. This is more efficient than loading a
     * complete snapshot when only a few flags need to be updated.
     *
     * @param patch The [SnapshotPatch] to apply
     */
    fun applyPatch(patch: SnapshotPatch)

    /**
     * Updates a single flag definition in the current configuration.
     *
     * This is a convenience method for updating individual flags without
     * creating a full patch or snapshot.
     *
     * @param definition The [FlagDefinition] to update
     * @param S The type of the flag's value
     * @param C The type of the context used for evaluation
     */
    fun <S : Any, C : Context> update(definition: FlagDefinition<S, C>)

    /**
     * Retrieves the current snapshot of all flag configurations.
     *
     * This provides a consistent view of all flags at a point in time.
     * The returned snapshot is immutable and can be safely shared.
     *
     * @return The current [Snapshot]
     */
    fun getCurrentSnapshot(): Snapshot

    /**
     * Retrieves a specific flag definition from the registry.
     *
     * @param key The [Conditional] key for the flag
     * @return The [ContextualFeatureFlag] if found, null otherwise
     * @param S The type of the flag's value
     * @param C The type of the context used for evaluation
     */
    @Suppress("UNCHECKED_CAST")
    fun <S : Any, C : Context> getFlag(key: Conditional<S, C>): ContextualFeatureFlag<S, C>? =
        getCurrentSnapshot().flags[key] as? ContextualFeatureFlag<S, C>

    /**
     * Retrieves all flags from the registry.
     *
     * @return Map of all [Conditional] keys to their [ContextualFeatureFlag] definitions
     */
    fun getAllFlags(): Map<Conditional<*, *>, ContextualFeatureFlag<*, *>> =
        getCurrentSnapshot().flags
}

/**
 * Evaluates a specific feature flag in the context of this [Context].
 *
 * This extension function provides convenient access to flag evaluation
 * for any [FlagRegistry] implementation.
 *
 * @param key The feature flag to evaluate
 * @param registry The [FlagRegistry] to use (defaults to [SingletonFlagRegistry])
 * @return The evaluated value of type [S]
 * @throws IllegalStateException if the flag is not found in the registry
 * @param S The type of the flag's value
 * @param C The type of the context
 */
@Suppress("UNCHECKED_CAST")
fun <S : Any, C : Context> C.evaluate(
    key: Conditional<S, C>,
    registry: FlagRegistry = SingletonFlagRegistry
): S {
    val flag = registry.getFlag(key)
        ?: throw IllegalStateException("Flag not found: ${key.key}")
    return flag.evaluate(this)
}

/**
 * Evaluates all feature flags in the context of this [Context].
 *
 * This extension function evaluates every flag in the registry and returns
 * a map of the results. SingletonFlagRegistry that don't match the context type will have
 * null values in the resulting map.
 *
 * @param registry The [FlagRegistry] to use (defaults to [SingletonFlagRegistry])
 * @return A map where each key is a [Conditional] and the value is the result
 *         of its evaluation (may be null if the flag doesn't match the context type)
 * @param C The type of the context
 */
@Suppress("UNCHECKED_CAST")
fun <C : Context> C.evaluate(registry: FlagRegistry = SingletonFlagRegistry): Map<Conditional<*, *>, Any?> =
    registry.getAllFlags().mapValues { (_, flag) ->
        (flag as? ContextualFeatureFlag<*, C>)?.evaluate(this)
    }
