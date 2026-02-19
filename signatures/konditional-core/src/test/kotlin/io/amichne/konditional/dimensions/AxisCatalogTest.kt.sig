file=konditional-core/src/test/kotlin/io/amichne/konditional/dimensions/AxisCatalogTest.kt
package=io.amichne.konditional.dimensions
imports=io.amichne.konditional.context.axis.Axis,io.amichne.konditional.context.axis.AxisValue,io.amichne.konditional.core.registry.AxisCatalog,org.junit.jupiter.api.Assertions,org.junit.jupiter.api.Test,org.junit.jupiter.api.assertThrows
type=io.amichne.konditional.dimensions.AxisCatalogTest|kind=class|decl=class AxisCatalogTest
type=io.amichne.konditional.dimensions.ExplicitEnvironment|kind=enum|decl=private enum class ExplicitEnvironment(override val id: String) : AxisValue<ExplicitEnvironment>
type=io.amichne.konditional.dimensions.ManualEnvironment|kind=enum|decl=private enum class ManualEnvironment(override val id: String) : AxisValue<ManualEnvironment>
type=io.amichne.konditional.dimensions.UnregisteredEnvironment|kind=enum|decl=private enum class UnregisteredEnvironment(override val id: String) : AxisValue<UnregisteredEnvironment>
type=io.amichne.konditional.dimensions.DuplicateEnvironment|kind=enum|decl=private enum class DuplicateEnvironment(override val id: String) : AxisValue<DuplicateEnvironment>
type=io.amichne.konditional.dimensions.DuplicateTenant|kind=enum|decl=private enum class DuplicateTenant(override val id: String) : AxisValue<DuplicateTenant>
type=io.amichne.konditional.dimensions.LookupEnvironment|kind=enum|decl=private enum class LookupEnvironment(override val id: String) : AxisValue<LookupEnvironment>
type=io.amichne.konditional.dimensions.SharedEnvironment|kind=enum|decl=private enum class SharedEnvironment(override val id: String) : AxisValue<SharedEnvironment>
methods:
- fun `explicit axis registers in scoped catalog`()
- fun `axis declaration does not implicitly register without catalog`()
- fun `axisForOrThrow fails for unregistered type in catalog`()
- fun `axis equality is based on id and valueClass`()
- fun `catalog rejects duplicate ids with different value types`()
- fun `type-based lookup returns expected axis in catalog`()
- fun `registrations are isolated per catalog with no cross leakage`()
- fun `same value type can be bound to different ids in different catalogs`()
