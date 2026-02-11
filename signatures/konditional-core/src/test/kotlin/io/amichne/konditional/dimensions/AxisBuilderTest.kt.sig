file=konditional-core/src/test/kotlin/io/amichne/konditional/dimensions/AxisBuilderTest.kt
package=io.amichne.konditional.dimensions
imports=io.amichne.konditional.api.axis,io.amichne.konditional.api.axisValues,io.amichne.konditional.api.evaluate,io.amichne.konditional.context.axis.AxisValues,io.amichne.konditional.core.dsl.unaryPlus,io.amichne.konditional.fixtures.FeaturesWithAxis,io.amichne.konditional.fixtures.TestAxes,io.amichne.konditional.fixtures.TestContext,io.amichne.konditional.fixtures.TestEnvironment,io.amichne.konditional.fixtures.TestTenant,io.amichne.konditional.fixtures.environment,io.amichne.konditional.fixtures.tenant,io.amichne.konditional.internal.builders.AxisValuesBuilder,org.junit.jupiter.api.Assertions,org.junit.jupiter.api.Test
type=io.amichne.konditional.dimensions.AxisBuilderTest|kind=class|decl=class AxisBuilderTest
methods:
- fun `axisValues builder returns EMPTY when no values set`()
- fun `axisValues builder stores and retrieves typed values`()
- fun `axisValues unary plus sets values`()
- fun `axisValues builder accumulates multiple values for same axis`()
- fun `axisValues setIfNotNull skips null values`()
- fun `context axis returns set of values`()
- fun `axis constraints match when any value is allowed`()
