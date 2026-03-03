@file:OptIn(io.amichne.konditional.api.KonditionalInternalApi::class)

package io.amichne.konditional.dimensions

import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.axis.AxisValue
import io.amichne.konditional.context.axis.KonditionalExplicitId
import io.amichne.konditional.context.axis.axes
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.enable
import io.amichne.konditional.core.dsl.rules.targeting.scopes.constrain
import io.amichne.konditional.fixtures.TestContext
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AxisFederationIntegrationTest {
    @KonditionalExplicitId("federated-environment")
    private enum class FederatedEnvironment(override val id: String) : AxisValue<FederatedEnvironment> {
        PROD("prod"),
    }

    private val namespaceA =
        object : Namespace(id = "federated-a") {
            val flag by boolean<TestContext>(default = false) {
                enable {
                    constrain(FederatedEnvironment.PROD)
                }
            }
        }

    private val namespaceB =
        object : Namespace(id = "federated-b") {
            val flag by boolean<TestContext>(default = false) {
                enable {
                    constrain(FederatedEnvironment.PROD)
                }
            }
        }

    @Test
    fun `namespaces can reuse a shared axis handle for explicit axis targeting`() {
        val context = TestContext(
            axes = axes(FederatedEnvironment.PROD)
        )

        assertTrue(namespaceA.flag.evaluate(context))
        assertTrue(namespaceB.flag.evaluate(context))
    }
}
