package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.instance.Konfig
import io.amichne.konditional.core.instance.KonfigPatch
import io.amichne.konditional.core.types.EncodableValue
import java.util.concurrent.atomic.AtomicReference

/**
 * In-memory implementation of [ModuleRegistry] that can be instantiated for testing.
 *
 * Unlike the singleton [io.amichne.konditional.core.internal.SingletonModuleRegistry],
 * this class can be instantiated multiple times, making it ideal for:
 * - Unit tests that need isolated flag configurations
 * - Integration tests that run in parallel
 * - Scenarios where multiple independent registries are needed
 *
 * Example usage in tests:
 * ```kotlin
 * @Test
 * fun `test feature flag behavior`() {
 *     val testRegistry = InMemoryModuleRegistry()
 *
 *     config(testRegistry) {
 *         MyFlags.FEATURE_A with {
 *             default(true)
 *         }
 *     }
 *
 *     val value = testRegistry.featureFlag(MyFlags.FEATURE_A)
 *     assertEquals(true, value?.defaultValue)
 * }
 * ```
 *
 * ## Thread Safety
 *
 * This implementation is thread-safe and uses [AtomicReference] for lock-free reads
 * and compare-and-swap updates.
 *
 * @constructor Creates a new empty in-memory registry
 * @since 0.0.2
 */
class InMemoryModuleRegistry : ModuleRegistry {
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
     * Applies a [KonfigPatch] to the current snapshot, atomically updating the flag configuration.
     *
     * This method is useful for incremental updates without replacing the entire snapshot.
     * The update is performed atomically using compare-and-swap semantics.
     *
     * @param patch The [KonfigPatch] to apply
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
     * @param definition The [FlagDefinition] to update
     * @param S The type of the flag's value
     * @param C The type of the context used for evaluation
     */
    override fun <S : EncodableValue<T>, T : Any, C : Context> update(definition: FlagDefinition<S, T, C, *>) {
        current.updateAndGet { currentSnapshot ->
            val mutableFlags = currentSnapshot.flags.toMutableMap()
            mutableFlags[definition.feature] = definition
            Konfig(mutableFlags)
        }
    }
}
