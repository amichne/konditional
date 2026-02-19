file=konditional-core/src/testFixtures/kotlin/io/amichne/konditional/fixtures/TestAxis.kt
package=io.amichne.konditional.fixtures
imports=io.amichne.konditional.context.AppLocale,io.amichne.konditional.context.Context,io.amichne.konditional.context.Platform,io.amichne.konditional.context.Version,io.amichne.konditional.context.axis.Axis,io.amichne.konditional.context.axis.AxisValue,io.amichne.konditional.context.axis.AxisValues,io.amichne.konditional.core.Namespace,io.amichne.konditional.core.dsl.AxisValuesScope,io.amichne.konditional.core.dsl.enable,io.amichne.konditional.core.id.StableId,io.amichne.konditional.core.registry.AxisCatalog
type=io.amichne.konditional.fixtures.TestEnvironment|kind=enum|decl=enum class TestEnvironment( override val id: String, ) : AxisValue<TestEnvironment>
type=io.amichne.konditional.fixtures.TestTenant|kind=enum|decl=enum class TestTenant( override val id: String, ) : AxisValue<TestTenant>
type=io.amichne.konditional.fixtures.TestAxes|kind=object|decl=object TestAxes
type=io.amichne.konditional.fixtures.TestContext|kind=class|decl=data class TestContext( override val locale: AppLocale = AppLocale.UNITED_STATES, override val platform: Platform = Platform.ANDROID, override val appVersion: Version = Version.parse("1.0.0").getOrThrow(), override val stableId: StableId = StableId.of("deadbeef"), override val axisValues: AxisValues = AxisValues.EMPTY, ) : Context,
type=io.amichne.konditional.fixtures.FeaturesWithAxis|kind=object|decl=object FeaturesWithAxis : Namespace.TestNamespaceFacade("dimensions-test")
fields:
- val axisCatalog
- val Environment
- val Tenant
- val envScopedFlag by boolean<TestContext>(default = false)
- val envAndTenantScopedFlag by boolean<TestContext>(default = false)
- val fallbackRuleFlag by boolean<TestContext>(default = false)
- val repeatedAxisFlag by boolean<TestContext>(default = false)
