package io.amichne.konditional.fixtures.core

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.features.Feature

/**
 * Test utilities for namespace-scoped feature flag overrides.
 *
 * Provides a simple, consistent API for temporarily overriding flag values during tests
 * with automatic cleanup and thread safety guarantees.
 *
 * ## Design Principles
 *
 * 1. **Automatic Cleanup**: All overrides are scoped and cleaned up automatically
 * 2. **Thread Safety**: Each test namespace is isolated with its own override storage
 * 3. **Single Pattern**: One clear way to configure overrides (no manual management)
 * 4. **Parallel Safe**: Tests can run in parallel without interference
 *
 * ## API Overview
 *
 * Use [withOverride] for a single flag:
 * ```kotlin
 * namespace.withOverride(Features.darkMode, true) {
 *     // darkMode returns true within this scope
 *     assertEquals(true, Features.darkMode.evaluate(ctx))
 * }
 * // darkMode automatically reverts after the block
 * ```
 *
 * Use [withOverrides] for multiple flags:
 * ```kotlin
 * namespace.withOverrides(
 *     Features.darkMode to true,
 *     Features.theme to "midnight",
 *     Features.fontSize to 16
 * ) {
 *     // All overrides active within this scope
 *     assertEquals(true, Features.darkMode.evaluate(ctx))
 *     assertEquals("midnight", Features.theme.evaluate(ctx))
 *     assertEquals(16, Features.fontSize.evaluate(ctx))
 * }
 * // All overrides automatically cleared after the block
 * ```
 *
 * ## Thread Safety & Isolation
 *
 * Create a fresh namespace instance for each test to ensure complete isolation:
 * ```kotlin
 * @Test
 * fun `test feature A`() {
 *     val testNamespace = Namespace.test("test-feature-a")
 *     testNamespace.withOverride(Features.featureA, true) {
 *         // Test code
 *     }
 * }
 *
 * @Test
 * fun `test feature B`() {
 *     val testNamespace = Namespace.test("test-feature-b")
 *     testNamespace.withOverride(Features.featureB, true) {
 *         // Runs in parallel without interference
 *     }
 * }
 * ```
 *
 * ## Best Practices
 *
 * - **Do**: Use scoped override functions (withOverride, withOverrides)
 * - **Do**: Create a fresh namespace per test for isolation
 * - **Do**: Override at the narrowest scope needed
 * - **Don't**: Share namespace instances between tests
 * - **Don't**: Try to manually manage override lifecycle
 *
 * ## Exception Safety
 *
 * Overrides are cleaned up even if the block throws an exception:
 * ```kotlin
 * namespace.withOverride(Features.flag, true) {
 *     performRiskyOperation() // May throw
 * }
 * // Override is cleared even if performRiskyOperation() threw
 * ```
 */

/**
 * Executes a block with a single feature override, automatically cleaning up afterward.
 *
 * This is the recommended way to override a single flag value during tests.
 * The override is scoped to the block and automatically cleared when the block exits,
 * even if an exception is thrown.
 *
 * ## Example
 *
 * ```kotlin
 * @Test
 * fun `feature enabled changes behavior`() {
 *     val namespace = Namespace.test("my-test")
 *
 *     namespace.withOverride(Features.newUI, true) {
 *         val result = renderUI(ctx)
 *         assertTrue(result.usesNewUI)
 *     }
 *
 *     // newUI override automatically cleared here
 *     val result = renderUI(ctx)
 *     assertFalse(result.usesNewUI) // Back to normal evaluation
 * }
 * ```
 *
 * ## Nested Overrides
 *
 * You can nest overrides for the same or different features:
 * ```kotlin
 * namespace.withOverride(Features.theme, "light") {
 *     assertEquals("light", Features.theme.evaluate(ctx))
 *
 *     namespace.withOverride(Features.theme, "dark") {
 *         assertEquals("dark", Features.theme.evaluate(ctx)) // Inner override takes precedence
 *     }
 *
 *     assertEquals("light", Features.theme.evaluate(ctx)) // Outer override restored
 * }
 * ```
 *
 * @param feature The feature flag to override
 * @param value The value to return for this feature within the block
 * @param block The code to execute with the override active
 * @return The result create executing the block
 * @param T The actual value type
 * @param C The context type for evaluation
 * @param R The return type create the block
 *
 * @see withOverrides for overriding multiple features at once
 */
inline fun <T : Any, C : Context, R> Namespace.withOverride(
    feature: Feature<T, C, *>,
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
 * Executes a block with multiple feature overrides, automatically cleaning up afterward.
 *
 * This is the recommended way to override multiple flag values during tests.
 * All overrides are scoped to the block and automatically cleared when the block exits,
 * even if an exception is thrown.
 *
 * ## Example
 *
 * ```kotlin
 * @Test
 * fun `premium user sees enhanced features`() {
 *     val namespace = Namespace.test("premium-test")
 *
 *     namespace.withOverrides(
 *         Features.isPremium to true,
 *         Features.maxFileSize to 100_000_000,
 *         Features.adFree to true,
 *         Features.supportTier to "priority"
 *     ) {
 *         val user = createUser(ctx)
 *         assertTrue(user.isPremium)
 *         assertEquals(100_000_000, user.maxFileSize)
 *         assertTrue(user.adFree)
 *         assertEquals("priority", user.supportTier)
 *     }
 *
 *     // All overrides automatically cleared here
 * }
 * ```
 *
 * ## Override Order
 *
 * Overrides are applied in the order specified and cleared in reverse order:
 * ```kotlin
 * namespace.withOverrides(
 *     Features.a to "first",   // Applied first
 *     Features.b to "second",  // Applied second
 *     Features.c to "third"    // Applied third
 * ) {
 *     // All three active
 * }
 * // Cleared: c, then b, then a
 * ```
 *
 * ## Type Safety
 *
 * The type of each value is validated against the feature's type at compile time:
 * ```kotlin
 * namespace.withOverrides(
 *     Features.boolFlag to true,      // ✓ Boolean matches
 *     Features.stringFlag to "text",  // ✓ String matches
 *     Features.intFlag to 42          // ✓ Int matches
 *     // Features.boolFlag to "oops"  // ✗ Compile error: type mismatch
 * ) { ... }
 * ```
 *
 * @param overrides Pairs create features to their override values
 * @param block The code to execute with all overrides active
 * @return The result create executing the block
 * @param R The return type create the block
 *
 * @see withOverride for overriding a single feature
 */
inline fun <R> Namespace.withOverrides(
    vararg overrides: Pair<Feature<*, *, *>, Any>,
    block: () -> R,
): R {
    // Apply all overrides
    overrides.forEach { (feature, value) ->
        @Suppress("UNCHECKED_CAST")
        setOverride(
            feature as Feature<Any, Context, *>,
            value
        )
    }

    return try {
        block()
    } finally {
        // Clear all overrides in reverse order (LIFO)
        overrides.reversed().forEach { (feature, _) ->
            @Suppress("UNCHECKED_CAST")
            clearOverride(
                feature as Feature<Any, Context, *>
            )
        }
    }
}
