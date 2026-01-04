package io.amichne.konditional.core

import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.result.getOrThrow
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
 * - Overrides bypassing rules and rampUp logic
 */
class TestNamespaceOverridesTest {

    private val testContext = Context(
        locale = AppLocale.UNITED_STATES,
        platform = Platform.ANDROID,
        appVersion = Version.parse("1.0.0").getOrThrow(),
        stableId = TestStableId
    )

    @Test
    fun `basic override sets flag to specific value`() {
        val testNamespace = object : Namespace.TestNamespaceFacade("basic-override") {
            val myFlag by boolean<Context>(default = false)
        }

        // Without override, returns default
        assertEquals(false, testNamespace.myFlag.evaluate(testContext))

        // Within scope - override is active
        testNamespace.withOverride(testNamespace.myFlag, true) {
            assertEquals(true, testNamespace.myFlag.evaluate(testContext))
        }

        // After scope - back to default
        assertEquals(false, testNamespace.myFlag.evaluate(testContext))
    }

    @Test
    fun `override bypasses rules and rollout logic`() {
        val testNamespace = object : Namespace.TestNamespaceFacade("override-bypasses-rules") {
            val myFlag by boolean<Context>(default = false) {
                // Rule that would normally make this true for Android
                rule(true) { android() }
            }
        }

        // Normal evaluation follows rule - returns true for Android
        assertEquals(true, testNamespace.myFlag.evaluate(testContext))

        // Override to false - bypasses rule
        testNamespace.withOverride(testNamespace.myFlag, false) {
            assertEquals(false, testNamespace.myFlag.evaluate(testContext))
        }

        // After override - rule applies again
        assertEquals(true, testNamespace.myFlag.evaluate(testContext))
    }

    @Test
    fun `scoped override with automatic cleanup`() {
        val testNamespace = object : Namespace.TestNamespaceFacade("scoped-override") {
            val myFlag by boolean<Context>(default = false)
        }

        // Before override
        assertEquals(false, testNamespace.myFlag.evaluate(testContext))

        // Within scope - override is active
        val result = testNamespace.withOverride(testNamespace.myFlag, true) {
            assertEquals(true, testNamespace.myFlag.evaluate(testContext))
            "success"
        }
        assertEquals("success", result)

        // After scope - override is cleared
        assertEquals(false, testNamespace.myFlag.evaluate(testContext))
    }

    @Test
    fun `scoped override cleans up even on exception`() {
        val testNamespace = object : Namespace.TestNamespaceFacade("scoped-override-exception") {
            val myFlag by boolean<Context>(default = false)
        }

        // Before override
        assertEquals(false, testNamespace.myFlag.evaluate(testContext))

        // Exception thrown in scope
        assertThrows(RuntimeException::class.java) {
            testNamespace.withOverride(testNamespace.myFlag, true) {
                assertEquals(true, testNamespace.myFlag.evaluate(testContext))
                throw RuntimeException("Test exception")
            }
        }

        // Override is still cleared despite exception
        assertEquals(false, testNamespace.myFlag.evaluate(testContext))
    }

    @Test
    fun `multiple overrides using withOverrides`() {
        val testNamespace = object : Namespace.TestNamespaceFacade("multiple-overrides") {
            val boolFlag by boolean<Context>(default = false)
            val stringFlag by string<Context>(default = "default")
            val intFlag by integer<Context>(default = 0)
            val doubleFlag by double<Context>(default = 0.0)
        }

        // All defaults before
        assertEquals(false, testNamespace.boolFlag.evaluate(testContext))
        assertEquals("default", testNamespace.stringFlag.evaluate(testContext))
        assertEquals(0, testNamespace.intFlag.evaluate(testContext))
        assertEquals(0.0, testNamespace.doubleFlag.evaluate(testContext))

        // All overrides active within scope
        testNamespace.withOverrides(
            testNamespace.boolFlag to true,
            testNamespace.stringFlag to "override",
            testNamespace.intFlag to 42,
            testNamespace.doubleFlag to 3.14
        ) {
            assertEquals(true, testNamespace.boolFlag.evaluate(testContext))
            assertEquals("override", testNamespace.stringFlag.evaluate(testContext))
            assertEquals(42, testNamespace.intFlag.evaluate(testContext))
            assertEquals(3.14, testNamespace.doubleFlag.evaluate(testContext))
        }

        // All back to defaults after
        assertEquals(false, testNamespace.boolFlag.evaluate(testContext))
        assertEquals("default", testNamespace.stringFlag.evaluate(testContext))
        assertEquals(0, testNamespace.intFlag.evaluate(testContext))
        assertEquals(0.0, testNamespace.doubleFlag.evaluate(testContext))
    }

