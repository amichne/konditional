package io.amichne.konditional.engine

import io.amichne.konditional.api.evaluate
import io.amichne.konditional.api.explain
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.disable
import io.amichne.konditional.core.dsl.enable
import io.amichne.konditional.core.dsl.rules.targeting.scopes.constrain
import io.amichne.konditional.core.instance.Configuration
import io.amichne.konditional.core.instance.ConfigurationMetadata
import io.amichne.konditional.core.registry.InMemoryNamespaceRegistry
import io.amichne.konditional.fixtures.TestContext
import io.amichne.konditional.fixtures.TestEnvironment
import io.amichne.konditional.internal.evaluation.EvaluationDiagnostics
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExplainDecisionsTest {

    @Test
    fun `RegistryDisabled decision returned when registry is disabled`() {
        val ns = object : Namespace.TestNamespaceFacade("explain-registry-disabled") {
            val flag by boolean<TestContext>(default = true) {
                enable { platforms(Platform.IOS) }
            }
        }

        ns.disableAll()

        val result = ns.flag.explain(TestContext(platform = Platform.IOS))

        assertIs<EvaluationDiagnostics.Decision.RegistryDisabled>(result.decision)
        // Value should come from declared default (true), not the loaded flag value
        assertEquals(true, result.value)
        assertEquals("explain-registry-disabled", result.namespaceId)
    }

    @Test
    fun `Inactive decision returned when flag definition is inactive`() {
        val ns = object : Namespace.TestNamespaceFacade("explain-inactive") {
            val flag by boolean<Context>(default = false) {
                active { false }
            }
        }

        val result = ns.flag.explain(TestContext())

        assertIs<EvaluationDiagnostics.Decision.Inactive>(result.decision)
        assertFalse(result.value)
    }

    @Test
    fun `Rule decision returned when a rule matches`() {
        val ns = object : Namespace.TestNamespaceFacade("explain-rule") {
            val flag by boolean<TestContext>(default = false) {
                enable { platforms(Platform.IOS) }
            }
        }

        val result = ns.flag.explain(TestContext(platform = Platform.IOS))

        val decision = assertIs<EvaluationDiagnostics.Decision.Rule>(result.decision)
        assertTrue(result.value)
        assertNotNull(decision.matched)
        assertNull(decision.skippedByRollout)
        assertEquals(ns.id.value, result.namespaceId)
        assertEquals(ns.flag.key, result.featureKey)
    }

    @Test
    fun `Default decision returned when no rule matches`() {
        val ns = object : Namespace.TestNamespaceFacade("explain-default") {
            val flag by boolean<TestContext>(default = false) {
                enable { platforms(Platform.IOS) }
            }
        }

        val result = ns.flag.explain(TestContext(platform = Platform.ANDROID))

        val decision = assertIs<EvaluationDiagnostics.Decision.Default>(result.decision)
        assertFalse(result.value)
        assertNull(decision.skippedByRollout)
    }

    @Test
    fun `Default decision with skippedByRollout when rampUp excludes context`() {
        // Use rampUp(0) so no one gets in, but rule criteria matches
        val ns = object : Namespace.TestNamespaceFacade("explain-rampup-skip") {
            val flag by boolean<TestContext>(default = false) {
                rule(true) {
                    platforms(Platform.IOS)
                    rampUp { 0.0 }
                }
            }
        }

        val result = ns.flag.explain(TestContext(platform = Platform.IOS))

        val decision = assertIs<EvaluationDiagnostics.Decision.Default>(result.decision)
        assertFalse(result.value)
        assertNotNull(decision.skippedByRollout)
    }

    @Test
    fun `Rule decision includes correct rule explanation fields`() {
        val ns = object : Namespace.TestNamespaceFacade("explain-rule-details") {
            val flag by boolean<TestContext>(default = false) {
                enable {
                    platforms(Platform.IOS)
                    note("ios-only")
                }
            }
        }

        val result = ns.flag.explain(TestContext(platform = Platform.IOS))

        val decision = assertIs<EvaluationDiagnostics.Decision.Rule>(result.decision)
        val ruleExplanation = decision.matched.rule
        assertEquals("ios-only", ruleExplanation.note)
        assertTrue(ruleExplanation.platforms.contains(Platform.IOS.id))
        assertTrue(ruleExplanation.ruleId.startsWith("rule::"))
        assertTrue(ruleExplanation.totalSpecificity >= 1)
    }

    @Test
    fun `explain and evaluate return consistent values`() {
        val ns = object : Namespace.TestNamespaceFacade("explain-consistency") {
            val flag by boolean<TestContext>(default = false) {
                enable { platforms(Platform.IOS) }
                disable { platforms(Platform.ANDROID) }
            }
        }

        val iosCtx = TestContext(platform = Platform.IOS)
        val androidCtx = TestContext(platform = Platform.ANDROID)

        assertEquals(ns.flag.evaluate(iosCtx), ns.flag.explain(iosCtx).value)
        assertEquals(ns.flag.evaluate(androidCtx), ns.flag.explain(androidCtx).value)
    }

    @Test
    fun `explain captures configVersion from registry metadata`() {
        val registry = InMemoryNamespaceRegistry(namespaceId = "explain-version")
        val ns = object : Namespace.TestNamespaceFacade(
            id = "explain-version",
            registry = registry,
        ) {
            val flag by boolean<Context>(default = false)
        }

        registry.load(
            Configuration(
                flags = mapOf(ns.flag to FlagDefinition(feature = ns.flag, bounds = emptyList(), defaultValue = false)),
                metadata = ConfigurationMetadata(version = "v42"),
            ),
        )

        val result = ns.flag.explain(TestContext())
        assertEquals("v42", result.configVersion)
    }

    @Test
    fun `axis-targeted rule produces correct Rule decision`() {
        val ns = object : Namespace.TestNamespaceFacade("explain-axis") {
            val flag by boolean<TestContext>(default = false) {
                enable { constrain(TestEnvironment.PROD) }
            }
        }

        val prodContext = TestContext(axes = io.amichne.konditional.context.axis.axes(TestEnvironment.PROD))
        val devContext = TestContext(axes = io.amichne.konditional.context.axis.axes(TestEnvironment.DEV))

        assertIs<EvaluationDiagnostics.Decision.Rule>(ns.flag.explain(prodContext).decision)
        assertIs<EvaluationDiagnostics.Decision.Default>(ns.flag.explain(devContext).decision)
    }
}
