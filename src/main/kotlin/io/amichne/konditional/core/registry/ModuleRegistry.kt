package io.amichne.konditional.core.registry

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Taxonomy
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.instance.Konfig
import io.amichne.konditional.core.types.EncodableValue
import java.util.concurrent.atomic.AtomicReference

/**
 * Abstraction for managing feature flag configurations and evaluations.
 *
 * This interface defines the contract for a feature flag registry that can:
 * - Load complete flag configurations via snapshots
 * - Retrieve current state for inspection or serialization
 * - Query individual flag definitions
 *
 * ## Primary Usage
 *
 * Most users interact with registries through [io.amichne.konditional.core.Taxonomy] instances and [io.amichne.konditional.core.features.FeatureContainer]:
 * ```kotlin
 * // Define features using FeatureContainer (recommended)
 * object PaymentFeatures : FeatureContainer<Taxonomy.Domain.Payments>(
 *     Taxonomy.Domain.Payments
 * ) {
 *     val APPLE_PAY by boolean {
 *         default(false)
 *         rule { platforms(Platform.IOS) } implies true
 *     }
 * }
 *
 * // Evaluate features
 * val isEnabled = context.evaluate(PaymentFeatures.APPLE_PAY)
 * ```
 *
 * ## Direct Registry Operations
 *
 * ### Loading Configuration
 * Load a complete configuration snapshot (typically from JSON):
 * ```kotlin
 * val registry = ModuleRegistry.create()
 * registry.load(konfig)
 * ```
 *
 * ### Inspecting State
 * ```kotlin
 * val currentState = registry.konfig()
 * val specificFlag = registry.featureFlag(MY_FLAG)
 * val allFlags = registry.allFlags()
 * ```
 *
 * ### Testing with Isolated Registries
 * ```kotlin
 * @Test
 * fun `test feature behavior`() {
 *     val testRegistry = ModuleRegistry.create()
 *     testRegistry.load(testConfig)
 *
 *     withRegistry(testRegistry) {
 *         val value = context.evaluate(MY_FLAG)
 *         assertEquals(expected, value)
 *     }
 * }
 * ```
 *
 * ## Implementation Details
 *
 * The primary implementation is [InMemoryModuleRegistry], which provides a thread-safe
 * registry backed by [AtomicReference].
 *
 * **Note**: Configuration updates are handled internally by [io.amichne.konditional.core.features.FeatureContainer].
 * Users should not need to manually update individual flags when using the delegation API.
 *
 * @see InMemoryModuleRegistry
 * @see io.amichne.konditional.core.Taxonomy
 * @see io.amichne.konditional.core.features.FeatureContainer
 * @see Konfig
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
     * Retrieves the current snapshot of all flag configurations.
     *
     * This provides a consistent view of all flags at a point in time.
     * The returned snapshot is immutable and can be safely shared.
     *
     * @return The current [Konfig]
     */
    @Deprecated("Use val accessor", replaceWith = ReplaceWith("konfig"))
    fun konfig(): Konfig

    val konfig: Konfig

    /**
     * Retrieves a specific flag definition from the registry.
     *
     * @param key The [Feature] key for the flag
     * @return The [io.amichne.konditional.core.FlagDefinition] if found, null otherwise
     * @param S The EncodableValue type wrapping the actual value
     * @param T The actual value type
     * @param C The type of the context used for evaluation
     * @param M The taxonomy the feature belongs to
     */
    @Suppress("UNCHECKED_CAST")
    fun <S : EncodableValue<T>, T : Any, C : Context, M : Taxonomy> featureFlag(
        key: Feature<S, T, C, M>
    ): FlagDefinition<S, T, C, M>? =
        konfig.flags[key] as? FlagDefinition<S, T, C, M>

    /**
     * Retrieves all flags from the registry.
     *
     * @return Map of all [Feature] keys to their [FlagDefinition] definitions
     */
    fun allFlags(): Map<Feature<*, *, *, *>, FlagDefinition<*, *, *, *>> =
        konfig.flags

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
         *     override val registry = ModuleRegistry()
         * }
         *
         * // Testing with isolated registry
         * @Test
         * fun `test feature flag`() {
         *     val testRegistry = ModuleRegistry(/* config */)
         *     // Test with isolated registry
         * }
         * ```
         *
         * @return A new [InMemoryModuleRegistry] instance
         * @since 0.0.2
         */
        operator fun invoke(konfig: Konfig = Konfig(emptyMap())): ModuleRegistry = InMemoryModuleRegistry().apply {
            load(konfig)
        }

        /**
         * Updates a single flag definition in the current configuration.
         *
         * This is a convenience method for updating individual flags without
         * creating a full patch or snapshot.
         *
         * **Internal API**: This method is used internally by FeatureContainer and should not be
         * called directly. Configuration updates are handled automatically through delegation.
         *
         * @param definition The [FlagDefinition] to update
         * @param S The EncodableValue type wrapping the actual value
         * @param T The actual value type
         * @param C The type of the context used for evaluation
         */
        internal fun <S : EncodableValue<T>, T : Any, C : Context> ModuleRegistry.updateDefinition(
            definition: FlagDefinition<S, T, C, *>
        ) {
            when (this) {
                is InMemoryModuleRegistry -> updateDefinition(definition)
                is Taxonomy -> registry.updateDefinition(definition)
                else -> error("updateDefinition only supported on InMemoryModuleRegistry or Taxonomy")
            }
        }
    }
}
