package io.amichne.konditional.core

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Context.Companion.evaluate
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.features.FeatureContainer
import io.amichne.konditional.core.result.getOrThrow
import io.amichne.konditional.fixtures.core.OverridingScope.Companion.setupTest
import io.amichne.konditional.fixtures.core.TestNamespace
import io.amichne.konditional.fixtures.core.clearAllOverrides
import io.amichne.konditional.fixtures.core.clearOverride
import io.amichne.konditional.fixtures.core.hasOverride
import io.amichne.konditional.fixtures.core.id.TestStableId
import io.amichne.konditional.fixtures.core.setOverride
import io.amichne.konditional.fixtures.core.withOverride
import io.amichne.konditional.fixtures.core.withOverrides
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Comprehensive tests for TestNamespace override functionality.
 *
 * These tests demonstrate:
 * - Basic override usage
 * - Scoped configure with automatic cleanup
 * - Multiple configure
 * - Override isolation between tests
 * - Thread safety and parallel test execution
 * - Overrides bypassing rules and rollout logic
 */
class TestNamespaceOverridesTest {

    private val testContext = Context(
        locale = AppLocale.UNITED_STATES,
        platform = Platform.WEB,
        appVersion = Version.parse("1.0.0").getOrThrow(),
        stableId = TestStableId
    )

    @Test
    fun `basic override sets flag to specific value`() {
        val testNamespace = TestNamespace.test("basic-override")
        val TestFeatures = object : FeatureContainer<TestNamespace>(testNamespace) {
            val myFlag by boolean<Context>(default = false)
        }

        setupTest(TestFeatures) {

        }

        // Without override, returns default
        assertEquals(false, testContext.evaluate(TestFeatures.myFlag))

        // Set override
        testNamespace.setOverride(TestFeatures.myFlag, true)
        assertEquals(true, testContext.evaluate(TestFeatures.myFlag))

        // Clear override
        testNamespace.clearOverride(TestFeatures.myFlag)
        assertEquals(false, testContext.evaluate(TestFeatures.myFlag))
    }

    @Test
    fun `override bypasses rules and rollout logic`() {
        val testNamespace = TestNamespace.test("override-bypasses-rules")
        val TestFeatures = object : FeatureContainer<TestNamespace>(testNamespace) {
            val myFlag by boolean<Context>(default = false) {
                // Rule that would normally make this true for WEB platform
                rule { platforms(Platform.WEB) } returns true
            }
        }

        // Normal evaluation follows rule - returns true for WEB
        assertEquals(true, testContext.evaluate(TestFeatures.myFlag))

        // Override to false - bypasses rule
        testNamespace.setOverride(TestFeatures.myFlag, false)
        assertEquals(false, testContext.evaluate(TestFeatures.myFlag))

        // Clear override - rule applies again
        testNamespace.clearOverride(TestFeatures.myFlag)
        assertEquals(true, testContext.evaluate(TestFeatures.myFlag))
    }

    @Test
    fun `scoped override with automatic cleanup`() {
        val testNamespace = TestNamespace.test("scoped-override")
        val TestFeatures = object : FeatureContainer<TestNamespace>(testNamespace) {
            val myFlag by boolean<Context>(default = false)
        }

        // Before override
        assertEquals(false, testContext.evaluate(TestFeatures.myFlag))

        // Within scope - override is active
        val result = testNamespace.withOverride(TestFeatures.myFlag, true) {
            assertEquals(true, testContext.evaluate(TestFeatures.myFlag))
            "success"
        }
        assertEquals("success", result)

        // After scope - override is cleared
        assertEquals(false, testContext.evaluate(TestFeatures.myFlag))
    }

    @Test
    fun `scoped override cleans up even on exception`() {
        val testNamespace = TestNamespace.test("scoped-override-exception")
        val TestFeatures = object : FeatureContainer<TestNamespace>(testNamespace) {
            val myFlag by boolean<Context>(default = false)
        }

        // Before override
        assertEquals(false, testContext.evaluate(TestFeatures.myFlag))

        // Exception thrown in scope
        assertThrows(RuntimeException::class.java) {
            testNamespace.withOverride(TestFeatures.myFlag, true) {
                assertEquals(true, testContext.evaluate(TestFeatures.myFlag))
                throw RuntimeException("Test exception")
            }
        }

        // Override is still cleared despite exception
        assertEquals(false, testContext.evaluate(TestFeatures.myFlag))
    }

