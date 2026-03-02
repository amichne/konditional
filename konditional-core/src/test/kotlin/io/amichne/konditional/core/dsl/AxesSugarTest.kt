package io.amichne.konditional.core.dsl

import io.amichne.konditional.api.axisValues
import io.amichne.konditional.context.axis.Axis
import io.amichne.konditional.context.axis.AxisValue
import io.amichne.konditional.context.axis.axes
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for the simplified axis value selection sugar functions.
 */
class AxesSugarTest {

    enum class Environment : AxisValue<Environment> {
        DEV,
        STAGE,
        PROD
    }

    enum class Tenant : AxisValue<Tenant> {
        CONSUMER,
        SME,
        ENTERPRISE
    }

    private val envAxis = Axis.of<Environment>()
    private val tenantAxis = Axis.of<Tenant>()

    @Test
    fun `axis function with single value`() {
        val values = axisValues {
            axis(Environment.PROD)
        }

        assertEquals(setOf(Environment.PROD), values[envAxis])
    }

    @Test
    fun `axis function with multiple values`() {
        val values = axisValues {
            axis(Environment.DEV, Environment.STAGE)
        }

        assertEquals(setOf(Environment.DEV, Environment.STAGE), values[envAxis])
    }

    @Test
    fun `axis function with multiple axes`() {
        val values = axisValues {
            axis(Environment.PROD)
            axis(Tenant.ENTERPRISE)
        }

        assertEquals(setOf(Environment.PROD), values[envAxis])
        assertEquals(setOf(Tenant.ENTERPRISE), values[tenantAxis])
    }

    @Test
    fun `axis function with single value infers axis`() {
        val values = axisValues {
            axis(Environment.PROD)
        }

        // Verify the axis was automatically derived
        val axis = Axis.of<Environment>()
        assertEquals(setOf(Environment.PROD), values[axis])
    }

    @Test
    fun `axis function with multiple values infers axis`() {
        val values = axisValues {
            axis(Environment.DEV, Environment.STAGE, Environment.PROD)
        }

        val axis = Axis.of<Environment>()
        assertEquals(
            setOf(Environment.DEV, Environment.STAGE, Environment.PROD),
            values[axis]
        )
    }

    @Test
    fun `axis function with multiple enum types`() {
        val values = axisValues {
            axis(Environment.PROD)
            axis(Tenant.ENTERPRISE)
        }

        val envAxis = Axis.of<Environment>()
        val tenantAxis = Axis.of<Tenant>()

        assertEquals(setOf(Environment.PROD), values[envAxis])
        assertEquals(setOf(Tenant.ENTERPRISE), values[tenantAxis])
    }

    @Test
    fun `heterogeneous axis declaration in single call`() {
        // Demonstrate declaring multiple different axes in one call
        val values = axisValues {
            axis(Environment.PROD, Tenant.ENTERPRISE)
        }

        assertEquals(setOf(Environment.PROD), values[envAxis])
        assertEquals(setOf(Tenant.ENTERPRISE), values[tenantAxis])
    }

    @Test
    fun `heterogeneous axis with multiple values per axis`() {
        val values = axisValues {
            axis(Environment.DEV, Environment.STAGE, Tenant.SME, Tenant.ENTERPRISE)
        }

        assertEquals(setOf(Environment.DEV, Environment.STAGE), values[envAxis])
        assertEquals(setOf(Tenant.SME, Tenant.ENTERPRISE), values[tenantAxis])
    }

    @Test
    fun `multiple calls to axis for same axis accumulate values`() {
        val values = axisValues {
            axis(Environment.DEV)
            axis(Environment.PROD)
        }

        // Both values should be present
        val result = values[envAxis]
        assertTrue(result.contains(Environment.DEV))
        assertTrue(result.contains(Environment.PROD))
    }

    @Test
    fun `multiple calls to axis for same type accumulate values`() {
        val values = axisValues {
            axis(Environment.DEV)
            axis(Environment.PROD)
        }

        val axis = Axis.of<Environment>()
        val result = values[axis]
        assertTrue(result.contains(Environment.DEV))
        assertTrue(result.contains(Environment.PROD))
    }

    @Test
    fun `AxisValues of factory with single value`() {
        val values = axes(Environment.PROD)
        assertEquals(setOf(Environment.PROD), values[envAxis])
    }

    @Test
    fun `AxisValues of factory with multiple values from same axis`() {
        val values = axes(Environment.DEV, Environment.STAGE)
        assertEquals(setOf(Environment.DEV, Environment.STAGE), values[envAxis])
    }

    @Test
    fun `AxisValues of factory with heterogeneous axes`() {
        val values = axes(Environment.PROD, Tenant.ENTERPRISE)
        assertEquals(setOf(Environment.PROD), values[envAxis])
        assertEquals(setOf(Tenant.ENTERPRISE), values[tenantAxis])
    }

    @Test
    fun `AxisValues of factory with multiple values per axis`() {
        val values = axes(Environment.DEV, Environment.STAGE, Tenant.SME, Tenant.ENTERPRISE)
        assertEquals(setOf(Environment.DEV, Environment.STAGE), values[envAxis])
        assertEquals(setOf(Tenant.SME, Tenant.ENTERPRISE), values[tenantAxis])
    }
}
