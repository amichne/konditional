package io.amichne.konditional.core.dsl

import io.amichne.konditional.context.axis.Axis
import io.amichne.konditional.context.axis.AxisValue
import io.amichne.konditional.context.axis.axes
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for the retained [axes] entrypoint.
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

    @Test
    fun `multiple values for same axis are deduplicated`() {
        val values = axes(Environment.PROD, Environment.PROD)
        val result = values[envAxis]

        assertTrue(result.contains(Environment.PROD))
        assertEquals(1, result.size)
    }
}