    @Test
    fun `multiple overrides work independently`() {
        val testNamespace = TestNamespace.test("multiple-testScope")
        val TestFeatures = object : FeatureContainer<TestNamespace>(testNamespace) {
            val boolFlag by boolean<Context>(default = false)
            val stringFlag by string<Context>(default = "default")
            val intFlag by integer<Context>(default = 0)
            val doubleFlag by double<Context>(default = 0.0)
        }

        // Set multiple configure
        testNamespace.setOverride(TestFeatures.boolFlag, true)
        testNamespace.setOverride(TestFeatures.stringFlag, "override")
        testNamespace.setOverride(TestFeatures.intFlag, 42)
        testNamespace.setOverride(TestFeatures.doubleFlag, 3.14)

        // All configure are active
        assertEquals(true, testContext.evaluate(TestFeatures.boolFlag))
        assertEquals("override", testContext.evaluate(TestFeatures.stringFlag))
        assertEquals(42, testContext.evaluate(TestFeatures.intFlag))
        assertEquals(3.14, testContext.evaluate(TestFeatures.doubleFlag))

        // Clear one override - others remain
        testNamespace.clearOverride(TestFeatures.boolFlag)
        assertEquals(false, testContext.evaluate(TestFeatures.boolFlag))
        assertEquals("override", testContext.evaluate(TestFeatures.stringFlag))
        assertEquals(42, testContext.evaluate(TestFeatures.intFlag))
        assertEquals(3.14, testContext.evaluate(TestFeatures.doubleFlag))

        // Clear all configure
        testNamespace.clearAllOverrides()
        assertEquals(false, testContext.evaluate(TestFeatures.boolFlag))
        assertEquals("default", testContext.evaluate(TestFeatures.stringFlag))
        assertEquals(0, testContext.evaluate(TestFeatures.intFlag))
        assertEquals(0.0, testContext.evaluate(TestFeatures.doubleFlag))
    }

    @Test
    fun `withOverrides supports multiple flags at once`() {
        val testNamespace = Namespace.Search
        val TestFeatures = object : FeatureContainer<Namespace.Search>(testNamespace) {
            val flagA by boolean<Context>(default = false)
            val flagB by string<Context>(default = "default")
            val flagC by integer<Context>(default = 0)
        }

        TestFeatures.withOverrides(
            TestFeatures.flagA to true,
            TestFeatures.flagB to "test",
            TestFeatures.flagC to 100
        ) {
            assertEquals(true, testContext.evaluate(TestFeatures.flagA))
            assertEquals("test", testContext.evaluate(TestFeatures.flagB))
            assertEquals(100, testContext.evaluate(TestFeatures.flagC))
        }

        // All configure cleared after block
        assertEquals(false, testContext.evaluate(TestFeatures.flagA))
        assertEquals("default", testContext.evaluate(TestFeatures.flagB))
        assertEquals(0, testContext.evaluate(TestFeatures.flagC))
    }

    @Test
    fun `hasOverride correctly detects override state`() {
        val testNamespace = TestNamespace.test("has-override")
        val TestFeatures = object : FeatureContainer<TestNamespace>(testNamespace) {
            val myFlag by boolean<Context>(default = false)
        }

        // No override initially
        assertFalse(testNamespace.hasOverride(TestFeatures.myFlag))

        // Set override
        testNamespace.setOverride(TestFeatures.myFlag, true)
        assertTrue(testNamespace.hasOverride(TestFeatures.myFlag))

        // Clear override
        testNamespace.clearOverride(TestFeatures.myFlag)
        assertFalse(testNamespace.hasOverride(TestFeatures.myFlag))
    }

