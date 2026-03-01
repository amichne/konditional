@file:OptIn(io.amichne.konditional.api.KonditionalInternalApi::class)

package io.amichne.konditional.dimensions

import io.amichne.konditional.api.axisValues
import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.axis.Axis
import io.amichne.konditional.context.axis.AxisValue
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.enable
import io.amichne.konditional.core.dsl.variant
import io.amichne.konditional.core.registry.AxisCatalog
import io.amichne.konditional.fixtures.TestContext
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AxisFederationIntegrationTest {
    private enum class FederatedEnvironment(override val id: String) : AxisValue<FederatedEnvironment> {
        PROD("prod"),
    }

    private val sharedCatalog = AxisCatalog()
    private val federatedEnvironmentAxis: Axis<FederatedEnvironment> =
        Axis.of("federated-environment", FederatedEnvironment::class, sharedCatalog)

    private val namespaceA =
        object : Namespace(
            id = "federated-a",
            axisCatalog = AxisCatalog(sharedCatalog),
        ) {
            val flag by boolean<TestContext>(default = false) {
                enable {
                    variant {
                        federatedEnvironmentAxis { include(FederatedEnvironment.PROD) }
                    }
                }
            }
        }

    private val namespaceB =
        object : Namespace(
            id = "federated-b",
            axisCatalog = AxisCatalog(sharedCatalog),
        ) {
            val flag by boolean<TestContext>(default = false) {
                enable {
                    variant {
                        federatedEnvironmentAxis { include(FederatedEnvironment.PROD) }
                    }
                }
            }
        }

    @Test
    fun `namespaces can reuse a shared parent catalog for explicit axis targeting`() {
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
