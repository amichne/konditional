package io.amichne.konditional.dimensions

import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.axis.AxisValue
import io.amichne.konditional.context.axis.axes
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.enable
import io.amichne.konditional.core.dsl.rules.targeting.scopes.constrain
import io.amichne.konditional.fixtures.TestContext
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AxisNamespaceIsolationTest {
    private enum class ScopedEnvironmentA(override val id: String) : AxisValue<ScopedEnvironmentA> {
        PROD("prod"),
    }

    private enum class ScopedEnvironmentB(override val id: String) : AxisValue<ScopedEnvironmentB> {
        PROD("prod"),
    }

    private object NamespaceA : Namespace.TestNamespaceFacade("axis-ns-a") {
        val flag by boolean<TestContext>(default = false) {
            enable {
                constrain(ScopedEnvironmentA.PROD)
            }
        }
    }

    private object NamespaceB : Namespace.TestNamespaceFacade("axis-ns-b") {
        val flag by boolean<TestContext>(default = false) {
            enable {
                constrain(ScopedEnvironmentB.PROD)
            }
        }
    }

    @Test
    fun `axis handles are isolated by axis id`() {
        val contextForA =
            TestContext(
                axes = axes(ScopedEnvironmentA.PROD),
            )
        val contextForB =
            TestContext(
                axes = axes(ScopedEnvironmentB.PROD),
            )

        assertTrue(NamespaceA.flag.evaluate(contextForA))
        assertFalse(NamespaceA.flag.evaluate(contextForB))

        assertTrue(NamespaceB.flag.evaluate(contextForB))
        assertFalse(NamespaceB.flag.evaluate(contextForA))
    }
}