    @Test
    fun `overrides are isolated between test namespaces`() {
        val namespace1 = TestNamespace.test("isolation-test-1")
        val namespace2 = TestNamespace.test("isolation-test-2")

        val Features1 = object : FeatureContainer<TestNamespace>(namespace1) {
            val myFlag by boolean<Context>(default = false)
        }

        val Features2 = object : FeatureContainer<TestNamespace>(namespace2) {
            val myFlag by boolean<Context>(default = false)
        }

        // Set override in namespace1 only
        namespace1.setOverride(Features1.myFlag, true)

        // namespace1 has override, namespace2 doesn't
        assertEquals(true, testContext.evaluate(Features1.myFlag))
        assertEquals(false, testContext.evaluate(Features2.myFlag))

        // Clear doesn't affect other namespace
        namespace1.clearOverride(Features1.myFlag)
        assertEquals(false, testContext.evaluate(Features1.myFlag))
        assertEquals(false, testContext.evaluate(Features2.myFlag))
    }

    @Test
    fun `overrides work with string flags`() {
        val testNamespace = TestNamespace.test("string-override")
        val TestFeatures = object : FeatureContainer<TestNamespace>(testNamespace) {
            val apiEndpoint by string<Context>(default = "https://api.default.com")
        }

        assertEquals("https://api.default.com", testContext.evaluate(TestFeatures.apiEndpoint))

        testNamespace.setOverride(TestFeatures.apiEndpoint, "https://api.test.com")
        assertEquals("https://api.test.com", testContext.evaluate(TestFeatures.apiEndpoint))

        testNamespace.clearOverride(TestFeatures.apiEndpoint)
        assertEquals("https://api.default.com", testContext.evaluate(TestFeatures.apiEndpoint))
    }

    @Test
    fun `overrides work with int flags`() {
        val testNamespace = TestNamespace.test("int-override")
        val TestFeatures = object : FeatureContainer<TestNamespace>(testNamespace) {
            val maxRetries by int<Context>(default = 3)
        }

        assertEquals(3, testContext.evaluate(TestFeatures.maxRetries))

        testNamespace.setOverride(TestFeatures.maxRetries, 10)
        assertEquals(10, testContext.evaluate(TestFeatures.maxRetries))

        testNamespace.clearOverride(TestFeatures.maxRetries)
        assertEquals(3, testContext.evaluate(TestFeatures.maxRetries))
    }

    @Test
    fun `overrides work with double flags`() {
        val testNamespace = TestNamespace.test("double-override")
        val TestFeatures = object : FeatureContainer<TestNamespace>(testNamespace) {
            val priceMultiplier by double<Context>(default = 1.0)
        }

        assertEquals(1.0, testContext.evaluate(TestFeatures.priceMultiplier))

        testNamespace.setOverride(TestFeatures.priceMultiplier, 1.5)
        assertEquals(1.5, testContext.evaluate(TestFeatures.priceMultiplier))

        testNamespace.clearOverride(TestFeatures.priceMultiplier)
        assertEquals(1.0, testContext.evaluate(TestFeatures.priceMultiplier))
    }

    @Test
    fun `overrides can be changed without clearing first`() {
        val testNamespace = TestNamespace.test("change-override")
        val TestFeatures = object : FeatureContainer<TestNamespace>(testNamespace) {
            val counter by int<Context>(default = 0)
        }

        testNamespace.setOverride(TestFeatures.counter, 1)
        assertEquals(1, testContext.evaluate(TestFeatures.counter))

        testNamespace.setOverride(TestFeatures.counter, 2)
        assertEquals(2, testContext.evaluate(TestFeatures.counter))

        testNamespace.setOverride(TestFeatures.counter, 3)
        assertEquals(3, testContext.evaluate(TestFeatures.counter))

        testNamespace.clearOverride(TestFeatures.counter)
        assertEquals(0, testContext.evaluate(TestFeatures.counter))
    }

