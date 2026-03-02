package io.amichne.konditional.dimensions

import io.amichne.konditional.context.axis.Axis
import io.amichne.konditional.context.axis.AxisValue
import io.amichne.konditional.context.axis.KonditionalExplicitId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class AxisTest {
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

    @KonditionalExplicitId("shared-id")
    private enum class ExplicitTypeA(override val id: String) : AxisValue<ExplicitTypeA> {
        X("x"),
    }

    @KonditionalExplicitId("shared-id")
    private enum class ExplicitTypeB(override val id: String) : AxisValue<ExplicitTypeB> {
        Y("y"),
    }

    @Test
    fun `axis value defaults id to enum name`() {
        assertEquals("PROD", DefaultIdEnvironment.PROD.id)
    }

    @Test
    fun `axis value id can be overridden`() {
        assertEquals("prod-custom", CustomIdEnvironment.PROD.id)
    }

    @Test
    fun `axis id defaults to value type fully-qualified name`() {
        val axis = Axis.of(DefaultIdEnvironment::class)

        assertEquals(DefaultIdEnvironment::class.qualifiedName, axis.id)
    }

    @Test
    fun `KonditionalExplicitId annotation overrides default axis id derivation`() {
        val axis = Axis.of(AnnotatedEnvironment::class)

        assertEquals("annotated-env", axis.id)
    }

    @Test
    fun `axis equality includes both id and value class`() {
        val axisA = Axis.of(ExplicitTypeA::class)
        val axisB = Axis.of(ExplicitTypeA::class)
        val axisC = Axis.of(ExplicitTypeB::class)

        assertEquals(axisA, axisB)
        assertEquals(axisA.hashCode(), axisB.hashCode())
        assertNotEquals(axisA, axisC)
    }
}
