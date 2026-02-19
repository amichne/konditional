file=konditional-core/src/test/kotlin/io/amichne/konditional/dimensions/AxisNamespaceIsolationTest.kt
package=io.amichne.konditional.dimensions
imports=io.amichne.konditional.api.axisValues,io.amichne.konditional.api.evaluate,io.amichne.konditional.context.axis.AxisValue,io.amichne.konditional.core.Namespace,io.amichne.konditional.core.dsl.enable,io.amichne.konditional.fixtures.TestContext,org.junit.jupiter.api.Assertions.assertFalse,org.junit.jupiter.api.Assertions.assertTrue,org.junit.jupiter.api.Test
type=io.amichne.konditional.dimensions.AxisNamespaceIsolationTest|kind=class|decl=class AxisNamespaceIsolationTest
type=io.amichne.konditional.dimensions.ScopedEnvironment|kind=enum|decl=private enum class ScopedEnvironment(override val id: String) : AxisValue<ScopedEnvironment>
type=io.amichne.konditional.dimensions.NamespaceA|kind=object|decl=private object NamespaceA : Namespace.TestNamespaceFacade("axis-ns-a")
type=io.amichne.konditional.dimensions.NamespaceB|kind=object|decl=private object NamespaceB : Namespace.TestNamespaceFacade("axis-ns-b")
fields:
- val environmentAxis
- val flag by boolean<TestContext>(default = false)
- val environmentAxis
- val flag by boolean<TestContext>(default = false)
methods:
- fun `type inferred axes are isolated by namespace axis catalogs`()
