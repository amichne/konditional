package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.snapshot.Snapshot
import io.amichne.konditional.core.snapshot.SnapshotPatch
import java.util.concurrent.atomic.AtomicReference

/**
 * Default singleton implementation of [FlagRegistry] for the Konditional core module.
 *
 * This object provides a thread-safe, in-memory registry for managing feature flags.
 * It uses [AtomicReference] to ensure atomic updates and lock-free reads.
 *
 * ## Usage
 *
 * ### Loading Configuration
 * ```kotlin
 * ConfigBuilder.config {
 *     MY_FLAG with { default(true) }
 * }
 * ```
 *
 * ### Evaluating SingletonFlagRegistry
 * ```kotlin
 * val value = context.evaluate(MY_FLAG) // Uses SingletonFlagRegistry by default
 * ```
 *
 * ### Custom Registry
 * ```kotlin
 * val customRegistry: FlagRegistry = MyCustomRegistry()
 * val value = context.evaluate(MY_FLAG, customRegistry)
 * ```
 *
 * ## Thread Safety
 *
 * All operations on this registry are atomic and thread-safe. Multiple threads can
 * safely read and update flags concurrently.
 *
 * @see FlagRegistry
 */
object SingletonFlagRegistry : FlagRegistry {
    private val current = AtomicReference(Snapshot(emptyMap()))

    /**
     * Loads the flag values from the provided [config] snapshot.
     *
     * This operation atomically replaces the entire current configuration.
     *
     * @param config The [Snapshot] containing the configuration to load
     */
    override fun load(config: Snapshot) {
        current.set(config)
    }

    /**
     * Applies a [SnapshotPatch] to the current snapshot, atomically updating the flag configuration.
     *
     * This method is useful for incremental updates without replacing the entire snapshot.
     * The update is performed atomically using compare-and-swap semantics.
     *
     * @param patch The [SnapshotPatch] to apply
     */
    override fun applyPatch(patch: SnapshotPatch) {
        current.updateAndGet { currentSnapshot ->
            patch.applyTo(currentSnapshot)
        }
    }

    /**
     * Returns the current snapshot of all flag configurations.
     *
     * @return The current [Snapshot]
     */
    override fun getCurrentSnapshot(): Snapshot = current.get()

    /**
     * Updates a single flag definition in the current configuration.
     *
     * This operation atomically updates the specified flag while leaving others unchanged.
     *
     * @param definition The [FlagDefinition] to update
     * @param S The type of the flag's value
     * @param C The type of the context used for evaluation
     */
    override fun <S : Any, C : Context> update(definition: FlagDefinition<S, C>) {
        current.updateAndGet { currentSnapshot ->
            val mutableFlags = currentSnapshot.flags.toMutableMap()
            mutableFlags[definition.conditional] = definition
            Snapshot(mutableFlags)
        }
    }
}
