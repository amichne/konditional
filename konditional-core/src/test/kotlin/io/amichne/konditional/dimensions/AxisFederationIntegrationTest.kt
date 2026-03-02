@file:OptIn(io.amichne.konditional.api.KonditionalInternalApi::class)

package io.amichne.konditional.dimensions

import io.amichne.konditional.api.axisValues
import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.axis.Axis
import io.amichne.konditional.context.axis.AxisValue
import io.amichne.konditional.context.axis.KonditionalExplicitId
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.enable
import io.amichne.konditional.core.dsl.variant
import io.amichne.konditional.fixtures.TestContext
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AxisFederationIntegrationTest {
    @KonditionalExplicitId("federated-environment")
    private enum class FederatedEnvironment(override val id: String) : AxisValue<FederatedEnvironment> {
        PROD("prod"),
    }

    private val federatedEnvironmentAxis: Axis<FederatedEnvironment> =
        Axis.of<FederatedEnvironment>()

    private val namespaceA =
        object : Namespace(id = "federated-a") {
            val flag by boolean<TestContext>(default = false) {
                enable {
                    variant {
                        federatedEnvironmentAxis { include(FederatedEnvironment.PROD) }
                    }
                }
            }
        }

    private val namespaceB =
        object : Namespace(id = "federated-b") {
            val flag by boolean<TestContext>(default = false) {
                enable {
                    variant {
                        federatedEnvironmentAxis { include(FederatedEnvironment.PROD) }
                    }
                }
            }
        }

    @Test
    fun `namespaces can reuse a shared axis handle for explicit axis targeting`() {
        val context = TestContext(
            axisValues = axisValues {
                variant {
                    federatedEnvironmentAxis { include(FederatedEnvironment.PROD) }
                }
            },
        )

        assertTrue(namespaceA.flag.evaluate(context))
        assertTrue(namespaceB.flag.evaluate(context))
    }
}
