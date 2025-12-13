package io.amichne.konditional.fixtures.core

import io.amichne.konditional.core.Namespace

/**
 * TestNamespace-scoped namespace for isolated testing.
 *
 * TestNamespace namespaces provide complete isolation for unit and integration tests:
 * - Each test gets its own registry instance
 * - No state pollution between tests
 * - Safe for concurrent test execution
 * - Can use extreme/adversarial values without affecting other tests
 *
 * ## Usage
 *
 * ```kotlin
 * @Test
 * fun `test feature flag behavior`() {
 *     val testNamespace = Namespace.test("my-test")
 *
 *     val TestFeatures = object : FeatureContainer<Namespace.TestNamespace>(testNamespace) {
 *         val myFlag by boolean<Context>(default = false) {
 *             rule(true) { platforms(Platform.IOS) }
 *         }
 *     }
 *
 *     val result = TestFeatures.myFlag.evaluate(contextFn)
 *     assertEquals(true, result)
 * }
 * ```
 *
 * ## Automatic Cleanup
 *
 * TestNamespace namespaces are isolated instances - no manual cleanup is needed.
 * Each test creates its own namespace instance that is garbage collected
 * after the test completes.
 *
 * @property id Unique identifier for this test namespace (for debugging)
 */
class TestNamespace internal constructor(id: String) : Namespace.TestNamespaceFacade("test-$id") {

    companion object {
        /**
         * Creates a new test-scoped namespace with isolated registry.
         *
         * Each invocation creates a fresh namespace instance with its own registry,
         * ensuring complete isolation between tests.
         *
         * @param id Optional identifier for debugging (e.g., test name)
         * @return A new isolated [io.amichne.konditional.core.TestNamespace] namespace instance
         */
        fun test(id: String = "default"): TestNamespace = TestNamespace(id)
    }
}

fun test(id: String = "default"): TestNamespace = TestNamespace(id)
