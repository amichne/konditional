file=konditional-core/src/test/kotlin/io/amichne/konditional/dimensions/AxisContextIntegrationTest.kt
package=io.amichne.konditional.dimensions
imports=io.amichne.konditional.api.axis,io.amichne.konditional.api.axisValues,io.amichne.konditional.core.dsl.unaryPlus,io.amichne.konditional.fixtures.TestAxes,io.amichne.konditional.fixtures.TestContext,io.amichne.konditional.fixtures.TestEnvironment,io.amichne.konditional.fixtures.TestTenant,org.junit.jupiter.api.Assertions,org.junit.jupiter.api.Test,org.junit.jupiter.api.assertThrows
type=io.amichne.konditional.dimensions.AxisContextIntegrationTest|kind=class|decl=class AxisContextIntegrationTest
type=io.amichne.konditional.dimensions.EphemeralEnvironment|kind=enum|decl=private enum class EphemeralEnvironment(override val id: String) :
methods:
- fun `context axis extension returns typed values`()
- fun `context axis type-based extension returns typed values`()
- fun `context axis extension returns null for missing axis`()
- fun `axis values require explicit axis registration for inferred values`()
