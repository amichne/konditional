package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.instance.Konfig
import io.amichne.konditional.core.instance.KonfigPatch
import io.amichne.konditional.core.types.EncodableValue

/**
 * Abstraction for managing feature flag configurations and evaluations.
 *
 * This interface defines the contract for a feature flag registry that can:
 * - Load and manage flag configurations via snapshots
 * - Apply incremental updates via patches
 * - Update individual flag definitions
 * - Retrieve current state
 *
 * Implementations of this interface can provide different backing stores,
 * particularly useful when writing tests.
 *
 * By default, [io.amichne.konditional.core.RegistryScope.global] provides a thread-safe, in-memory implementation
 * accessible globally or through [io.amichne.konditional.core.RegistryScope.current] for scoped access.
 *
 * ## Core Operations
 *
 * ### Loading Configuration
 * ```kotlin
 * registry.load(konfig)
 * ```
 *
 * ### Applying Patches
 * ```kotlin
 * val patch = KonfigPatch.from(registry.konfig()) {
 *     add(MY_FLAG to newDefinition)
 *     remove(OLD_FLAG)
 * }
 * registry.update(patch)
 * ```
 *
 * ### Updating Individual Flags
 * ```kotlin
 * registry.update(flagDefinition)
 * ```
 *
 * ### Evaluating Flags
 * Use the extension functions on [Context] for evaluation:
 * ```kotlin
 * val value = MY_FLAG.evaluate(context, registry)
 * val allValues =
 * ```
 *
 * ## Implementations
 *
 * The primary implementation is [InMemoryModuleRegistry], which provides a thread-safe
 * registry backed by [java.util.concurrent.atomic.AtomicReference].
 *
 * For global access, use [RegistryScope.global] or [RegistryScope.current].
 *
 * @see InMemoryModuleRegistry
 * @see RegistryScope
 * @see Konfig
 * @see KonfigPatch
 */
interface ModuleRegistry {
    /**
     * Loads a complete flag configuration from the provided snapshot.
     *
     * This operation replaces the entire current configuration atomically.
     *
     * @param config The [Konfig] containing the flag configuration to load
     */
    fun load(config: Konfig)

    /**
     * Applies an incremental patch to the current configuration.
     *
     * This operation atomically updates only the flags specified in the patch,
     * leaving other flags unchanged. This is more efficient than loading a
     * complete snapshot when only a few flags need to be updated.
     *
     * @param patch The [KonfigPatch] to apply
     */
    fun update(patch: KonfigPatch)

    /**
     * Updates a single flag definition in the current configuration.
     *
     * This is a convenience method for updating individual flags without
     * creating a full patch or snapshot.
     *
     * @param definition The [io.amichne.konditional.core.internal.FlagDefinition] to update
     * @param S The EncodableValue type wrapping the actual value
     * @param T The actual value type
     * @param C The type of the context used for evaluation
     */
    fun <S : EncodableValue<T>, T : Any, C : Context> update(definition: FlagDefinition<S, T, C, *>)

    /**
     * Retrieves the current snapshot of all flag configurations.
     *
     * This provides a consistent view of all flags at a point in time.
     * The returned snapshot is immutable and can be safely shared.
     *
     * @return The current [Konfig]
     */
    fun konfig(): Konfig

    /**
     * Retrieves a specific flag definition from the registry.
     *
     * @param key The [Feature] key for the flag
     * @return The [FlagDefinition] if found, null otherwise
     * @param S The EncodableValue type wrapping the actual value
     * @param T The actual value type
     * @param C The type of the context used for evaluation
     * @param M The taxonomy the feature belongs to
     */
    @Suppress("UNCHECKED_CAST")
    fun <S : EncodableValue<T>, T : Any, C : Context, M : Taxonomy> featureFlag(
        key: Feature<S, T, C, M>
    ): FlagDefinition<S, T, C, M>? =
        konfig().flags[key] as? FlagDefinition<S, T, C, M>

    /**
     * Retrieves all flags from the registry.
     *
     * @return Map of all [Feature] keys to their [FlagDefinition] definitions
     */
    fun allFlags(): Map<Feature<*, *, *, *>, FlagDefinition<*, *, *, *>> =
        konfig().flags

    companion object {

        /**
         * Creates a new in-memory registry instance.
         *
         * This is the primary way to create registry instances for modules.
         * Each taxonomy should have its own isolated registry instance.
         *
         * Example:
         * ```kotlin
         * // Taxonomy definition
         * data object MyTeam : Taxonomy.Domain("my-team") {
         *     override val registry = ModuleRegistry.create()
         * }
         *
         * // Testing with isolated registry
         * @Test
         * fun `test feature flag`() {
         *     val testRegistry = ModuleRegistry.create()
         *     testRegistry.load(/* config */)
         *     // Test with isolated registry
         * }
         * ```
         *
         * @return A new [InMemoryModuleRegistry] instance
         * @since 0.0.2
         */
        fun create(): ModuleRegistry = InMemoryModuleRegistry()

        /**
         * Creates a new in-memory registry instance with an initial configuration.
         *
         * This is a convenience method for creating and loading a registry in one step.
         *
         * Example:
         * ```kotlin
         * data object MyTeam : Taxonomy.Domain("my-team") {
         *     override val registry = ModuleRegistry.create(buildSnapshot {
         *         MyFlags.FEATURE_A with { default(true) }
         *     })
         * }
         * ```
         *
         * @param initialConfig The initial [Konfig] to load into the registry
         * @return A new [InMemoryModuleRegistry] instance with the initial configuration loaded
         * @since 0.0.2
         */
        fun create(initialConfig: Konfig): ModuleRegistry = InMemoryModuleRegistry().apply {
            load(initialConfig)
        }
    }
}
