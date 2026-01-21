package io.amichne.konditional.dimensions

import io.amichne.konditional.api.axis
import io.amichne.konditional.api.axisValues
import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.axis.AxisValues
import io.amichne.konditional.core.dsl.unaryPlus
import io.amichne.konditional.fixtures.TestAxes
import io.amichne.konditional.fixtures.TestEnvironment
import io.amichne.konditional.fixtures.TestTenant
import io.amichne.konditional.fixtures.TestContext
import io.amichne.konditional.fixtures.FeaturesWithAxis
import io.amichne.konditional.fixtures.environment
import io.amichne.konditional.fixtures.tenant
import io.amichne.konditional.internal.builders.AxisValuesBuilder
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
    fun `axisValues unary plus sets values`() {
        @Suppress("UnusedExpression")
        TestAxes.Environment
        @Suppress("UnusedExpression")
        TestAxes.Tenant

        val values = axisValues {
            +TestEnvironment.DEV
            +TestTenant.CONSUMER
        }

        Assertions.assertEquals(
            setOf(TestEnvironment.DEV),
            values[TestAxes.Environment],
            "Unary plus should set environment value",
        )
        Assertions.assertEquals(
            setOf(TestTenant.CONSUMER),
            values[TestAxes.Tenant],
            "Unary plus should set tenant value",
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
    fun `axisValues setIfNotNull skips null values`() {
        val builder = AxisValuesBuilder()
        builder[TestAxes.Environment] = TestEnvironment.STAGE
        builder.setIfNotNull(TestAxes.Tenant, null)

        val values = builder.build()

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
}
