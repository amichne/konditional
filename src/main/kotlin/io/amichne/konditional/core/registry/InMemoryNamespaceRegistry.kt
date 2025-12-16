package io.amichne.konditional.core.registry

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.instance.Configuration
import io.amichne.konditional.core.instance.ConfigurationPatch
import io.amichne.konditional.core.ops.ConfigLoadMetric
import io.amichne.konditional.core.ops.ConfigRollbackMetric
import io.amichne.konditional.core.ops.RegistryHooks
import io.amichne.konditional.rules.ConditionalValue.Companion.targetedBy
import io.amichne.konditional.rules.Rule
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
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
internal class InMemoryNamespaceRegistry(
    override val namespaceId: String,
    hooks: RegistryHooks = RegistryHooks.None,
    private val historyLimit: Int = DEFAULT_HISTORY_LIMIT,
) : NamespaceRegistry {
    private val current = AtomicReference(Configuration(emptyMap()))
    private val hooksRef = AtomicReference(hooks)
    private val allDisabled = AtomicBoolean(false)
    private val historyRef = AtomicReference<List<Configuration>>(emptyList())
    private val writeLock = Any()

    /**
     * Thread-safe storage for test overrides.
     * Maps features to a stack of override values to support nested overrides.
     * The top of each stack is the currently active override.
     * These overrides take precedence over normal flag evaluation.
     */
    private val overrides = ConcurrentHashMap<Feature<*, *, *>, ArrayDeque<Any>>()

    /**
     * Loads the flag values from the provided [config] snapshot.
     *
     * This operation atomically replaces the entire current configuration.
     *
     * @param config The [Configuration] containing the configuration to load
     */
    override fun load(config: Configuration) {
        synchronized(writeLock) {
            val previous = current.getAndSet(config)
            historyRef.set((historyRef.get() + previous).takeLast(historyLimit))
        }

        hooksRef.get().metrics.recordConfigLoad(
            ConfigLoadMetric.of(
                namespaceId = namespaceId,
                featureCount = config.flags.size,
                version = config.metadata.version,
            )
        )
    }

    /**
     * Returns the current snapshot of all flag configurations.
     *
     * @return The current [Configuration]
     */
    override val configuration: Configuration
        get() = current.get()

    override val hooks: RegistryHooks
        get() = hooksRef.get()

    override fun setHooks(hooks: RegistryHooks) {
        hooksRef.set(hooks)
    }

    override val isAllDisabled: Boolean
        get() = allDisabled.get()

    override fun disableAll() {
        allDisabled.set(true)
    }

    override fun enableAll() {
        allDisabled.set(false)
    }

    override val history: List<Configuration>
        get() = historyRef.get()

    override fun rollback(steps: Int): Boolean {
        require(steps >= 1) { "steps must be >= 1" }
        val restored = synchronized(writeLock) {
            val history = historyRef.get()
            if (history.size < steps) return false

            val targetIndex = history.size - steps
            val target = history[targetIndex]
            val newHistory = history.take(targetIndex)

            current.set(target)
            historyRef.set(newHistory)
            target
        }

        hooksRef.get().metrics.recordConfigRollback(
            ConfigRollbackMetric(
                namespaceId = namespaceId,
                steps = steps,
                success = true,
                version = restored.metadata.version,
            )
        )
        return true
    }

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
     * @param T The actual value type
     * @param C The type of the contextFn used for evaluation
     * @param M The namespace the feature belongs to
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any, C : Context, M : Namespace> flag(
        key: Feature<T, C, M>,
    ): FlagDefinition<T, C, M> {
        val override = getOverride(key)
        return if (override != null) {
            val originalDefinition = configuration.flags[key] as? FlagDefinition<T, C, M>
            originalDefinition?.let { original ->
                FlagDefinition(
                    feature = original.feature,
                    bounds = listOf(Rule<C>().targetedBy(override)),
                    defaultValue = original.defaultValue,
                    salt = original.salt,
                    isActive = true
                )
            } ?: throw IllegalStateException("Flag not found in configuration: ${key.key}")
        } else {
            configuration.flags[key] as? FlagDefinition<T, C, M>
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
    internal fun <T : Any, C : Context> updateDefinition(definition: FlagDefinition<T, C, *>) {
        current.updateAndGet { currentSnapshot ->
            val mutableFlags = currentSnapshot.flags.toMutableMap()
            mutableFlags[definition.feature] = definition
            Configuration(mutableFlags, currentSnapshot.metadata)
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
     * @param T The actual value type
     * @param C The type of the contextFn used for evaluation
     * @param M The namespace the feature belongs to
     *
     * @see clearOverride
     * @see clearAllOverrides
     * @see hasOverride
     */
    @PublishedApi
    internal fun <T : Any, C : Context, M : Namespace> setOverride(
        feature: Feature<T, C, M>,
        value: T,
    ) {
        overrides.compute(feature) { _, stack ->
            val deque = stack ?: ArrayDeque()
            deque.addLast(value as Any)
            deque
        }
    }

    /**
     * Clears the most recent test override for a specific feature flag.
     *
     * If multiple nested overrides exist for the same feature, this pops the
     * top value from the stack and restores the previous override. Only when
     * all overrides are cleared will the flag resume normal evaluation.
     *
     * @param feature The feature flag to clear the override for
     * @param T The actual value type
     * @param C The type of the contextFn used for evaluation
     * @param M The namespace the feature belongs to
     *
     * @see setOverride
     * @see clearAllOverrides
     */
    @PublishedApi
    internal fun <T : Any, C : Context, M : Namespace> clearOverride(
        feature: Feature<T, C, M>,
    ) {
        overrides.compute(feature) { _, stack ->
            if (stack.isNullOrEmpty()) {
                null
            } else {
                stack.removeLast()
                if (stack.isEmpty()) null else stack
            }
        }
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
     * @param T The actual value type
     * @param C The type of the contextFn used for evaluation
     * @param M The namespace the feature belongs to
     *
     * @see setOverride
     */
    internal fun <T : Any, C : Context, M : Namespace> hasOverride(
        feature: Feature<T, C, M>,
    ): Boolean = overrides[feature]?.isNotEmpty() == true

    /**
     * Gets the current override value for a specific feature flag, if one exists.
     *
     * If multiple nested overrides exist, returns the most recent (top of stack).
     *
     * @param feature The feature flag to get the override for
     * @return The override value, or null if no override is set
     * @param T The actual value type
     * @param C The type of the contextFn used for evaluation
     * @param M The namespace the feature belongs to
     *
     * @see setOverride
     */
    @Suppress("UNCHECKED_CAST")
    internal fun <T : Any, C : Context, M : Namespace> getOverride(
        feature: Feature<T, C, M>,
    ): T? = overrides[feature]?.lastOrNull() as? T

    companion object {
        internal const val DEFAULT_HISTORY_LIMIT: Int = 10
    }
}