    @Test
    fun `overrides are isolated between test namespaces`() {
        val namespace1 = object : Namespace.TestNamespaceFacade("isolation-test-1") {
            val myFlag by boolean<Context>(default = false)
        }

        val namespace2 = object : Namespace.TestNamespaceFacade("isolation-test-2") {
            val myFlag by boolean<Context>(default = false)
        }

        // Override in namespace1 only
        namespace1.withOverride(namespace1.myFlag, true) {
            // namespace1 has override, namespace2 doesn't
            assertEquals(true, namespace1.myFlag.evaluate(testContext))
            assertEquals(false, namespace2.myFlag.evaluate(testContext))
        }

        // Both back to defaults after
        assertEquals(false, namespace1.myFlag.evaluate(testContext))
        assertEquals(false, namespace2.myFlag.evaluate(testContext))
    }

    @Test
    fun `overrides work with string flags`() {
        val testNamespace = object : Namespace.TestNamespaceFacade("string-override") {
            val apiEndpoint by string<Context>(default = "https://api.default.com")
        }

        assertEquals("https://api.default.com", testNamespace.apiEndpoint.evaluate(testContext))

        testNamespace.withOverride(testNamespace.apiEndpoint, "https://api.test.com") {
            assertEquals("https://api.test.com", testNamespace.apiEndpoint.evaluate(testContext))
        }

        assertEquals("https://api.default.com", testNamespace.apiEndpoint.evaluate(testContext))
    }

    @Test
    fun `overrides work with int flags`() {
        val testNamespace = object : Namespace.TestNamespaceFacade("int-override") {
            val maxRetries by integer<Context>(default = 3)
        }

        assertEquals(3, testNamespace.maxRetries.evaluate(testContext))

        testNamespace.withOverride(testNamespace.maxRetries, 10) {
            assertEquals(10, testNamespace.maxRetries.evaluate(testContext))
        }

        assertEquals(3, testNamespace.maxRetries.evaluate(testContext))
    }

