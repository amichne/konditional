package io.amichne.konditional.dimensions

import io.amichne.konditional.context.axis.Axis
import io.amichne.konditional.context.axis.axes
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
    fun `context axes getter returns typed values`() {
        val ctx = TestContext(axes = axes(TestEnvironment.STAGE, TestTenant.ENTERPRISE))

        Assertions.assertEquals(
            setOf(TestEnvironment.STAGE),
            ctx.axes[Axis.of<TestEnvironment>()],
        )
        Assertions.assertEquals(
            setOf(TestTenant.ENTERPRISE),
            ctx.axes[Axis.of<TestTenant>()],
        )
    }

    @Test
    fun `context axes getter returns empty set for missing axis`() {
        val ctx = TestContext(axes = axes(TestEnvironment.PROD))

        Assertions.assertTrue(ctx.axes[Axis.of<TestTenant>()].isEmpty(), "Tenant should be empty when not set")
    }

}
