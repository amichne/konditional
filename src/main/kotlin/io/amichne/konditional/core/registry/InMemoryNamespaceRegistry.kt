package io.amichne.konditional.core.registry

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.instance.Configuration
import io.amichne.konditional.core.instance.ConfigurationPatch
import io.amichne.konditional.core.types.EncodableValue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

/**
 * In-memory implementation of [NamespaceRegistry] that can be instantiated for testing.
 *
 * this class can be instantiated multiple times, making it ideal for:
 * - Unit tests that need isolated flag configurations
 * - Integration tests that run in parallel
 * - Scenarios where multiple independent registries are needed
 *
 * Example usage in tests:
 * ```kotlin
 * @Test
 * fun `test feature flag behavior`() {
 *     val testRegistry = InMemoryNamespaceRegistry()
 *
 *     config(testRegistry) {
 *         MyFlags.FEATURE_A with {
 *             default(true)
 *         }
 *     }
 *
 *     val value = testRegistry.flag(MyFlags.FEATURE_A)
 *     assertEquals(true, value?.defaultValue)
 * }
 * ```
 *
 * ## Thread Safety
 *
 * This implementation is thread-safe and uses [java.util.concurrent.atomic.AtomicReference] for lock-free reads
 * and compare-and-swap updates.
 *
 * ## Test Overrides
 *
 * This registry supports test-scoped overrides that allow you to force a specific flag value
 * for the duration of a test without affecting other tests running in parallel:
 *
 * ```kotlin
 * @Test
 * fun `test with override`() {
 *     val testNamespace = TestNamespace.test("my-test")
 *     // Set override - flag will always return this value
 *     testNamespace.registry.setOverride(MyFlags.FEATURE_A, true)
 *
 *     val result = MyFlags.FEATURE_A.evaluate(contextFn)
 *     assertEquals(true, result)
 *
 *     // Clean up
 *     testNamespace.registry.clearOverride(MyFlags.FEATURE_A)
 * }
 * ```
 *
 * @constructor Creates a new empty in-memory registry
 * @since 0.0.2
 */
internal class InMemoryNamespaceRegistry : NamespaceRegistry {
    private val current = AtomicReference(Configuration(emptyMap()))

    /**
     * Thread-safe storage for test overrides.
     * Maps feature keys to their override values.
     * These overrides take precedence over normal flag evaluation.
     */
    private val overrides = ConcurrentHashMap<Feature<*, *, *, *>, Any>()

    /**
     * Loads the flag values from the provided [config] snapshot.
     *
     * This operation atomically replaces the entire current configuration.
     *
     * @param config The [Configuration] containing the configuration to load
     */
    override fun load(config: Configuration) {
        current.set(config)
    }

    /**
     * Returns the current snapshot of all flag configurations.
     *
     * @return The current [Configuration]
     */
    override val configuration: Configuration
        get() = current.get()

    /**
     * Retrieves a specific flag definition from the registry, applying any test overrides.
     *
     * If a test override is set for this flag, returns a special FlagDefinition that
     * always evaluates to the override value, bypassing all rules and rollout logic.
     *
     * This override of the [NamespaceRegistry.flag] method ensures that test overrides
     * are transparently applied when flags are evaluated via `contextFn.evaluate(feature)`.
     *
     * @param key The [Feature] key for the flag
     * @return The [FlagDefinition] (potentially wrapped with override logic)
     * @param S The EncodableValue type wrapping the actual value
     * @param T The actual value type
     * @param C The type of the contextFn used for evaluation
     * @param M The namespace the feature belongs to
     */
    @Suppress("UNCHECKED_CAST")
    override fun <S : EncodableValue<T>, T : Any, C : Context, M : Namespace> flag(
        key: Feature<S, T, C, M>,
    ): FlagDefinition<S, T, C, M> {
        val override = getOverride(key)
        return if (override != null) {
            // Create a FlagDefinition that always returns the override value
            val originalDefinition = configuration.flags[key] as? FlagDefinition<S, T, C, M>
            originalDefinition?.let { original ->
                FlagDefinition(
                    feature = original.feature,
                    bounds = emptyList(), // No rules needed - override takes precedence
                    defaultValue = override,
                    salt = original.salt,
                    isActive = true
                )
            } ?: throw IllegalStateException("Flag not found in configuration: ${key.key}")
        } else {
            configuration.flags[key] as? FlagDefinition<S, T, C, M>
            ?: throw IllegalStateException("Flag not found in configuration: ${key.key}")
        }
    }

