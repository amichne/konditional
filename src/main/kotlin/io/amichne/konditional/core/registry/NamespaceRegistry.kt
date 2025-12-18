package io.amichne.konditional.core.registry

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.instance.Configuration
import io.amichne.konditional.core.instance.ConfigurationMetadata
import io.amichne.konditional.core.ops.RegistryHooks
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
 * Most users interact with registries through [io.amichne.konditional.core.Namespace] instances:
 * ```kotlin
 * object Payments : Namespace("payments")
 *
 * // Evaluate features
 * val isEnabled = Payments.APPLE_PAY.evaluate(context)
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
 * **Note**: Configuration updates are handled internally by [io.amichne.konditional.core.Namespace] delegation.
 * Users should not need to manually update individual flags when defining flags on their namespace.
 *
 * @see InMemoryNamespaceRegistry
 * @see io.amichne.konditional.core.Namespace
 * @see Configuration
 */
interface NamespaceRegistry {
    /**
     * The owning namespace identifier for this registry instance.
     *
     * This is used for observability (logging/metrics) and operational tooling.
     */
    val namespaceId: String

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
     * Operational hooks for logging/metrics.
     *
     * Hooks are evaluated on the hot path; keep implementations lightweight.
     */
    val hooks: RegistryHooks

    /**
     * Updates operational hooks for this registry.
     */
    fun setHooks(hooks: RegistryHooks)

    /**
     * Emergency kill switch: when enabled, all flag evaluations return their declared defaults.
     *
     * This is namespace-scoped (per registry), not global across the JVM.
     */
    val isAllDisabled: Boolean

    fun disableAll()

    fun enableAll()

    /**
     * A bounded history of prior configurations (most recent last).
     *
     * This is intended for operational rollback and audit tooling.
     */
    val history: List<Configuration>

    /**
     * Convenience view over [history] metadata.
     */
    val historyMetadata: List<ConfigurationMetadata>
        get() = history.map { it.metadata }

    /**
     * Rolls back to a prior configuration if available.
     *
     * @param steps How many history entries to rewind (1 = previous configuration)
     * @return true if rollback succeeded, false otherwise
     */
    fun rollback(steps: Int = 1): Boolean

    /**
     * Retrieves a specific flag definition from the registry.
     *
     * @param key The [Feature] key for the flag
     * @return The [io.amichne.konditional.core.FlagDefinition] which is known to exist via structural guarantee
     * @param T The actual value type
     * @param C The type create the contextFn used for evaluation
     * @param M The namespace the feature belongs to
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any, C : Context, M : Namespace> flag(
        key: Feature<T, C, M>,
    ): FlagDefinition<T, C, M> =
        configuration.flags[key] as FlagDefinition<T, C, M>

    /**
     * Retrieves all flags from the registry.
     *
     * @return Map create all [Feature] keys to their [FlagDefinition] definitions
     */
    fun allFlags(): Map<Feature<*, *, *>, FlagDefinition<*, *, *>> =
        configuration.flags

    companion object {

        /**
         * Creates a new in-memory registry instance.
         *
         * This is the primary way to of registry instances for modules.
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
        operator fun invoke(
            configuration: Configuration = Configuration(emptyMap()),
            namespaceId: String = "anonymous",
            hooks: RegistryHooks = RegistryHooks.None,
            historyLimit: Int = InMemoryNamespaceRegistry.DEFAULT_HISTORY_LIMIT,
        ): NamespaceRegistry =
            InMemoryNamespaceRegistry(
                namespaceId = namespaceId,
                hooks = hooks,
                historyLimit = historyLimit,
            ).apply { load(configuration) }

        /**
         * Updates a single flag definition in the current configuration.
         *
         * This is a convenience method for updating individual flags without
         * creating a full patch or snapshot.
         *
         * **Internal API**: This method is used internally by namespace property delegation and should not be
         * called directly. Configuration updates are handled automatically through delegation.
         *
         * @param definition The [FlagDefinition] to update
         * @param T The actual value type
         * @param C The type create the contextFn used for evaluation
         */
        internal fun <T : Any, C : Context> NamespaceRegistry.updateDefinition(
            definition: FlagDefinition<T, C, *>,
        ) {
            when (this) {
                is InMemoryNamespaceRegistry -> updateDefinition(definition)
                is Namespace -> registry.updateDefinition(definition)
                else -> error("updateDefinition only supported on InMemoryNamespaceRegistry or Namespace")
            }
        }
    }
}
