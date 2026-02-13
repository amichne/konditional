package io.amichne.konditional.dimensions

import io.amichne.konditional.context.axis.Axis
import io.amichne.konditional.context.axis.AxisValue
import io.amichne.konditional.core.registry.AxisRegistry
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AxisRegistryTest {

    private enum class ExplicitEnvironment(override val id: String) : AxisValue<ExplicitEnvironment> {
        PROD("prod"),
    }

    private enum class ManualEnvironment(override val id: String) : AxisValue<ManualEnvironment> {
        STAGE("stage"),
    }

    private enum class UnregisteredEnvironment(override val id: String) : AxisValue<UnregisteredEnvironment> {
        DEV("dev"),
    }

    private enum class DuplicateEnvironment(override val id: String) : AxisValue<DuplicateEnvironment> {
        PROD("prod"),
    }

    private enum class DuplicateTenant(override val id: String) : AxisValue<DuplicateTenant> {
        ENTERPRISE("enterprise"),
    }

    private enum class LookupEnvironment(override val id: String) : AxisValue<LookupEnvironment> {
        PROD("prod"),
    }

    @Test
    fun `explicit axis registers once`() {
        Axis.of("explicit-axis", ExplicitEnvironment::class)

        val axis = AxisRegistry.axisFor(ExplicitEnvironment::class)

        Assertions.assertNotNull(axis)
        Assertions.assertEquals("explicit-axis", axis?.id)
        Assertions.assertEquals(ExplicitEnvironment::class, axis?.valueClass)
    }

    @Test
    fun `autoRegister false prevents registration`() {
        Axis.of("manual-axis", ManualEnvironment::class, autoRegister = false)

        val axis = AxisRegistry.axisFor(ManualEnvironment::class)

        Assertions.assertNull(axis)
    }

    @Test
    fun `axisForOrThrow fails for unregistered axis type`() {
        val error = assertThrows<IllegalArgumentException> {
            AxisRegistry.axisForOrThrow(UnregisteredEnvironment::class)
        }

        Assertions.assertTrue(error.message.orEmpty().contains("No axis registered for type"))
    }

    @Test
    fun `axis equality is based on id and valueClass`() {
        val axisA = Axis.of("equality-axis", ExplicitEnvironment::class, autoRegister = false)
        val axisB = Axis.of("equality-axis", ExplicitEnvironment::class, autoRegister = false)

        Assertions.assertEquals(axisA, axisB)
        Assertions.assertEquals(axisA.hashCode(), axisB.hashCode())
    }

    @Test
    fun `registry rejects duplicate ids with different value types`() {
        Axis.of("duplicate-axis", DuplicateEnvironment::class)

        assertThrows<IllegalArgumentException> {
            Axis.of("duplicate-axis", DuplicateTenant::class)
        }
    }

    @Test
    fun `type-based lookup returns expected axis`() {
        Axis.of("lookup-axis", LookupEnvironment::class)

        val axis = AxisRegistry.axisFor(LookupEnvironment::class)

        Assertions.assertNotNull(axis)
        Assertions.assertEquals("lookup-axis", axis?.id)
        Assertions.assertEquals(LookupEnvironment::class, axis?.valueClass)
    }
}
