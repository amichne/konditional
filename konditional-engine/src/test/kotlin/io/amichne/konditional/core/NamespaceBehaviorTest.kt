package io.amichne.konditional.core

import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.fact.FactDomain
import io.amichne.konditional.context.fact.SingleFact
import io.amichne.konditional.context.fact.allOf
import io.amichne.konditional.core.dsl.enable
import io.amichne.konditional.fixtures.Platform
import io.amichne.konditional.fixtures.TestContext
import io.amichne.konditional.fixtures.TestEnvironment
import io.amichne.konditional.fixtures.TestFacts
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

    private object CheckoutFlags : Namespace.TestNamespaceFacade("checkout", factSchema = TestFacts.schema) {
        val enabled by boolean<TestContext>(default = false) {
            enable { match(TestFacts.platform.oneOf(Platform.IOS)) }
        }
    }

    private object BillingFlags : Namespace.TestNamespaceFacade("billing") {
        val enabled by boolean<TestContext>(default = false)
    }

    @Test
    fun `a rule cannot use a foreign descriptor with the same wire id`() {
        val foreign = SingleFact("platform", FactDomain.enumeration(Platform.entries) { it.name })
        assertFailsWith<IllegalArgumentException> {
            object : Namespace("shipping", TestFacts.schema) {
                val dispatch by boolean<Context>(default = false) {
                    enable {
                        anyOf {
                            match(foreign.oneOf(Platform.IOS))
                            match(TestFacts.environment.oneOf(TestEnvironment.PROD))
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `an empty disjunction never enables a feature`() {
        val namespace = object : Namespace("empty-or") {
            val enabled by boolean<Context>(default = false) {
                enable { anyOf {} }
            }
        }
        assertFalse(namespace.enabled.evaluate(Context()))
    }

    @Test
    fun `the most specific eligible fact rule wins with declaration order for ties`() {
        val prod = TestFacts.environment.oneOf(TestEnvironment.PROD)
        val ios = TestFacts.platform.oneOf(Platform.IOS)
        val namespace = object : Namespace("priority", TestFacts.schema) {
            val message by string<TestContext>("default") {
                rule("general", prod)
                rule("first", allOf(prod, ios))
                rule("second", allOf(ios, prod))
            }
        }
        assertEquals("first", namespace.message.evaluate(TestContext(environment = TestEnvironment.PROD)))
        assertEquals("general", namespace.message.evaluate(TestContext(environment = TestEnvironment.PROD, platform = Platform.ANDROID)))
        assertEquals("default", namespace.message.evaluate(TestContext()))
    }

    private object EnvironmentFlags : Namespace.TestNamespaceFacade("environment", factSchema = TestFacts.schema) {
        val prodOnly by boolean<TestContext>(default = false) {
            enable { match(TestFacts.environment.oneOf(TestEnvironment.PROD)) }
        }
    }
}