    /**
     * Applies a [io.amichne.konditional.core.instance.ConfigurationPatch] to the current snapshot, atomically updating the flag configuration.
     *
     * This method is useful for incremental updates without replacing the entire snapshot.
     * The update is performed atomically using compare-and-swap semantics.
     *
     * **Internal API**: Used internally by FeatureContainer. Configuration updates are
     * handled automatically through delegation.
     *
     * @param patch The [io.amichne.konditional.core.instance.ConfigurationPatch] to apply
     */
    internal fun updatePatch(patch: ConfigurationPatch) {
        current.updateAndGet { currentSnapshot ->
            patch.applyTo(currentSnapshot)
        }
    }

    /**
     * Updates a single flag definition in the current configuration.
     *
     * This operation atomically updates the specified flag while leaving others unchanged.
     *
     * **Internal API**: Used internally by FeatureContainer. Configuration updates are
     * handled automatically through delegation.
     *
     * @param definition The [io.amichne.konditional.core.FlagDefinition] to update
     * @param S The type of the flag's value
     * @param C The type of the contextFn used for evaluation
     */
    internal fun <S : EncodableValue<T>, T : Any, C : Context> updateDefinition(definition: FlagDefinition<S, T, C, *>) {
        current.updateAndGet { currentSnapshot ->
            val mutableFlags = currentSnapshot.flags.toMutableMap()
            mutableFlags[definition.feature] = definition
            Configuration(mutableFlags)
        }
    }

    /**
     * Sets a test-scoped override for a specific feature flag.
     *
     * When an override is set, the flag will always return the override value
     * regardless of rules, contextFn, or rollout configuration. This is useful
     * for testing specific scenarios without modifying flag definitions.
     *
     * **Thread Safety**: This method is thread-safe and can be called from
     * concurrent test executions. Each registry instance maintains its own
     * isolated overrides map.
     *
     * @param feature The feature flag to override
     * @param value The value to return for this flag
     * @param S The EncodableValue type wrapping the actual value
     * @param T The actual value type
     * @param C The type of the contextFn used for evaluation
     * @param M The namespace the feature belongs to
     *
     * @see clearOverride
     * @see clearAllOverrides
     * @see hasOverride
     */
    @PublishedApi
    internal fun <S : EncodableValue<T>, T : Any, C : Context, M : Namespace> setOverride(
        feature: Feature<S, T, C, M>,
        value: T,
    ) {
        overrides[feature] = value as Any
    }

    /**
     * Clears the test override for a specific feature flag.
     *
     * After clearing, the flag will resume normal evaluation based on
     * its rules and configuration.
     *
     * @param feature The feature flag to clear the override for
     * @param S The EncodableValue type wrapping the actual value
     * @param T The actual value type
     * @param C The type of the contextFn used for evaluation
     * @param M The namespace the feature belongs to
     *
     * @see setOverride
     * @see clearAllOverrides
     */
    internal fun <S : EncodableValue<T>, T : Any, C : Context, M : Namespace> clearOverride(
        feature: Feature<S, T, C, M>,
    ) {
        overrides.remove(feature)
    }

    /**
     * Clears all test overrides in this registry.
     *
     * After clearing, all flags will resume normal evaluation based on
     * their rules and configuration.
     *
     * @see setOverride
     * @see clearOverride
     */
    internal fun clearAllOverrides() {
        overrides.clear()
    }

    /**
     * Checks if a test override is currently set for a specific feature flag.
     *
     * @param feature The feature flag to check
     * @return true if an override is set, false otherwise
     * @param S The EncodableValue type wrapping the actual value
     * @param T The actual value type
     * @param C The type of the contextFn used for evaluation
     * @param M The namespace the feature belongs to
     *
     * @see setOverride
     */
    internal fun <S : EncodableValue<T>, T : Any, C : Context, M : Namespace> hasOverride(
        feature: Feature<S, T, C, M>,
    ): Boolean = overrides.containsKey(feature)

    /**
     * Gets the override value for a specific feature flag, if one exists.
     *
     * @param feature The feature flag to get the override for
     * @return The override value, or null if no override is set
     * @param S The EncodableValue type wrapping the actual value
     * @param T The actual value type
     * @param C The type of the contextFn used for evaluation
     * @param M The namespace the feature belongs to
     *
     * @see setOverride
     */
    @Suppress("UNCHECKED_CAST")
    internal fun <S : EncodableValue<T>, T : Any, C : Context, M : Namespace> getOverride(
        feature: Feature<S, T, C, M>,
    ): T? = overrides[feature] as? T
}
