package io.amichne.konditional.core.internal

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.ModuleRegistry
import io.amichne.konditional.core.instance.Konfig
import io.amichne.konditional.core.instance.KonfigPatch
import java.util.concurrent.atomic.AtomicReference

/**
 * Default singleton implementation of [io.amichne.konditional.core.ModuleRegistry] for the Konditional core taxonomy.
 *
 * This object provides a thread-safe, in-memory registry for managing feature flags.
 * It uses [java.util.concurrent.atomic.AtomicReference] to ensure atomic updates and lock-free reads.
 *
 * ## Thread Safety
 *
 * All operations on this registry are atomic and thread-safe. Multiple threads can
 * safely read and update flags concurrently.
 *
 * @see io.amichne.konditional.core.ModuleRegistry
 */
internal object SingletonModuleRegistry : ModuleRegistry {
    private val current = AtomicReference(Konfig(emptyMap()))

    /**
     * Loads the flag values from the provided [config] snapshot.
     *
     * This operation atomically replaces the entire current configuration.
     *
     * @param config The [Konfig] containing the configuration to load
     */
    override fun load(config: Konfig) {
        current.set(config)
    }

    /**
     * Applies a [io.amichne.konditional.core.instance.KonfigPatch] to the current snapshot, atomically updating the flag configuration.
     *
     * This method is useful for incremental updates without replacing the entire snapshot.
     * The update is performed atomically using compare-and-swap semantics.
     *
     * @param patch The [io.amichne.konditional.core.instance.KonfigPatch] to apply
     */
    override fun update(patch: KonfigPatch) {
        current.updateAndGet { currentSnapshot ->
            patch.applyTo(currentSnapshot)
        }
    }

    /**
     * Returns the current snapshot of all flag configurations.
     *
     * @return The current [Konfig]
     */
    override fun konfig(): Konfig = current.get()

    /**
     * Updates a single flag definition in the current configuration.
     *
     * This operation atomically updates the specified flag while leaving others unchanged.
     *
     * @param definition The [io.amichne.konditional.core.internal.FlagDefinition] to update
     * @param S The type of the flag's value
     * @param C The type of the context used for evaluation
     */
    override fun <S : io.amichne.konditional.core.types.EncodableValue<T>, T : Any, C : Context> update(definition: FlagDefinition<S, T, C, *>) {
        current.updateAndGet { currentSnapshot ->
            val mutableFlags = currentSnapshot.flags.toMutableMap()
            mutableFlags[definition.feature] = definition
            Konfig(mutableFlags)
        }
    }
}
