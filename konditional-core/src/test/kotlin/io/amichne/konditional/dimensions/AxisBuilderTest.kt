package io.amichne.konditional.dimensions

import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.axis.Axis
import io.amichne.konditional.context.axis.axes
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.enable
import io.amichne.konditional.core.dsl.rules.targeting.scopes.constrain
import io.amichne.konditional.fixtures.FeaturesWithAxis
import io.amichne.konditional.fixtures.TestContext
import io.amichne.konditional.fixtures.TestEnvironment
import io.amichne.konditional.fixtures.TestTenant
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Unit tests for the retained axis entrypoints: [axes] and [constrain].
 */
class AxisBuilderTest {

    @Test
    fun `axes factory stores and retrieves typed values`() {
        val values = axes(TestEnvironment.DEV, TestTenant.SME)

        Assertions.assertEquals(
            setOf(TestEnvironment.DEV),
            values[Axis.of<TestEnvironment>()],
            "Typed axis lookup should return environment value",
        )
        Assertions.assertEquals(
            setOf(TestTenant.SME),
            values[Axis.of<TestTenant>()],
            "Typed axis lookup should return tenant value",
        )
    }

    @Test
    fun `axes factory accumulates multiple values for same axis`() {
        val values = axes(
            TestEnvironment.DEV,
            TestEnvironment.PROD,
            TestTenant.CONSUMER,
            TestTenant.ENTERPRISE,
        )

        Assertions.assertEquals(
            setOf(TestEnvironment.DEV, TestEnvironment.PROD),
            values[Axis.of<TestEnvironment>()],
            "Environment values should accumulate",
        )
        Assertions.assertEquals(
            setOf(TestTenant.CONSUMER, TestTenant.ENTERPRISE),
            values[Axis.of<TestTenant>()],
            "Tenant values should accumulate",
        )
    }

    @Test
    fun `axes supports nullable control flow via Axes EMPTY fallback`() {
        val maybeTenant: TestTenant? = null
        val values = maybeTenant
            ?.let { axes(TestEnvironment.STAGE, it) }
            ?: axes(TestEnvironment.STAGE)

        Assertions.assertEquals(
            setOf(TestEnvironment.STAGE),
            values[Axis.of<TestEnvironment>()],
        )
        Assertions.assertTrue(values[Axis.of<TestTenant>()].isEmpty(), "Tenant should be empty when null is passed")
    }

    @Test
    fun `context axis returns set of values`() {
        val ctx = TestContext(
            axes = axes(TestEnvironment.DEV, TestEnvironment.PROD),
        )

        Assertions.assertEquals(
            setOf(TestEnvironment.DEV, TestEnvironment.PROD),
            ctx.axes[Axis.of<TestEnvironment>()],
        )
    }

    @Test
    fun `axis constraints match when any value is allowed`() {
        val ctx = TestContext(
            axes = axes(TestEnvironment.DEV, TestEnvironment.PROD),
        )

        val enabled = FeaturesWithAxis.envScopedFlag.evaluate(ctx)

        Assertions.assertTrue(enabled)
    }

    @Test
    fun `multiple constrain calls merge values for the same axis`() {
        val namespace = object : Namespace.TestNamespaceFacade("axis-constrain-merge") {
            val flag by boolean<TestContext>(default = false) {
                enable {
                    constrain(TestEnvironment.DEV)
                    constrain(TestEnvironment.STAGE)
                }
            }
        }

        val dev = TestContext(axes = axes(TestEnvironment.DEV))
        val stage = TestContext(axes = axes(TestEnvironment.STAGE))
        val prod = TestContext(axes = axes(TestEnvironment.PROD))

        Assertions.assertTrue(namespace.flag.evaluate(dev))
        Assertions.assertTrue(namespace.flag.evaluate(stage))
        Assertions.assertFalse(namespace.flag.evaluate(prod))
    }

    @Test
    fun `constrain works inside anyOf blocks`() {
        val namespace = object : Namespace.TestNamespaceFacade("axis-constrain-anyof") {
            val flag by boolean<TestContext>(default = false) {
                enable {
                    anyOf {
                        constrain(TestEnvironment.PROD)
                        constrain(TestTenant.ENTERPRISE)
                    }
                }
            }
        }

        val envOnly = TestContext(axes = axes(TestEnvironment.PROD))
        val tenantOnly = TestContext(axes = axes(TestTenant.ENTERPRISE))
        val none = TestContext(axes = axes(TestTenant.CONSUMER))

        Assertions.assertTrue(namespace.flag.evaluate(envOnly))
        Assertions.assertTrue(namespace.flag.evaluate(tenantOnly))
        Assertions.assertFalse(namespace.flag.evaluate(none))
    }
}
