package io.amichne.konditional.dimensions

import io.amichne.konditional.context.Dimension
import io.amichne.konditional.context.dimension
import io.amichne.konditional.core.features.dimensions
import io.amichne.konditional.fixtures.TestAxes
import io.amichne.konditional.fixtures.TestContext
import io.amichne.konditional.fixtures.TestEnvironment
import io.amichne.konditional.fixtures.TestTenant
import io.amichne.konditional.fixtures.environment
import io.amichne.konditional.fixtures.tenant
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Unit tests for Context + dimensions integration.
 */
class DimensionsContextIntegrationTest {

    @Test
    fun `context dimension extension returns typed values`() {
        val dims = dimensions {
            environment(TestEnvironment.STAGE)
            tenant(TestTenant.ENTERPRISE)
        }

        val ctx = TestContext(dimensions = dims)

        Assertions.assertEquals(
            TestEnvironment.STAGE,
            ctx.dimension(TestAxes.Environment),
        )
        Assertions.assertEquals(
            TestTenant.ENTERPRISE,
            ctx.dimension(TestAxes.Tenant),
        )
    }

    @Test
    fun `context dimension extension returns null for unknown axis`() {
        val dims = dimensions {
            environment(TestEnvironment.PROD)
        }
        val ctx = TestContext(dimensions = dims)

        val unknownAxis = object : Dimension<TestEnvironment> {
            override val id: String = "unknown-axis"
        }

        Assertions.assertNull(
            ctx.dimension(unknownAxis),
            "Unknown axis should return null",
        )
    }
}
