package io.amichne.konditional.core.registry

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.instance.Configuration
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
 * Most users interact with registries through [io.amichne.konditional.core.Namespace] instances and [io.amichne.konditional.core.features.FeatureContainer]:
 * ```kotlin
 * object Payments : Namespace("payments")
 *
 * // Define features using FeatureContainer (recommended)
 * object PaymentFeatures : FeatureContainer<Payments>(
 *     Payments
 * ) {
 *     val APPLE_PAY by boolean(default = false) {
 *         rule(true) { platforms(Platform.IOS) }
 *     }
 * }
 *
 * // Evaluate features
 * val isEnabled = PaymentFeatures.APPLE_PAY.evaluate(context)
 * ```
 *
 * ## Direct Registry Operations
 *
 * ### Loading Configuration
 * Load a complete configuration snapshot (typically from JSON):
 * ```kotlin
 * val registry = NamespaceRegistry.create()
 * registry.load(configuration)
 * ```
 *
 * ### Inspecting State
 * ```kotlin
 * val currentState = registry.configuration()
 * val specificFlag = registry.flag(MY_FLAG)
 * val allFlags = registry.allFlags()
 * ```
 *
 * ### Testing with Isolated Registries
 * ```kotlin
 * @Test
 * fun `test feature behavior`() {
 *     val testRegistry = NamespaceRegistry.create()
 *     testRegistry.load(testConfig)
 *
 *     withRegistry(testRegistry) {
 *         val value = contextFn.evaluate(MY_FLAG)
 *         assertEquals(expected, value)
 *     }
 * }
 * ```
 *
 * ## Implementation Details
 *
 * The primary implementation is [InMemoryNamespaceRegistry], which provides a thread-safe
 * registry backed by [AtomicReference].
 *
 * **Note**: Configuration updates are handled internally by [io.amichne.konditional.core.features.FeatureContainer].
 * Users should not need to manually update individual flags when using the delegation API.
 *
 * @see InMemoryNamespaceRegistry
 * @see io.amichne.konditional.core.Namespace
 * @see io.amichne.konditional.core.features.FeatureContainer
 * @see Configuration
 */
interface NamespaceRegistry {
    /**
     * Loads a complete flag configuration from the provided snapshot.
     *
     * This operation replaces the entire current configuration atomically.
     *
     * @param config The [Configuration] containing the flag configuration to load
     */
    fun load(config: Configuration)

    /**
     * Retrieves the current snapshot of all flag configurations.
     *
     * This provides a consistent view of all flags at a point in time.
     * The returned snapshot is immutable and can be safely shared.
     *
     * @return The current [Configuration]
     */
    val configuration: Configuration

    /**
     * Retrieves a specific flag definition from the registry.
     *
     * @param key The [Feature] key for the flag
     * @return The [io.amichne.konditional.core.FlagDefinition] which is known to exist via structural guarantee
     * @param S The EncodableValue type wrapping the actual value
     * @param T The actual value type
     * @param C The type of the contextFn used for evaluation
     * @param M The namespace the feature belongs to
     */
    @Suppress("UNCHECKED_CAST")
    fun <S : EncodableValue<T>, T : Any, C : Context, M : Namespace> flag(
        key: Feature<S, T, C, M>,
    ): FlagDefinition<S, T, C, M> =
        configuration.flags[key] as FlagDefinition<S, T, C, M>

    /**
     * Retrieves all flags from the registry.
     *
     * @return Map of all [Feature] keys to their [FlagDefinition] definitions
     */
    fun allFlags(): Map<Feature<*, *, *, *>, FlagDefinition<*, *, *, *>> =
        configuration.flags

    companion object {

        /**
         * Creates a new in-memory registry instance.
         *
         * This is the primary way to create registry instances for modules.
         * Each namespace should have its own isolated registry instance.
         *
         * Example:
         * ```kotlin
         * // Namespace definition
         * data object MyTeam : Namespace("my-team") {
         *     override val registry = NamespaceRegistry()
         * }
         *
         * // Testing with isolated registry
         * @Test
         * fun `test feature flag`() {
         *     val testRegistry = NamespaceRegistry(/* config */)
         *     // Test with isolated registry
         * }
         * ```
         *
         * @return A new [InMemoryNamespaceRegistry] instance
         * @since 0.0.2
         */
        operator fun invoke(configuration: Configuration = Configuration(emptyMap())): NamespaceRegistry =
            InMemoryNamespaceRegistry().apply {
                load(configuration)
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
         * @param C The type of the contextFn used for evaluation
         */
        internal fun <S : EncodableValue<T>, T : Any, C : Context> NamespaceRegistry.updateDefinition(
            definition: FlagDefinition<S, T, C, *>,
        ) {
            when (this) {
                is InMemoryNamespaceRegistry -> updateDefinition(definition)
                is Namespace -> registry.updateDefinition(definition)
                else -> error("updateDefinition only supported on InMemoryNamespaceRegistry or Namespace")
            }
        }
    }
}