    @Test
    fun `overrides work with double flags`() {
        val testNamespace = object : Namespace.TestNamespaceFacade("double-override") {
            val priceMultiplier by double<Context>(default = 1.0)
        }

        assertEquals(1.0, testNamespace.priceMultiplier.evaluate(testContext))

        testNamespace.withOverride(testNamespace.priceMultiplier, 1.5) {
            assertEquals(1.5, testNamespace.priceMultiplier.evaluate(testContext))
        }

        assertEquals(1.0, testNamespace.priceMultiplier.evaluate(testContext))
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
                    val testNamespace = object : Namespace.TestNamespaceFacade("parallel-test-$i") {
                        val myFlag by integer<Context>(default = 0)
                    }

                    // Override to unique value per test
                    testNamespace.withOverride(testNamespace.myFlag, i) {
                        // Verify we get the correct value
                        val result = testNamespace.myFlag.evaluate(testContext)
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
        val testNamespace = object : Namespace.TestNamespaceFacade("multiple-evaluations") {
            val myFlag by boolean<Context>(default = false)
        }

        testNamespace.withOverride(testNamespace.myFlag, true) {
            // Multiple evaluations should all return the override value
            repeat(5) {
                assertEquals(true, testNamespace.myFlag.evaluate(testContext))
            }
        }

        // After scope, all evaluations return default
        repeat(5) {
            assertEquals(false, testNamespace.myFlag.evaluate(testContext))
        }
    }

    @Test
    fun `nested withOverride calls work correctly`() {
        val testNamespace = object : Namespace.TestNamespaceFacade("nested-overrides") {
            val outerFlag by boolean<Context>(default = false)
            val innerFlag by string<Context>(default = "default")
        }

        testNamespace.withOverride(testNamespace.outerFlag, true) {
            assertEquals(true, testNamespace.outerFlag.evaluate(testContext))
            assertEquals("default", testNamespace.innerFlag.evaluate(testContext))

            testNamespace.withOverride(testNamespace.innerFlag, "nested") {
                assertEquals(true, testNamespace.outerFlag.evaluate(testContext))
                assertEquals("nested", testNamespace.innerFlag.evaluate(testContext))
            }

            // Inner override cleared
            assertEquals(true, testNamespace.outerFlag.evaluate(testContext))
            assertEquals("default", testNamespace.innerFlag.evaluate(testContext))
        }

        // Both overrides cleared
        assertEquals(false, testNamespace.outerFlag.evaluate(testContext))
        assertEquals("default", testNamespace.innerFlag.evaluate(testContext))
    }

    @Test
    fun `nested override of same flag - inner takes precedence`() {
        val testNamespace = object : Namespace.TestNamespaceFacade("nested-same-flag") {
            val counter by integer<Context>(default = 0)
        }

        testNamespace.withOverride(testNamespace.counter, 1) {
            assertEquals(1, testNamespace.counter.evaluate(testContext))

            testNamespace.withOverride(testNamespace.counter, 2) {
                assertEquals(2, testNamespace.counter.evaluate(testContext))

                testNamespace.withOverride(testNamespace.counter, 3) {
                    assertEquals(3, testNamespace.counter.evaluate(testContext))
                }

                // Back to middle level
                assertEquals(2, testNamespace.counter.evaluate(testContext))
            }

            // Back to outer level
            assertEquals(1, testNamespace.counter.evaluate(testContext))
        }

        // Back to default
        assertEquals(0, testNamespace.counter.evaluate(testContext))
    }

    @Test
    fun `withOverrides cleans up in reverse order on exception`() {
        val testNamespace = object : Namespace.TestNamespaceFacade("exception-cleanup") {
            val flag1 by boolean<Context>(default = false)
            val flag2 by string<Context>(default = "default")
            val flag3 by integer<Context>(default = 0)
        }

        assertThrows(RuntimeException::class.java) {
            testNamespace.withOverrides(
                testNamespace.flag1 to true,
                testNamespace.flag2 to "override",
                testNamespace.flag3 to 42
            ) {
                assertEquals(true, testNamespace.flag1.evaluate(testContext))
                assertEquals("override", testNamespace.flag2.evaluate(testContext))
                assertEquals(42, testNamespace.flag3.evaluate(testContext))

                throw RuntimeException("Test exception")
            }
        }

        // All overrides cleaned up despite exception
        assertEquals(false, testNamespace.flag1.evaluate(testContext))
        assertEquals("default", testNamespace.flag2.evaluate(testContext))
        assertEquals(0, testNamespace.flag3.evaluate(testContext))
    }

    @Test
    fun `empty withOverrides is a no-op`() {
        val testNamespace = object : Namespace.TestNamespaceFacade("empty-overrides") {
            val myFlag by boolean<Context>(default = false)
        }

        testNamespace.withOverrides {
            // No overrides set
            assertEquals(false, testNamespace.myFlag.evaluate(testContext))
        }

        assertEquals(false, testNamespace.myFlag.evaluate(testContext))
    }
}
