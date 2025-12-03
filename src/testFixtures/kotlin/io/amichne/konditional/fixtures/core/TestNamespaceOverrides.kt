package io.amichne.konditional.fixtures.core

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.features.FeatureAware
import io.amichne.konditional.core.features.FeatureContainer
import io.amichne.konditional.core.registry.InMemoryNamespaceRegistry
import io.amichne.konditional.core.types.EncodableValue

/**
 * Extension functions for [io.amichne.konditional.core.Namespace] that provide convenient test-scoped override functionality.
 *
 * These extensions allow tests to temporarily override flag values without affecting:
 * - Other tests running in parallel
 * - The actual flag definitions
 * - Other test namespaces
 *
 * ## Usage Examples
 *
 * ### Basic Override
 * ```kotlin
 * @Test
 * fun `test with flag enabled`() {
 *     val Namespace = Namespace.test("my-test")
 *     val TestFeatures = object : FeatureContainer<Namespace>(Namespace) {
 *         val myFlag by boolean<Context>(default = false)
 *     }
 *
 *     // Override the flag to return true
 *     Namespace.setOverride(TestFeatures.myFlag, true)
 *
 *     val result = contextFn.evaluate(TestFeatures.myFlag)
 *     assertEquals(true, result)
 *
 *     // Clean up
 *     Namespace.clearOverride(TestFeatures.myFlag)
 * }
 * ```
 *
 * ### Scoped Override with Automatic Cleanup
 * ```kotlin
 * @Test
 * fun `test with scoped override`() {
 *     val Namespace = Namespace.test("my-test")
 *     val TestFeatures = object : FeatureContainer<Namespace>(Namespace) {
 *         val myFlag by boolean<Context>(default = false)
 *     }
 *
 *     Namespace.withOverride(TestFeatures.myFlag, true) {
 *         val result = contextFn.evaluate(TestFeatures.myFlag)
 *         assertEquals(true, result)
 *     }
 *     // Override automatically cleared after block
 * }
 * ```
 *
 * ### Multiple Overrides
 * ```kotlin
 * @Test
 * fun `test with multiple configure`() {
 *     val Namespace = Namespace.test("my-test")
 *     val TestFeatures = object : FeatureContainer<Namespace>(Namespace) {
 *         val flagA by boolean<Context>(default = false)
 *         val flagB by string<Context>(default = "default")
 *         val flagC by integer<Context>(default = 0)
 *     }
 *
 *     Namespace.withOverrides(
 *         TestFeatures.flagA to true,
 *         TestFeatures.flagB to "override",
 *         TestFeatures.flagC to 42
 *     ) {
 *         assertEquals(true, contextFn.evaluate(TestFeatures.flagA))
 *         assertEquals("override", contextFn.evaluate(TestFeatures.flagB))
 *         assertEquals(42, contextFn.evaluate(TestFeatures.flagC))
 *     }
 *     // All configure automatically cleared
 * }
 * ```
 *
 * ## Thread Safety
 *
 * Each Namespace instance has its own isolated registry, so configure are naturally
 * isolated between tests. Multiple tests can run in parallel without interference.
 *
 * ## Best Practices
 *
 * - Use `withOverride` or `withOverrides` for automatic cleanup
 * - If using `setOverride`, always pair with `clearOverride` (preferably in a try-finally)
 * - Create a fresh Namespace instance for each test
 * - Don't share Namespace instances between tests
 */

/**
 * Sets a test-scoped override for a specific feature flag.
 *
 * When an override is set, the flag will always return the override value
 * regardless of rules, contextFn, or rollout configuration.
 *
 * **Important**: Remember to call [clearOverride] when done, or use [withOverride]
 * for automatic cleanup.
 *
 * @param feature The feature flag to override
 * @param value The value to return for this flag
 * @param S The EncodableValue type wrapping the actual value
 * @param T The actual value type
 * @param C The type of the contextFn used for evaluation
 *
 * @see clearOverride
 * @see withOverride
 */
