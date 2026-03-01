package io.amichne.konditional.dimensions

import io.amichne.konditional.api.axis
import io.amichne.konditional.api.axisValues
import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.axis.AxisValues
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.enable
import io.amichne.konditional.core.dsl.variant
import io.amichne.konditional.fixtures.TestAxes
import io.amichne.konditional.fixtures.TestContext
import io.amichne.konditional.fixtures.TestEnvironment
import io.amichne.konditional.fixtures.TestTenant
import io.amichne.konditional.fixtures.FeaturesWithAxis
import io.amichne.konditional.fixtures.environment
import io.amichne.konditional.fixtures.tenant
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Unit tests for AxisValues & AxisValuesBuilder (and legacy Dimensions).
 */
class AxisBuilderTest {

    @Test
    fun `axisValues builder returns EMPTY when no values set`() {
        val values = axisValues {}

        Assertions.assertSame(
            AxisValues.EMPTY,
            values,
            "Empty builder should return the shared EMPTY instance",
        )
        Assertions.assertTrue(values[TestAxes.Environment].isEmpty(), "No environment should be present")
        Assertions.assertTrue(values[TestAxes.Tenant].isEmpty(), "No tenant should be present")
    }

    @Test
    fun `axisValues builder stores and retrieves typed values`() {
        val values = axisValues {
            environment(TestEnvironment.DEV)
            tenant(TestTenant.SME)
        }

        Assertions.assertEquals(
            setOf(TestEnvironment.DEV),
            values[TestAxes.Environment],
            "Typed axis lookup should return environment value",
        )
        Assertions.assertEquals(
            setOf(TestTenant.SME),
            values[TestAxes.Tenant],
            "Typed axis lookup should return tenant value",
        )
    }

    @Test
    fun `axisValues builder accumulates multiple values for same axis`() {
        val values = axisValues {
            environment(TestEnvironment.DEV)
            environment(TestEnvironment.PROD)
            tenant(TestTenant.CONSUMER)
            tenant(TestTenant.ENTERPRISE)
        }

        Assertions.assertEquals(
            setOf(TestEnvironment.DEV, TestEnvironment.PROD),
            values[TestAxes.Environment],
            "Environment values should accumulate",
        )
        Assertions.assertEquals(
            setOf(TestTenant.CONSUMER, TestTenant.ENTERPRISE),
            values[TestAxes.Tenant],
            "Tenant values should accumulate",
        )
    }

    @Test
    fun `axisValues variant supports nullable control flow`() {
        val maybeTenant: TestTenant? = null
        val values = axisValues {
            variant {
                TestAxes.Environment { include(TestEnvironment.STAGE) }
                maybeTenant?.let { TestAxes.Tenant { include(it) } }
            }
        }

        Assertions.assertEquals(
            setOf(TestEnvironment.STAGE),
            values[TestAxes.Environment],
        )
        Assertions.assertTrue(values[TestAxes.Tenant].isEmpty(), "Tenant should be empty when null is passed")
    }

    @Test
    fun `context axis returns set of values`() {
        val ctx = TestContext(
            axisValues =
                axisValues {
                    environment(TestEnvironment.DEV)
                    environment(TestEnvironment.PROD)
                },
        )

        Assertions.assertEquals(
            setOf(TestEnvironment.DEV, TestEnvironment.PROD),
            ctx.axis<TestEnvironment>(),
        )
    }

    @Test
    fun `axis constraints match when any value is allowed`() {
        val ctx = TestContext(
            axisValues =
                axisValues {
                    environment(TestEnvironment.DEV)
                    environment(TestEnvironment.PROD)
                },
        )

        val enabled = FeaturesWithAxis.envScopedFlag.evaluate(ctx)

        Assertions.assertTrue(enabled)
    }

    @Test
    fun `variant block requires at least one include call`() {
        val error = Assertions.assertThrows(IllegalArgumentException::class.java) {
            axisValues {
                variant {
                    TestAxes.Environment { }
                }
            }
        }
        Assertions.assertTrue(error.message.orEmpty().contains("must include at least one value"))
    }

    @Test
    fun `multiple variant calls merge values for the same axis`() {
        val namespace = object : Namespace.TestNamespaceFacade("axis-variant-merge") {
            val flag by boolean<TestContext>(default = false) {
                enable {
                    variant {
                        TestAxes.Environment { include(TestEnvironment.DEV) }
                    }
                    variant {
                        TestAxes.Environment { include(TestEnvironment.STAGE) }
                    }
                }
            }
        }

        val dev = TestContext(axisValues = axisValues { environment(TestEnvironment.DEV) })
        val stage = TestContext(axisValues = axisValues { environment(TestEnvironment.STAGE) })
        val prod = TestContext(axisValues = axisValues { environment(TestEnvironment.PROD) })

        Assertions.assertTrue(namespace.flag.evaluate(dev))
        Assertions.assertTrue(namespace.flag.evaluate(stage))
        Assertions.assertFalse(namespace.flag.evaluate(prod))
    }

    @Test
    fun `variant works inside anyOf blocks`() {
        val namespace = object : Namespace.TestNamespaceFacade("axis-variant-anyof") {
            val flag by boolean<TestContext>(default = false) {
                enable {
                    anyOf {
                        variant {
                            TestAxes.Environment { include(TestEnvironment.PROD) }
                        }
                        variant {
                            TestAxes.Tenant { include(TestTenant.ENTERPRISE) }
                        }
                    }
                }
            }
        }

        val envOnly = TestContext(axisValues = axisValues { environment(TestEnvironment.PROD) })
        val tenantOnly = TestContext(axisValues = axisValues { tenant(TestTenant.ENTERPRISE) })
        val none = TestContext(axisValues = axisValues { tenant(TestTenant.CONSUMER) })

        Assertions.assertTrue(namespace.flag.evaluate(envOnly))
        Assertions.assertTrue(namespace.flag.evaluate(tenantOnly))
        Assertions.assertFalse(namespace.flag.evaluate(none))
    }
}
