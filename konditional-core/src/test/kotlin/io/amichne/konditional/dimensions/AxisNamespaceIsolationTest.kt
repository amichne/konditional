package io.amichne.konditional.dimensions

import io.amichne.konditional.api.axisValues
import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.axis.AxisValue
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.enable
import io.amichne.konditional.fixtures.TestContext
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AxisNamespaceIsolationTest {
    private enum class ScopedEnvironment(override val id: String) : AxisValue<ScopedEnvironment> {
        PROD("prod"),
    }

    private object NamespaceA : Namespace.TestNamespaceFacade("axis-ns-a") {
        val environmentAxis = axis<ScopedEnvironment>("namespace-a-environment")

        val flag by boolean<TestContext>(default = false) {
            enable { axis(ScopedEnvironment.PROD) }
        }
    }

    private object NamespaceB : Namespace.TestNamespaceFacade("axis-ns-b") {
        val environmentAxis = axis<ScopedEnvironment>("namespace-b-environment")

        val flag by boolean<TestContext>(default = false) {
            enable { axis(ScopedEnvironment.PROD) }
        }
    }

    @Test
    fun `type inferred axes are isolated by namespace axis catalogs`() {
        val contextForA =
            TestContext(
                axisValues = axisValues { set(NamespaceA.environmentAxis, ScopedEnvironment.PROD) },
            )
        val contextForB =
            TestContext(
                axisValues = axisValues { set(NamespaceB.environmentAxis, ScopedEnvironment.PROD) },
            )

        assertTrue(NamespaceA.flag.evaluate(contextForA))
        assertFalse(NamespaceA.flag.evaluate(contextForB))

        assertTrue(NamespaceB.flag.evaluate(contextForB))
        assertFalse(NamespaceB.flag.evaluate(contextForA))
    }
}