fun <S : EncodableValue<T>, T : Any, C : Context> Namespace.setOverride(
    feature: Feature<S, T, C, *>,
    value: T,
) {
    (registry as InMemoryNamespaceRegistry).setOverride(feature, value)
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
 *
 * @see setOverride
 * @see clearAllOverrides
 */
fun <S : EncodableValue<T>, T : Any, C : Context> Namespace.clearOverride(
    feature: Feature<S, T, C, *>,
) {
    (registry as InMemoryNamespaceRegistry).clearOverride(feature)
}

/**
 * Clears all test configure in this namespace.
 *
 * After clearing, all flags will resume normal evaluation based on
 * their rules and configuration.
 *
 * @see setOverride
 * @see clearOverride
 */
fun Namespace.clearAllOverrides() {
    (registry as InMemoryNamespaceRegistry).clearAllOverrides()
}

/**
 * Checks if a test override is currently set for a specific feature flag.
 *
 * @param feature The feature flag to check
 * @return true if an override is set, false otherwise
 * @param S The EncodableValue type wrapping the actual value
 * @param T The actual value type
 * @param C The type of the contextFn used for evaluation
 *
 * @see setOverride
 */
fun <S : EncodableValue<T>, T : Any, C : Context> Namespace.hasOverride(
    feature: Feature<S, T, C, *>,
): Boolean = (registry as InMemoryNamespaceRegistry).hasOverride(feature)

/**
 * Executes a block of code with a test-scoped override, automatically clearing it afterward.
 *
 * This is the recommended way to use configure as it ensures cleanup even if the block throws.
 *
 * @param feature The feature flag to override
 * @param value The value to return for this flag
 * @param block The code to execute with the override in place
 * @return The result of the block
 * @param S The EncodableValue type wrapping the actual value
 * @param T The actual value type
 * @param C The type of the contextFn used for evaluation
 * @param R The return type of the block
 *
 * @see setOverride
 * @see withOverrides
 */
inline fun <S : EncodableValue<T>, T : Any, C : Context, R> Namespace.withOverride(
    feature: Feature<S, T, C, *>,
    value: T,
    block: () -> R,
): R {
    setOverride(feature, value)
    return try {
        block()
    } finally {
        clearOverride(feature)
    }
}

/**
 * Executes a block of code with multiple test-scoped configure, automatically clearing them afterward.
 *
 * This is the recommended way to use multiple configure as it ensures cleanup even if the block throws.
 *
 * @param overrides Pairs of features to their override values
 * @param block The code to execute with the configure in place
 * @return The result of the block
 * @param R The return type of the block
 *
 * @see setOverride
 * @see withOverride
 */
fun <M : Namespace, F : FeatureContainer<M>> F.withOverrides(
    vararg overrides: Pair<Feature<*, *, *, *>, Any>,
    block: F.() -> Unit,
): Unit {
    // Set all configure
    overrides.forEach { (feature, value) ->
        @Suppress("UNCHECKED_CAST")
        (namespace.registry as InMemoryNamespaceRegistry).setOverride(
            feature as Feature<EncodableValue<Any>, Any, Context, *>,
            value
        )
    }
    return try {
        block()
    } finally {
        // Clear all configure
        overrides.forEach { (feature, _) ->
            @Suppress("UNCHECKED_CAST")
            (namespace.registry as InMemoryNamespaceRegistry).clearOverride(
                feature as Feature<EncodableValue<Any>, Any, Context, *>
            )
        }
    }
}

interface AtomicTestScope {
    companion object {
        operator fun <M : Namespace, F : FeatureAware<M>> invoke(
            overridingScope: OverridingScope<M, F>,
        ): AtomicTestScope = object : AtomicTestScope {}


        infix fun AtomicTestScope.runTest(testBlock: () -> Unit) = testBlock()
    }

}

@ConsistentCopyVisibility
data class OverridingScope<M : Namespace, F : FeatureAware<M>> @PublishedApi internal constructor(
    private val features: F,
) : FeatureAware<M> by features {
    inline fun <reified S : EncodableValue<T>, reified T : Any, reified C : Context> update(
        feature: Feature<S, T, C, *>,
        value: T,
    ) {
        container.namespace.setOverride(feature, value)
    }

    companion object {
        inline fun <reified M : Namespace, reified T> setupTest(
            container: T,
            features: T.(OverridingScope<M, T>) -> Unit,
        ): AtomicTestScope where T : FeatureAware<M> =
            AtomicTestScope(OverridingScope(container).apply { features(container, this) })
    }
}
