@file:OptIn(io.amichne.konditional.api.KonditionalInternalApi::class)

package io.amichne.konditional.dimensions

import io.amichne.konditional.context.axis.Axis
import io.amichne.konditional.context.axis.AxisValue
import io.amichne.konditional.context.axis.KonditionalExplicitId
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

    private enum class DefaultIdEnvironment : AxisValue<DefaultIdEnvironment> {
        PROD,
    }

    private enum class CustomIdEnvironment : AxisValue<CustomIdEnvironment> {
        PROD;

        override val id: String = "prod-custom"
    }

    @KonditionalExplicitId("annotated-env")
    private enum class AnnotatedEnvironment(override val id: String) : AxisValue<AnnotatedEnvironment> {
        PROD("prod"),
    }

    @KonditionalExplicitId("collision-id")
    private enum class CollisionTypeA(override val id: String) : AxisValue<CollisionTypeA> {
        X("x"),
    }

    @KonditionalExplicitId("collision-id")
    private enum class CollisionTypeB(override val id: String) : AxisValue<CollisionTypeB> {
        Y("y"),
    }

    @Test
    fun `axis value defaults id to enum name`() {
        Assertions.assertEquals("PROD", DefaultIdEnvironment.PROD.id)
    }

    @Test
    fun `axis value id can still be overridden`() {
        Assertions.assertEquals("prod-custom", CustomIdEnvironment.PROD.id)
    }

    @Test
    fun `axis id defaults to FQCN of value class`() {
        val axis = Axis.of(DefaultIdEnvironment::class)

        Assertions.assertEquals(DefaultIdEnvironment::class.qualifiedName, axis.id)
    }

    @Test
    fun `KonditionalExplicitId annotation overrides FQCN as axis id`() {
        val axis = Axis.of(AnnotatedEnvironment::class)

        Assertions.assertEquals("annotated-env", axis.id)
    }

    @Test
    fun `axis registers in scoped catalog`() {
        val catalog = AxisCatalog()
        Axis.of(ExplicitEnvironment::class, catalog)

        val axis = catalog.axisFor(ExplicitEnvironment::class)

        Assertions.assertNotNull(axis)
        Assertions.assertEquals(ExplicitEnvironment::class.qualifiedName, axis?.id)
        Assertions.assertEquals(ExplicitEnvironment::class, axis?.valueClass)
    }

    @Test
    fun `axis declaration does not implicitly register without catalog`() {
        val catalog = AxisCatalog()
        Axis.of(ManualEnvironment::class)

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
        val axisA = Axis.of(ExplicitEnvironment::class)
        val axisB = Axis.of(ExplicitEnvironment::class)

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
    fun `duplicate KonditionalExplicitId across types fails fast at catalog registration`() {
        val catalog = AxisCatalog()
        Axis.of(CollisionTypeA::class, catalog)

        val error = assertThrows<IllegalArgumentException> {
            Axis.of(CollisionTypeB::class, catalog)
        }

        Assertions.assertTrue(error.message.orEmpty().contains("collision-id"))
    }

    @Test
    fun `type-based lookup returns axis in catalog`() {
        val catalog = AxisCatalog()
        Axis.of(LookupEnvironment::class, catalog)

        val axis = catalog.axisFor(LookupEnvironment::class)

        Assertions.assertNotNull(axis)
        Assertions.assertEquals(LookupEnvironment::class.qualifiedName, axis?.id)
        Assertions.assertEquals(LookupEnvironment::class, axis?.valueClass)
    }

    @Test
    fun `registrations are isolated per catalog with no cross leakage`() {
        val catalogA = AxisCatalog()
        val catalogB = AxisCatalog()
        Axis.of(LookupEnvironment::class, catalogA)

        Assertions.assertNotNull(catalogA.axisFor(LookupEnvironment::class))
        Assertions.assertNull(catalogB.axisFor(LookupEnvironment::class))
    }

    @Test
    fun `child catalog can resolve parent catalog registrations`() {
        val sharedCatalog = AxisCatalog()
        Axis.of(SharedEnvironment::class, sharedCatalog)
        val childCatalog = AxisCatalog(sharedCatalog)

        val axis = childCatalog.axisFor(SharedEnvironment::class)

        Assertions.assertNotNull(axis)
        Assertions.assertEquals(SharedEnvironment::class.qualifiedName, axis?.id)
    }

}
