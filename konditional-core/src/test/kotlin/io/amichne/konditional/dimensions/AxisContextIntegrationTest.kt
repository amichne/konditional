package io.amichne.konditional.dimensions

import io.amichne.konditional.api.axis
import io.amichne.konditional.api.axisValues
import io.amichne.konditional.core.dsl.unaryPlus
import io.amichne.konditional.fixtures.TestAxes
import io.amichne.konditional.fixtures.TestContext
import io.amichne.konditional.fixtures.TestEnvironment
import io.amichne.konditional.fixtures.TestTenant
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Unit tests for Context + axis values integration.
 */
class AxisContextIntegrationTest {

    @Test
    fun `context axis extension returns typed values`() {
        val values = axisValues {
            +TestEnvironment.STAGE
            +TestTenant.ENTERPRISE
        }

        val ctx = TestContext(axisValues = values)

        Assertions.assertEquals(
            setOf(TestEnvironment.STAGE),
            ctx.axis(TestAxes.Environment),
        )
        Assertions.assertEquals(
            setOf(TestTenant.ENTERPRISE),
            ctx.axis(TestAxes.Tenant),
        )
    }

    @Test
    fun `context axis type-based extension returns typed values`() {
        val values = axisValues {
            +TestEnvironment.STAGE
            +TestTenant.ENTERPRISE
        }

        val ctx = TestContext(axisValues = values)

        // Type-based access
        Assertions.assertEquals(
            setOf(TestEnvironment.STAGE),
            ctx.axis<TestEnvironment>(),
        )
        Assertions.assertEquals(
            setOf(TestTenant.ENTERPRISE),
            ctx.axis<TestTenant>(),
        )
    }

    @Test
    fun `context axis extension returns null for missing axis`() {
        val values = axisValues {
            +TestEnvironment.PROD
        }
        val ctx = TestContext(axisValues = values)

        Assertions.assertTrue(ctx.axis(TestAxes.Tenant).isEmpty(), "Tenant should be empty when not set")
        Assertions.assertTrue(ctx.axis<TestTenant>().isEmpty(), "Type-based access should also return empty")
    }

    @Test
    fun `axis values auto-register axis when not explicitly defined`() {
        val values = axisValues {
            +EphemeralEnvironment.PROD
        }

        val ctx = TestContext(axisValues = values)

        Assertions.assertEquals(
            setOf(EphemeralEnvironment.PROD),
            ctx.axis<EphemeralEnvironment>(),
        )
    }
}

private enum class EphemeralEnvironment(override val id: String) :
    io.amichne.konditional.context.axis.AxisValue<EphemeralEnvironment> {
    PROD("prod"),
}
