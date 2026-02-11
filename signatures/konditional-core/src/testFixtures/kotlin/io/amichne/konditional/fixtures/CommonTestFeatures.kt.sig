file=konditional-core/src/testFixtures/kotlin/io/amichne/konditional/fixtures/CommonTestFeatures.kt
package=io.amichne.konditional.fixtures
imports=io.amichne.konditional.context.Context,io.amichne.konditional.core.Namespace
type=io.amichne.konditional.fixtures.CommonTestFeatures|kind=object|decl=object CommonTestFeatures : Namespace.TestNamespaceFacade("common-test-features")
fields:
- val testFeature by boolean<Context>(default = false)
- val alwaysTrue by boolean<Context>(default = true)
- val enabledFeature by boolean<Context>(default = true)
- val disabledFeature by boolean<Context>(default = false)
- val rolloutFeature by boolean<Context>(default = false)
- val platformFeature by boolean<Context>(default = false)
- val localeFeature by boolean<Context>(default = false)
- val versionFeature by boolean<Context>(default = false)
