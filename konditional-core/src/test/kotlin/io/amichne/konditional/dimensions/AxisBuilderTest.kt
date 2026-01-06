package io.amichne.konditional.dimensions

import io.amichne.konditional.api.axisValues
import io.amichne.konditional.context.axis.AxisValues
import io.amichne.konditional.core.dsl.unaryPlus
import io.amichne.konditional.fixtures.TestAxes
import io.amichne.konditional.fixtures.TestEnvironment
import io.amichne.konditional.fixtures.TestTenant
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
        Assertions.assertNull(values[TestAxes.Environment], "No environment should be present")
        Assertions.assertNull(values[TestAxes.Tenant], "No tenant should be present")
    }

    @Test
    fun `axisValues builder stores and retrieves typed values`() {
        val values = axisValues {
            environment(TestEnvironment.DEV)
            tenant(TestTenant.SME)
        }

        Assertions.assertEquals(
            TestEnvironment.DEV,
            values[TestAxes.Environment],
            "Typed axis lookup should return environment value",
        )
        Assertions.assertEquals(
            TestTenant.SME,
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
            TestEnvironment.DEV,
            values[TestAxes.Environment],
            "Unary plus should set environment value",
        )
        Assertions.assertEquals(
            TestTenant.CONSUMER,
            values[TestAxes.Tenant],
            "Unary plus should set tenant value",
        )
    }

    @Test
    fun `axisValues builder overrides previous value for same axis`() {
        val values = axisValues {
            environment(TestEnvironment.DEV)
            environment(TestEnvironment.PROD)   // override
            tenant(TestTenant.CONSUMER)
            tenant(TestTenant.ENTERPRISE)       // override
        }

        Assertions.assertEquals(
            TestEnvironment.PROD,
            values[TestAxes.Environment],
            "Last value for Environment axis must win",
        )
        Assertions.assertEquals(
            TestTenant.ENTERPRISE,
            values[TestAxes.Tenant],
            "Last value for Tenant axis must win",
        )
    }

    @Test
    fun `axisValues setIfNotNull skips null values`() {
        val builder = AxisValuesBuilder()
        builder[TestAxes.Environment] = TestEnvironment.STAGE
        builder.setIfNotNull(TestAxes.Tenant, null)

        val values = builder.build()

        Assertions.assertEquals(
            TestEnvironment.STAGE,
            values[TestAxes.Environment],
        )
        Assertions.assertNull(
            values[TestAxes.Tenant],
            "Tenant should not be present when null is passed to setIfNotNull",
        )
    }
}
