package io.amichne.konditional.core

import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.fact.FactDomain
import io.amichne.konditional.context.fact.FactSchema
import io.amichne.konditional.context.fact.allOf
import io.amichne.konditional.context.fact.anyOf
import io.amichne.konditional.core.dsl.enableWhen
import io.amichne.konditional.fixtures.Platform
import io.amichne.konditional.fixtures.TestContext
import io.amichne.konditional.fixtures.TestEnvironment
import io.amichne.konditional.fixtures.TestFacts
import io.amichne.konditional.fixtures.TestNamespace
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NamespaceBehaviorTest {
    @Test
    fun `evaluation is deterministic for the same context and snapshot`() {
        val context = TestContext()

        val results = (1..50).map { CheckoutFlags.enabled.evaluate(context) }

        assertTrue(results.all { it })
    }

    @Test
    fun `same feature key in separate namespaces remains isolated`() {
        val context = TestContext()

        assertTrue(CheckoutFlags.enabled.evaluate(context))
        assertFalse(BillingFlags.enabled.evaluate(context))
    }

    @Test
    fun `fact predicates evaluate only for matching values`() {
        val prod = TestContext(environment = TestEnvironment.PROD)
        val stage = TestContext(environment = TestEnvironment.DEV)

        assertTrue(EnvironmentFlags.prodOnly.evaluate(prod))
        assertFalse(EnvironmentFlags.prodOnly.evaluate(stage))
    }

    private object CheckoutFlags : TestNamespace("checkout") {
        val enabled by boolean<TestContext>(default = false) {
            enableWhen { platform.oneOf(Platform.IOS) }
        }
    }

    private object BillingFlags : TestNamespace("billing") {
        val enabled by boolean<TestContext>(default = false)
    }

    @Test
    fun `a rule cannot use a foreign descriptor with the same wire id`() {
        val foreign = object : FactSchema() {
            val platform = single("platform", FactDomain.enumeration(Platform.entries) { it.name })
        }
        val failure = assertFailsWith<IllegalArgumentException> {
            object : Namespace<TestFacts>("shipping", TestFacts) {
                val dispatch by boolean<Context>(default = false) {
                    enableWhen { anyOf(foreign.platform.oneOf(Platform.IOS), environment.oneOf(TestEnvironment.PROD)) }
                }
            }
        }
        assertTrue("'platform'" in failure.message.orEmpty(), failure.message)
        assertTrue("dispatch" in failure.message.orEmpty(), failure.message)
    }

    @Test
    fun `the most specific eligible fact rule wins with declaration order for ties`() {
        val namespace = object : Namespace<TestFacts>("priority", TestFacts) {
            val message by string<TestContext>("default") {
                rule("general") { environment.oneOf(TestEnvironment.PROD) }
                rule("first") { allOf(environment.oneOf(TestEnvironment.PROD), platform.oneOf(Platform.IOS)) }
                rule("second") { allOf(platform.oneOf(Platform.IOS), environment.oneOf(TestEnvironment.PROD)) }
            }
        }
        assertEquals("first", namespace.message.evaluate(TestContext(environment = TestEnvironment.PROD)))
        assertEquals(
            "general",
            namespace.message.evaluate(TestContext(environment = TestEnvironment.PROD, platform = Platform.ANDROID)),
        )
        assertEquals("default", namespace.message.evaluate(TestContext()))
    }

    private object EnvironmentFlags : TestNamespace("environment") {
        val prodOnly by boolean<TestContext>(default = false) {
            enableWhen { environment.oneOf(TestEnvironment.PROD) }
        }
    }
}
