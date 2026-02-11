file=konditional-core/src/test/kotlin/io/amichne/konditional/core/DslSugarTest.kt
package=io.amichne.konditional.core
imports=io.amichne.konditional.api.KonditionalInternalApi,io.amichne.konditional.api.evaluate,io.amichne.konditional.context.AppLocale,io.amichne.konditional.context.Context,io.amichne.konditional.context.Platform,io.amichne.konditional.context.Version,io.amichne.konditional.core.dsl.disable,io.amichne.konditional.core.dsl.enable,io.amichne.konditional.core.id.StableId,io.amichne.konditional.fixtures.core.id.TestStableId,org.junit.jupiter.api.Assertions.assertEquals,org.junit.jupiter.api.Assertions.assertThrows,org.junit.jupiter.api.Assertions.assertTrue,org.junit.jupiter.api.Test
type=io.amichne.konditional.core.DslSugarTest|kind=class|decl=class DslSugarTest
type=io.amichne.konditional.core.Features|kind=object|decl=private object Features : Namespace.TestNamespaceFacade("dsl-sugar")
fields:
- val boolFlag by boolean<Context>(default = false)
- val stringFlag by string<Context>(default = "default")
methods:
- private fun ctx( platform: Platform, locale: AppLocale = AppLocale.UNITED_STATES, version: Version = Version.of(1, 0, 0), stableId: StableId = TestStableId, )
- fun `rule yields declares a criteria-first rule`()
- fun `unclosed criteria-first rule fails fast`()
