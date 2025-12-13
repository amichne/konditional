package io.amichne.konditional.dimensions

import io.amichne.konditional.context.Dimensions
import io.amichne.konditional.core.features.dimensions
import io.amichne.konditional.fixtures.TestAxes
import io.amichne.konditional.fixtures.TestEnvironment
import io.amichne.konditional.fixtures.TestTenant
import io.amichne.konditional.fixtures.environment
import io.amichne.konditional.fixtures.tenant
import io.amichne.konditional.internal.builders.DimensionBuilder
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Unit tests for Dimensions & ContextDimensionsBuilder.
 */
class DimensionsBuilderTest {

    @Test
    fun `dimensions builder returns EMPTY when no values set`() {
        val dims = dimensions { }

        Assertions.assertSame(
            Dimensions.EMPTY,
            dims,
            "Empty builder should return the shared EMPTY instance",
        )
        Assertions.assertNull(dims[TestAxes.Environment], "No environment should be present")
        Assertions.assertNull(dims[TestAxes.Tenant], "No tenant should be present")
        Assertions.assertNull(dims["env"], "Axis ID lookup should also be empty")
    }

    @Test
    fun `dimensions builder stores and retrieves typed values`() {
        val dims = dimensions {
            environment(TestEnvironment.DEV)
            tenant(TestTenant.SME)
        }

        Assertions.assertEquals(
            TestEnvironment.DEV,
            dims[TestAxes.Environment],
            "Typed axis lookup should return environment value",
        )
        Assertions.assertEquals(
            TestTenant.SME,
            dims[TestAxes.Tenant],
            "Typed axis lookup should return tenant value",
        )

        val envById = dims["env"]
        Assertions.assertNotNull(envById)
        Assertions.assertEquals("dev", envById!!.id)
    }

    @Test
    fun `dimensions builder overrides previous value for same axis`() {
        val dims = dimensions {
            environment(TestEnvironment.DEV)
            environment(TestEnvironment.PROD)   // override
            tenant(TestTenant.CONSUMER)
            tenant(TestTenant.ENTERPRISE)       // override
        }

        Assertions.assertEquals(
            TestEnvironment.PROD,
            dims[TestAxes.Environment],
            "Last value for Environment axis must win",
        )
        Assertions.assertEquals(
            TestTenant.ENTERPRISE,
            dims[TestAxes.Tenant],
            "Last value for Tenant axis must win",
        )
    }

    @Test
    fun `setIfNotNull skips null values`() {
        val builder = DimensionBuilder()
        builder.set(TestAxes.Environment, TestEnvironment.STAGE)
        builder.setIfNotNull(TestAxes.Tenant, null)

        val dims = builder.build()

        Assertions.assertEquals(
            TestEnvironment.STAGE,
            dims[TestAxes.Environment],
        )
        Assertions.assertNull(
            dims[TestAxes.Tenant],
            "Tenant should not be present when null is passed to setIfNotNull",
        )
    }
}
