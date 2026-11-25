package io.amichne.konditional.core

import io.amichne.konditional.api.evaluate
import io.amichne.konditional.api.explain
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Rollout
import io.amichne.konditional.context.fact.Condition
import io.amichne.konditional.context.fact.FactDomain
import io.amichne.konditional.context.fact.FactSchema
import io.amichne.konditional.context.fact.FactSpec
import io.amichne.konditional.context.fact.Facts
import io.amichne.konditional.context.fact.SingleFact
import io.amichne.konditional.context.fact.allOf
import io.amichne.konditional.context.fact.anyOf
import io.amichne.konditional.context.fact.named
import io.amichne.konditional.context.fact.not
import io.amichne.konditional.core.dsl.disableWhen
import io.amichne.konditional.core.dsl.enable
import io.amichne.konditional.core.dsl.enableWhen
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.fixtures.AppLocale
import io.amichne.konditional.fixtures.Platform
import io.amichne.konditional.fixtures.TestContext
import io.amichne.konditional.fixtures.TestEnvironment
import io.amichne.konditional.fixtures.TestFacts
import io.amichne.konditional.fixtures.TestNamespace
import io.amichne.konditional.internal.evaluation.EvaluationDiagnostics.Decision
import io.amichne.konditional.internal.evaluation.EvaluationDiagnostics.TargetingNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ConditionRuleTest {
    private interface HasPlatform {
        val platform: SingleFact<Platform>
    }

    private class DeviceFacts : FactSchema(), HasPlatform {
        override val platform = single("platform", FactDomain.enumeration(Platform.entries) { it.name })
    }

    private fun on(vararg platforms: Platform): Condition<TestFacts> = {
        platform.oneOf(platforms.first(), *platforms.drop(1).toTypedArray())
    }

    private fun inEnvironment(environment: TestEnvironment): Condition<TestFacts> = {
        this.environment.oneOf(environment)
    }

    private val contexts = listOf(
        TestContext(),
        TestContext(environment = TestEnvironment.PROD),
        TestContext(environment = TestEnvironment.PROD, platform = Platform.ANDROID),
        TestContext(environment = TestEnvironment.PROD, locale = AppLocale.CANADA),
    )

    @Test
    fun `a composed condition and the equal explicit expression declare the same flag`() {
        val composed: Condition<TestFacts> = allOf(inEnvironment(TestEnvironment.PROD), anyOf(on(Platform.IOS), on(Platform.ANDROID)))
        val namespace = object : TestNamespace("equivalence") {
            val explicit by string<Context>("default") {
                rule("general") { locale.oneOf(AppLocale.CANADA) }
                rule("matched") {
                    allOf(
                        environment.oneOf(TestEnvironment.PROD),
                        anyOf(platform.oneOf(Platform.IOS), platform.oneOf(Platform.ANDROID)),
                    )
                }
            }
            val reused by string<Context>("default") {
                rule("general") { locale.oneOf(AppLocale.CANADA) }
                rule("matched", composed)
            }
        }

        for (context in contexts + Context()) {
            val explicit = namespace.explicit.explain(context)
            val reused = namespace.reused.explain(context)
            assertEquals(explicit.value, reused.value)
            val expected = (explicit.decision as? Decision.Rule)?.matched
            val actual = (reused.decision as? Decision.Rule)?.matched
            assertEquals(expected?.targeting, actual?.targeting)
            assertEquals(expected?.totalSpecificity, actual?.totalSpecificity)
        }
        assertEquals("default", namespace.reused.evaluate(Context()))
    }

    @Test
    fun `a condition is bound once and never runs during evaluation`() {
        var bindings = 0
        val counted: Condition<TestFacts> = {
            bindings++
            platform.oneOf(Platform.IOS)
        }
        val namespace = object : TestNamespace("bind-once") {
            val enabled by boolean<Context>(default = false) {
                enableWhen(allOf(counted, inEnvironment(TestEnvironment.DEV)))
            }
        }
        assertEquals(1, bindings)
        repeat(3) { assertTrue(namespace.enabled.evaluate(TestContext())) }
        namespace.enabled.explain(TestContext())
        assertEquals(1, bindings)
    }

    @Test
    fun `a label reaches the explanation without changing the decision`() {
        val pilot = named("pilot", allOf(on(Platform.IOS), inEnvironment(TestEnvironment.PROD)))
        val namespace = object : TestNamespace("labels") {
            val enabled by boolean<Context>(default = false) { enableWhen(pilot) }
        }
        val matched = assertIs<Decision.Rule>(namespace.enabled.explain(productionContext()).decision).matched
        val expected = FactSpec.Named(
            "pilot",
            FactSpec.All(
                listOf(FactSpec.OneOf("platform", listOf("IOS")), FactSpec.OneOf("environment", listOf("PROD"))),
            ),
        )
        assertEquals(TargetingNode.All(listOf(TargetingNode.Facts(expected))), matched.targeting)
        assertEquals(2, matched.totalSpecificity)
    }

    @Test
    fun `repeating a condition does not outrank a more specific rule`() {
        val prod = inEnvironment(TestEnvironment.PROD)
        val namespace = object : TestNamespace("no-padding") {
            val message by string<Context>("default") {
                rule("padded", allOf(prod, prod, named("again", prod)))
                rule("specific", allOf(prod, on(Platform.IOS)))
            }
        }
        assertEquals("specific", namespace.message.evaluate(productionContext()))
        assertEquals("padded", namespace.message.evaluate(productionContext().copy(platform = Platform.ANDROID)))
    }

    @Test
    fun `an empty group fails the declaration instead of matching everyone`() {
        val assembled = emptyList<Condition<TestFacts>>()
        assertFailsWith<IllegalArgumentException> {
            object : TestNamespace("empty-all") {
                val enabled by boolean<Context>(default = false) { enableWhen(allOf(assembled)) }
            }
        }
        assertFailsWith<IllegalArgumentException> {
            object : TestNamespace("empty-any") {
                val enabled by boolean<Context>(default = false) { enableWhen(anyOf(assembled)) }
            }
        }
    }

    @Test
    fun `a negated condition needs the fact to be supplied`() {
        val namespace = object : TestNamespace("negation") {
            val enabled by boolean<Context>(default = false) {
                enableWhen(not(on(Platform.IOS)))
            }
        }
        assertTrue(namespace.enabled.evaluate(TestContext(platform = Platform.ANDROID)))
        assertFalse(namespace.enabled.evaluate(TestContext(platform = Platform.IOS)))
        assertFalse(namespace.enabled.evaluate(Context()))
    }

    @Test
    fun `rule settings are arguments and an unconditional rule ranks last`() {
        val tester = StableId.of("tester")
        val namespace = object : TestNamespace("settings") {
            val enabled by boolean<TestContext>(default = false) {
                enable(rollout = Rollout.of(0), allowlist = setOf(tester), note = "testers only")
                disableWhen(on(Platform.ANDROID))
            }
        }
        assertTrue(namespace.enabled.evaluate(TestContext(stableId = tester)))
        assertFalse(namespace.enabled.evaluate(TestContext(stableId = StableId.of("someone-else"))))
        val android = namespace.enabled.explain(TestContext(stableId = tester, platform = Platform.ANDROID))
        assertFalse(android.value)
        assertEquals(1, assertIs<Decision.Rule>(android.decision).matched.totalSpecificity)
        val unconditional = assertIs<Decision.Rule>(namespace.enabled.explain(TestContext(stableId = tester)).decision)
        assertEquals("testers only", unconditional.matched.note)
        assertEquals(TargetingNode.All(emptyList()), unconditional.matched.targeting)
    }

    @Test
    fun `named and inline conditions accept the same settings`() {
        val namespace = object : TestNamespace("setting-forms") {
            val inline by boolean<TestContext>(default = false) {
                enableWhen(rollout = Rollout.of(0), note = "inline") { platform.oneOf(Platform.IOS) }
            }
            val named by string<TestContext>("default") {
                rule("ios", on(Platform.IOS), rollout = Rollout.of(100), note = "named")
                rule("prod", note = "inline") { environment.oneOf(TestEnvironment.PROD) }
            }
        }
        assertFalse(namespace.inline.evaluate(TestContext()))
        val skipped = assertIs<Decision.Default>(namespace.inline.explain(TestContext()).decision)
        assertEquals("inline", skipped.skippedByRollout?.note)
        assertEquals("ios", namespace.named.evaluate(TestContext()))
        assertEquals("prod", namespace.named.evaluate(productionContext().copy(platform = Platform.ANDROID)))
    }

    @Test
    fun `a condition that requires one capability fits a schema that supplies it`() {
        val facts = DeviceFacts()
        val onIos: Condition<HasPlatform> = { platform.oneOf(Platform.IOS) }
        val namespace = object : Namespace<DeviceFacts>("capability", facts) {
            val enabled by boolean<Context>(default = false) { enableWhen(onIos) }
        }
        fun device(os: Platform) = Context(Facts.of(facts.platform.value(os)))

        assertTrue(namespace.enabled.evaluate(device(Platform.IOS)))
        assertFalse(namespace.enabled.evaluate(device(Platform.ANDROID)))
        // TestContext supplies TestFacts.platform, which is a different declaration.
        assertFalse(namespace.enabled.evaluate(TestContext(platform = Platform.IOS)))
    }

    private fun productionContext() = TestContext(environment = TestEnvironment.PROD)
}
