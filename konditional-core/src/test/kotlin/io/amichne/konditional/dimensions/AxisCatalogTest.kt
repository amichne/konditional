package io.amichne.konditional.dimensions

import io.amichne.konditional.context.axis.Axis
import io.amichne.konditional.context.axis.AxisValue
import io.amichne.konditional.core.registry.AxisCatalog
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AxisCatalogTest {

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

    private enum class SharedEnvironment(override val id: String) : AxisValue<SharedEnvironment> {
        PROD("prod"),
    }

    @Test
    fun `explicit axis registers in scoped catalog`() {
        val catalog = AxisCatalog()
        Axis.of("explicit-axis", ExplicitEnvironment::class, catalog)

        val axis = catalog.axisFor(ExplicitEnvironment::class)

        Assertions.assertNotNull(axis)
        Assertions.assertEquals("explicit-axis", axis?.id)
        Assertions.assertEquals(ExplicitEnvironment::class, axis?.valueClass)
    }

    @Test
    fun `axis declaration does not implicitly register without catalog`() {
        val catalog = AxisCatalog()
        Axis.of("manual-axis", ManualEnvironment::class)

        val axis = catalog.axisFor(ManualEnvironment::class)

        Assertions.assertNull(axis)
    }

    @Test
    fun `axisForOrThrow fails for unregistered type in catalog`() {
        val catalog = AxisCatalog()
        val error = assertThrows<IllegalArgumentException> {
            catalog.axisForOrThrow(UnregisteredEnvironment::class)
        }

        Assertions.assertTrue(error.message.orEmpty().contains("No axis registered for type"))
    }

    @Test
    fun `axis equality is based on id and valueClass`() {
        val axisA = Axis.of("equality-axis", ExplicitEnvironment::class)
        val axisB = Axis.of("equality-axis", ExplicitEnvironment::class)

        Assertions.assertEquals(axisA, axisB)
        Assertions.assertEquals(axisA.hashCode(), axisB.hashCode())
    }

    @Test
    fun `catalog rejects duplicate ids with different value types`() {
        val catalog = AxisCatalog()
        Axis.of("duplicate-axis", DuplicateEnvironment::class, catalog)

        assertThrows<IllegalArgumentException> {
            Axis.of("duplicate-axis", DuplicateTenant::class, catalog)
        }
    }

    @Test
    fun `type-based lookup returns expected axis in catalog`() {
        val catalog = AxisCatalog()
        Axis.of("lookup-axis", LookupEnvironment::class, catalog)

        val axis = catalog.axisFor(LookupEnvironment::class)

        Assertions.assertNotNull(axis)
        Assertions.assertEquals("lookup-axis", axis?.id)
        Assertions.assertEquals(LookupEnvironment::class, axis?.valueClass)
    }

    @Test
    fun `registrations are isolated per catalog with no cross leakage`() {
        val catalogA = AxisCatalog()
        val catalogB = AxisCatalog()
        Axis.of("isolated-axis", LookupEnvironment::class, catalogA)

        Assertions.assertNotNull(catalogA.axisFor(LookupEnvironment::class))
        Assertions.assertNull(catalogB.axisFor(LookupEnvironment::class))
    }

    @Test
    fun `same value type can be bound to different ids in different catalogs`() {
        val catalogA = AxisCatalog()
        val catalogB = AxisCatalog()
        Axis.of("namespace-a-env", SharedEnvironment::class, catalogA)
        Axis.of("namespace-b-env", SharedEnvironment::class, catalogB)

        Assertions.assertEquals("namespace-a-env", catalogA.axisFor(SharedEnvironment::class)?.id)
        Assertions.assertEquals("namespace-b-env", catalogB.axisFor(SharedEnvironment::class)?.id)
    }
}
