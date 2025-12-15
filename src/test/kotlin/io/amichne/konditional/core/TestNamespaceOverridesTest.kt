package io.amichne.konditional.core

import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.features.FeatureContainer
import io.amichne.konditional.core.result.getOrThrow
import io.amichne.konditional.fixtures.core.TestNamespace
import io.amichne.konditional.fixtures.core.id.TestStableId
import io.amichne.konditional.fixtures.core.withOverride
import io.amichne.konditional.fixtures.core.withOverrides
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Tests for TestNamespace override functionality.
 *
 * Demonstrates the simplified override API:
 * - Scoped overrides with automatic cleanup
 * - Multiple overrides
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

        // Without override, returns default
        assertEquals(false, TestFeatures.myFlag.evaluate(testContext))

        // Within scope - override is active
        testNamespace.withOverride(TestFeatures.myFlag, true) {
            assertEquals(true, TestFeatures.myFlag.evaluate(testContext))
        }

        // After scope - back to default
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
        testNamespace.withOverride(TestFeatures.myFlag, false) {
            assertEquals(false, TestFeatures.myFlag.evaluate(testContext))
        }

        // After override - rule applies again
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
    fun `multiple overrides using withOverrides`() {
        val testNamespace = TestNamespace.test("multiple-overrides")
        val TestFeatures = object : FeatureContainer<TestNamespace>(testNamespace) {
            val boolFlag by boolean<Context>(default = false)
            val stringFlag by string<Context>(default = "default")
            val intFlag by integer<Context>(default = 0)
            val doubleFlag by double<Context>(default = 0.0)
        }

        // All defaults before
        assertEquals(false, TestFeatures.boolFlag.evaluate(testContext))
        assertEquals("default", TestFeatures.stringFlag.evaluate(testContext))
        assertEquals(0, TestFeatures.intFlag.evaluate(testContext))
        assertEquals(0.0, TestFeatures.doubleFlag.evaluate(testContext))

        // All overrides active within scope
        testNamespace.withOverrides(
            TestFeatures.boolFlag to true,
            TestFeatures.stringFlag to "override",
            TestFeatures.intFlag to 42,
            TestFeatures.doubleFlag to 3.14
        ) {
            assertEquals(true, TestFeatures.boolFlag.evaluate(testContext))
            assertEquals("override", TestFeatures.stringFlag.evaluate(testContext))
            assertEquals(42, TestFeatures.intFlag.evaluate(testContext))
            assertEquals(3.14, TestFeatures.doubleFlag.evaluate(testContext))
        }

        // All back to defaults after
        assertEquals(false, TestFeatures.boolFlag.evaluate(testContext))
        assertEquals("default", TestFeatures.stringFlag.evaluate(testContext))
        assertEquals(0, TestFeatures.intFlag.evaluate(testContext))
        assertEquals(0.0, TestFeatures.doubleFlag.evaluate(testContext))
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

        // Override in namespace1 only
        namespace1.withOverride(Features1.myFlag, true) {
            // namespace1 has override, namespace2 doesn't
            assertEquals(true, Features1.myFlag.evaluate(testContext))
            assertEquals(false, Features2.myFlag.evaluate(testContext))
        }

        // Both back to defaults after
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

        testNamespace.withOverride(TestFeatures.apiEndpoint, "https://api.test.com") {
            assertEquals("https://api.test.com", TestFeatures.apiEndpoint.evaluate(testContext))
        }

        assertEquals("https://api.default.com", TestFeatures.apiEndpoint.evaluate(testContext))
    }

    @Test
    fun `overrides work with int flags`() {
        val testNamespace = TestNamespace.test("int-override")
        val TestFeatures = object : FeatureContainer<TestNamespace>(testNamespace) {
            val maxRetries by integer<Context>(default = 3)
        }

        assertEquals(3, TestFeatures.maxRetries.evaluate(testContext))

        testNamespace.withOverride(TestFeatures.maxRetries, 10) {
            assertEquals(10, TestFeatures.maxRetries.evaluate(testContext))
        }

        assertEquals(3, TestFeatures.maxRetries.evaluate(testContext))
    }

    @Test
    fun `overrides work with double flags`() {
        val testNamespace = TestNamespace.test("double-override")
        val TestFeatures = object : FeatureContainer<TestNamespace>(testNamespace) {
            val priceMultiplier by double<Context>(default = 1.0)
        }

        assertEquals(1.0, TestFeatures.priceMultiplier.evaluate(testContext))

        testNamespace.withOverride(TestFeatures.priceMultiplier, 1.5) {
            assertEquals(1.5, TestFeatures.priceMultiplier.evaluate(testContext))
        }

        assertEquals(1.0, TestFeatures.priceMultiplier.evaluate(testContext))
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

                    // Override to unique value per test
                    testNamespace.withOverride(TestFeatures.myFlag, i) {
                        // Verify we get the correct value
                        val result = TestFeatures.myFlag.evaluate(testContext)
                        if (result == i) {
                            successCount.incrementAndGet()
                        }
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        assert(latch.await(5, TimeUnit.SECONDS))
        assertEquals(10, successCount.get(), "All parallel tests should succeed without interference")
        executor.shutdown()
    }

    @Test
    fun `override affects all evaluations until scope exits`() {
        val testNamespace = TestNamespace.test("multiple-evaluations")
        val TestFeatures = object : FeatureContainer<TestNamespace>(testNamespace) {
            val myFlag by boolean<Context>(default = false)
        }

        testNamespace.withOverride(TestFeatures.myFlag, true) {
            // Multiple evaluations should all return the override value
            repeat(5) {
                assertEquals(true, TestFeatures.myFlag.evaluate(testContext))
            }
        }

        // After scope, all evaluations return default
        repeat(5) {
            assertEquals(false, TestFeatures.myFlag.evaluate(testContext))
        }
    }

    @Test
    fun `nested withOverride calls work correctly`() {
        val testNamespace = TestNamespace.test("nested-overrides")
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

        // Both overrides cleared
        assertEquals(false, TestFeatures.outerFlag.evaluate(testContext))
        assertEquals("default", TestFeatures.innerFlag.evaluate(testContext))
    }

    @Test
    fun `nested override of same flag - inner takes precedence`() {
        val testNamespace = TestNamespace.test("nested-same-flag")
        val TestFeatures = object : FeatureContainer<TestNamespace>(testNamespace) {
            val counter by integer<Context>(default = 0)
        }

        testNamespace.withOverride(TestFeatures.counter, 1) {
            assertEquals(1, TestFeatures.counter.evaluate(testContext))

            testNamespace.withOverride(TestFeatures.counter, 2) {
                assertEquals(2, TestFeatures.counter.evaluate(testContext))

                testNamespace.withOverride(TestFeatures.counter, 3) {
                    assertEquals(3, TestFeatures.counter.evaluate(testContext))
                }

                // Back to middle level
                assertEquals(2, TestFeatures.counter.evaluate(testContext))
            }

            // Back to outer level
            assertEquals(1, TestFeatures.counter.evaluate(testContext))
        }

        // Back to default
        assertEquals(0, TestFeatures.counter.evaluate(testContext))
    }

    @Test
    fun `withOverrides cleans up in reverse order on exception`() {
        val testNamespace = TestNamespace.test("exception-cleanup")
        val TestFeatures = object : FeatureContainer<TestNamespace>(testNamespace) {
            val flag1 by boolean<Context>(default = false)
            val flag2 by string<Context>(default = "default")
            val flag3 by integer<Context>(default = 0)
        }

        assertThrows(RuntimeException::class.java) {
            testNamespace.withOverrides(
                TestFeatures.flag1 to true,
                TestFeatures.flag2 to "override",
                TestFeatures.flag3 to 42
            ) {
                assertEquals(true, TestFeatures.flag1.evaluate(testContext))
                assertEquals("override", TestFeatures.flag2.evaluate(testContext))
                assertEquals(42, TestFeatures.flag3.evaluate(testContext))

                throw RuntimeException("Test exception")
            }
        }

        // All overrides cleaned up despite exception
        assertEquals(false, TestFeatures.flag1.evaluate(testContext))
        assertEquals("default", TestFeatures.flag2.evaluate(testContext))
        assertEquals(0, TestFeatures.flag3.evaluate(testContext))
    }

    @Test
    fun `empty withOverrides is a no-op`() {
        val testNamespace = TestNamespace.test("empty-overrides")
        val TestFeatures = object : FeatureContainer<TestNamespace>(testNamespace) {
            val myFlag by boolean<Context>(default = false)
        }

        testNamespace.withOverrides() {
            // No overrides set
            assertEquals(false, TestFeatures.myFlag.evaluate(testContext))
        }

        assertEquals(false, TestFeatures.myFlag.evaluate(testContext))
    }
}