    @Test
    fun `parallel tests with separate namespaces don't interfere`() {
        val executor = Executors.newFixedThreadPool(10)
        val latch = CountDownLatch(10)
        val successCount = AtomicInteger(0)

        repeat(10) { i ->
            executor.submit {
                try {
                    // Each test gets its own namespace
                    val testNamespace = TestNamespace.test("parallel-test-$i")
                    val TestFeatures = object : FeatureContainer<TestNamespace>(testNamespace) {
                        val myFlag by int<Context>(default = 0)
                    }

                    // Set override to unique value per test
                    testNamespace.setOverride(TestFeatures.myFlag, i)

                    // Verify we get the correct value
                    val result = testContext.evaluate(TestFeatures.myFlag)
                    if (result == i) {
                        successCount.incrementAndGet()
                    }

                    testNamespace.clearOverride(TestFeatures.myFlag)
                } finally {
                    latch.countDown()
                }
            }
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS))
        assertEquals(10, successCount.get(), "All parallel tests should succeed without interference")
        executor.shutdown()
    }

    @Test
    fun `override affects all evaluations until cleared`() {
        val testNamespace = TestNamespace.test("multiple-evaluations")
        val TestFeatures = object : FeatureContainer<TestNamespace>(testNamespace) {
            val myFlag by boolean<Context>(default = false)
        }

        testNamespace.setOverride(TestFeatures.myFlag, true)

        // Multiple evaluations should all return the override value
        repeat(5) {
            assertEquals(true, testContext.evaluate(TestFeatures.myFlag))
        }

        testNamespace.clearOverride(TestFeatures.myFlag)

        // After clearing, all evaluations return default
        repeat(5) {
            assertEquals(false, testContext.evaluate(TestFeatures.myFlag))
        }
    }

    @Test
    fun `clearAllOverrides removes all overrides at once`() {
        val testNamespace = TestNamespace.test("clear-all")
        val TestFeatures = object : FeatureContainer<TestNamespace>(testNamespace) {
            val flag1 by boolean<Context>(default = false)
            val flag2 by string<Context>(default = "default")
            val flag3 by integer<Context>(default = 0)
        }

        // Set multiple configure
        testNamespace.setOverride(TestFeatures.flag1, true)
        testNamespace.setOverride(TestFeatures.flag2, "override")
        testNamespace.setOverride(TestFeatures.flag3, 42)

        // Verify all are active
        assertTrue(testNamespace.hasOverride(TestFeatures.flag1))
        assertTrue(testNamespace.hasOverride(TestFeatures.flag2))
        assertTrue(testNamespace.hasOverride(TestFeatures.flag3))

        // Clear all at once
        testNamespace.clearAllOverrides()

        // Verify all are cleared
        assertFalse(testNamespace.hasOverride(TestFeatures.flag1))
        assertFalse(testNamespace.hasOverride(TestFeatures.flag2))
        assertFalse(testNamespace.hasOverride(TestFeatures.flag3))

        // Verify values are back to defaults
        assertEquals(false, testContext.evaluate(TestFeatures.flag1))
        assertEquals("default", testContext.evaluate(TestFeatures.flag2))
        assertEquals(0, testContext.evaluate(TestFeatures.flag3))
    }

    @Test
    fun `nested withOverride calls work correctly`() {
        val testNamespace = TestNamespace.test("nested-testScope")
        val TestFeatures = object : FeatureContainer<TestNamespace>(testNamespace) {
            val outerFlag by boolean<Context>(default = false)
            val innerFlag by string<Context>(default = "default")
        }

        testNamespace.withOverride(TestFeatures.outerFlag, true) {
            assertEquals(true, testContext.evaluate(TestFeatures.outerFlag))
            assertEquals("default", testContext.evaluate(TestFeatures.innerFlag))

            testNamespace.withOverride(TestFeatures.innerFlag, "nested") {
                assertEquals(true, testContext.evaluate(TestFeatures.outerFlag))
                assertEquals("nested", testContext.evaluate(TestFeatures.innerFlag))
            }

            // Inner override cleared
            assertEquals(true, testContext.evaluate(TestFeatures.outerFlag))
            assertEquals("default", testContext.evaluate(TestFeatures.innerFlag))
        }

        // Both configure cleared
        assertEquals(false, testContext.evaluate(TestFeatures.outerFlag))
        assertEquals("default", testContext.evaluate(TestFeatures.innerFlag))
    }
}
