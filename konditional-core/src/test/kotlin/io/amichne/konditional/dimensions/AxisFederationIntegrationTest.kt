package io.amichne.konditional.dimensions

import io.amichne.konditional.api.axisValues
import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.axis.Axis
import io.amichne.konditional.context.axis.AxisValue
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.enable
import io.amichne.konditional.core.registry.AxisCatalogFederator
import io.amichne.konditional.fixtures.TestContext
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AxisFederationIntegrationTest {
    private enum class FederatedEnvironment(override val id: String) : AxisValue<FederatedEnvironment> {
        PROD("prod"),
    }

    private val federator = AxisCatalogFederator()
    private val federatedEnvironmentAxis: Axis<FederatedEnvironment> =
        Axis.of("federated-environment", FederatedEnvironment::class).also(federator::register)

    private val namespaceA =
        object : Namespace(
            id = "federated-a",
            axisCatalog = federator.namespaceCatalog(),
        ) {
            val flag by boolean<TestContext>(default = false) {
                enable { axis(FederatedEnvironment.PROD) }
            }
        }

    private val namespaceB =
        object : Namespace(
            id = "federated-b",
            axisCatalog = federator.namespaceCatalog(),
        ) {
            val flag by boolean<TestContext>(default = false) {
                enable { axis(FederatedEnvironment.PROD) }
            }
        }

    @Test
    fun `namespaces can reuse federated axis catalogs for inferred axis targeting`() {
        val context = TestContext(axisValues = axisValues { set(federatedEnvironmentAxis, FederatedEnvironment.PROD) })

        assertTrue(namespaceA.flag.evaluate(context))
        assertTrue(namespaceB.flag.evaluate(context))
    }
}
