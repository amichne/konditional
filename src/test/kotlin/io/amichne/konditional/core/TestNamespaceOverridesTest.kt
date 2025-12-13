package io.amichne.konditional.core

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.features.FeatureContainer
import io.amichne.konditional.core.features.evaluate
import io.amichne.konditional.core.result.getOrThrow
import io.amichne.konditional.fixtures.core.TestNamespace
import io.amichne.konditional.fixtures.core.clearAllOverrides
import io.amichne.konditional.fixtures.core.clearOverride
import io.amichne.konditional.fixtures.core.hasOverride
import io.amichne.konditional.fixtures.core.id.TestStableId
import io.amichne.konditional.fixtures.core.setOverride
import io.amichne.konditional.fixtures.core.setupTest
import io.amichne.konditional.fixtures.core.testScoped
import io.amichne.konditional.fixtures.core.withOverride
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
        assertEquals(false, TestFeatures.myFlag.evaluate(testContext))

        // Set override
        testNamespace.setOverride(TestFeatures.myFlag, true)
        assertEquals(true, TestFeatures.myFlag.evaluate(testContext))

        // Clear override
        testNamespace.clearOverride(TestFeatures.myFlag)
        assertEquals(false, TestFeatures.myFlag.evaluate(testContext))
    }

    @Test
    fun `override bypasses rules and rollout logic`() {
        val testNamespace = TestNamespace.test("override-bypasses-rules")
        val TestFeatures = object : FeatureContainer<TestNamespace>(testNamespace) {
            val myFlag by boolean<Context>(default = false) {
                // Rule that would normally make this true for WEB platform
                rule(true) { platforms(Platform.WEB) }
            }
        }

        // Normal evaluation follows rule - returns true for WEB
        assertEquals(true, TestFeatures.myFlag.evaluate(testContext))

        // Override to false - bypasses rule
        testNamespace.setOverride(TestFeatures.myFlag, false)
        assertEquals(false, TestFeatures.myFlag.evaluate(testContext))

        // Clear override - rule applies again
        testNamespace.clearOverride(TestFeatures.myFlag)
        assertEquals(true, TestFeatures.myFlag.evaluate(testContext))
    }

    @Test
    fun `scoped override with automatic cleanup`() {
        val testNamespace = TestNamespace.test("scoped-override")
        val TestFeatures = object : FeatureContainer<TestNamespace>(testNamespace) {
            val myFlag by boolean<Context>(default = false)
        }

        // Before override
        assertEquals(false, TestFeatures.myFlag.evaluate(testContext))

        // Within scope - override is active
        val result = testNamespace.withOverride(TestFeatures.myFlag, true) {
            assertEquals(true, TestFeatures.myFlag.evaluate(testContext))
            "success"
        }
        assertEquals("success", result)

        // After scope - override is cleared
        assertEquals(false, TestFeatures.myFlag.evaluate(testContext))
    }

    @Test
    fun `scoped override cleans up even on exception`() {
        val testNamespace = TestNamespace.test("scoped-override-exception")
        val TestFeatures = object : FeatureContainer<TestNamespace>(testNamespace) {
            val myFlag by boolean<Context>(default = false)
        }

        // Before override
        assertEquals(false, TestFeatures.myFlag.evaluate(testContext))

        // Exception thrown in scope
        assertThrows(RuntimeException::class.java) {
            testNamespace.withOverride(TestFeatures.myFlag, true) {
                assertEquals(true, TestFeatures.myFlag.evaluate(testContext))
                throw RuntimeException("Test exception")
            }
        }

        // Override is still cleared despite exception
        assertEquals(false, TestFeatures.myFlag.evaluate(testContext))
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
        assertEquals(true, TestFeatures.boolFlag.evaluate(testContext))
        assertEquals("override", TestFeatures.stringFlag.evaluate(testContext))
        assertEquals(42, TestFeatures.intFlag.evaluate(testContext))
        assertEquals(3.14, TestFeatures.doubleFlag.evaluate(testContext))

        // Clear one override - others remain
        testNamespace.clearOverride(TestFeatures.boolFlag)
        assertEquals(false, TestFeatures.boolFlag.evaluate(testContext))
        assertEquals("override", TestFeatures.stringFlag.evaluate(testContext))
        assertEquals(42, TestFeatures.intFlag.evaluate(testContext))
        assertEquals(3.14, TestFeatures.doubleFlag.evaluate(testContext))

        // Clear all configure
        testNamespace.clearAllOverrides()
        assertEquals(false, TestFeatures.boolFlag.evaluate(testContext))
        assertEquals("default", TestFeatures.stringFlag.evaluate(testContext))
        assertEquals(0, TestFeatures.intFlag.evaluate(testContext))
        assertEquals(0.0, TestFeatures.doubleFlag.evaluate(testContext))
    }

    @Test
    fun `withOverrides supports multiple flags at once`() {
        val testNamespace = Namespace.Search
        val TestFeatures = object : FeatureContainer<Namespace.Search>(testNamespace) {
            val flagA by boolean<Context>(default = false)
            val flagB by string<Context>(default = "default")
            val flagC by integer<Context>(default = 0)
        }

        TestFeatures.testScoped(
            TestFeatures.flagA to true,
            TestFeatures.flagB to "test",
            TestFeatures.flagC to 100
        ) {
            assertEquals(true, TestFeatures.flagA.evaluate(testContext))
            assertEquals("test", TestFeatures.flagB.evaluate(testContext))
            assertEquals(100, TestFeatures.flagC.evaluate(testContext))
        }

        // All configure cleared after block
        assertEquals(false, TestFeatures.flagA.evaluate(testContext))
        assertEquals("default", TestFeatures.flagB.evaluate(testContext))
        assertEquals(0, TestFeatures.flagC.evaluate(testContext))
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
        assertEquals(true, Features1.myFlag.evaluate(testContext))
        assertEquals(false, Features2.myFlag.evaluate(testContext))

        // Clear doesn't affect other namespace
        namespace1.clearOverride(Features1.myFlag)
        assertEquals(false, Features1.myFlag.evaluate(testContext))
        assertEquals(false, Features2.myFlag.evaluate(testContext))
    }

    @Test
    fun `overrides work with string flags`() {
        val testNamespace = TestNamespace.test("string-override")
        val TestFeatures = object : FeatureContainer<TestNamespace>(testNamespace) {
            val apiEndpoint by string<Context>(default = "https://api.default.com")
        }

        assertEquals("https://api.default.com", TestFeatures.apiEndpoint.evaluate(testContext))

        testNamespace.setOverride(TestFeatures.apiEndpoint, "https://api.test.com")
        assertEquals("https://api.test.com", TestFeatures.apiEndpoint.evaluate(testContext))

        testNamespace.clearOverride(TestFeatures.apiEndpoint)
        assertEquals("https://api.default.com", TestFeatures.apiEndpoint.evaluate(testContext))
    }

    @Test
    fun `overrides work with int flags`() {
        val testNamespace = TestNamespace.test("int-override")
        val TestFeatures = object : FeatureContainer<TestNamespace>(testNamespace) {
            val maxRetries by integer<Context>(default = 3)
        }

        assertEquals(3, TestFeatures.maxRetries.evaluate(testContext))

        testNamespace.setOverride(TestFeatures.maxRetries, 10)
        assertEquals(10, TestFeatures.maxRetries.evaluate(testContext))

        testNamespace.clearOverride(TestFeatures.maxRetries)
        assertEquals(3, TestFeatures.maxRetries.evaluate(testContext))
    }

    @Test
    fun `overrides work with double flags`() {
        val testNamespace = TestNamespace.test("double-override")
        val TestFeatures = object : FeatureContainer<TestNamespace>(testNamespace) {
            val priceMultiplier by double<Context>(default = 1.0)
        }

        assertEquals(1.0, TestFeatures.priceMultiplier.evaluate(testContext))

        testNamespace.setOverride(TestFeatures.priceMultiplier, 1.5)
        assertEquals(1.5, TestFeatures.priceMultiplier.evaluate(testContext))

        testNamespace.clearOverride(TestFeatures.priceMultiplier)
        assertEquals(1.0, TestFeatures.priceMultiplier.evaluate(testContext))
    }

    @Test
    fun `overrides can be changed without clearing first`() {
        val testNamespace = TestNamespace.test("change-override")
        val TestFeatures = object : FeatureContainer<TestNamespace>(testNamespace) {
            val counter by integer<Context>(default = 0)
        }

        testNamespace.setOverride(TestFeatures.counter, 1)
        assertEquals(1, TestFeatures.counter.evaluate(testContext))

        testNamespace.setOverride(TestFeatures.counter, 2)
        assertEquals(2, TestFeatures.counter.evaluate(testContext))

        testNamespace.setOverride(TestFeatures.counter, 3)
        assertEquals(3, TestFeatures.counter.evaluate(testContext))

        testNamespace.clearOverride(TestFeatures.counter)
        assertEquals(0, TestFeatures.counter.evaluate(testContext))
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
                        val myFlag by integer<Context>(default = 0)
                    }

                    // Set override to unique value per test
                    testNamespace.setOverride(TestFeatures.myFlag, i)

                    // Verify we get the correct value
                    val result = TestFeatures.myFlag.evaluate(testContext)
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
            assertEquals(true, TestFeatures.myFlag.evaluate(testContext))
        }

        testNamespace.clearOverride(TestFeatures.myFlag)

        // After clearing, all evaluations return default
        repeat(5) {
            assertEquals(false, TestFeatures.myFlag.evaluate(testContext))
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
        assertEquals(false, TestFeatures.flag1.evaluate(testContext))
        assertEquals("default", TestFeatures.flag2.evaluate(testContext))
        assertEquals(0, TestFeatures.flag3.evaluate(testContext))
    }

    @Test
    fun `nested withOverride calls work correctly`() {
        val testNamespace = TestNamespace.test("nested-testScope")
        val TestFeatures = object : FeatureContainer<TestNamespace>(testNamespace) {
            val outerFlag by boolean<Context>(default = false)
            val innerFlag by string<Context>(default = "default")
        }

        testNamespace.withOverride(TestFeatures.outerFlag, true) {
            assertEquals(true, TestFeatures.outerFlag.evaluate(testContext))
            assertEquals("default", TestFeatures.innerFlag.evaluate(testContext))

            testNamespace.withOverride(TestFeatures.innerFlag, "nested") {
                assertEquals(true, TestFeatures.outerFlag.evaluate(testContext))
                assertEquals("nested", TestFeatures.innerFlag.evaluate(testContext))
            }

            // Inner override cleared
            assertEquals(true, TestFeatures.outerFlag.evaluate(testContext))
            assertEquals("default", TestFeatures.innerFlag.evaluate(testContext))
        }

        // Both configure cleared
        assertEquals(false, TestFeatures.outerFlag.evaluate(testContext))
        assertEquals("default", TestFeatures.innerFlag.evaluate(testContext))
    }
}
