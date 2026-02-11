file=konditional-core/src/test/kotlin/io/amichne/konditional/core/FlagEntryTypeSafetyTest.kt
package=io.amichne.konditional.core
imports=io.amichne.konditional.api.KonditionalInternalApi,io.amichne.konditional.api.evaluate,io.amichne.konditional.context.AppLocale,io.amichne.konditional.context.Context,io.amichne.konditional.context.Platform,io.amichne.konditional.context.RampUp.Companion.MAX,io.amichne.konditional.context.Version,io.amichne.konditional.core.dsl.enable,io.amichne.konditional.core.id.StableId,io.amichne.konditional.fixtures.utilities.localeIds,io.amichne.konditional.rules.ConditionalValue.Companion.targetedBy,io.amichne.konditional.rules.Rule,io.amichne.konditional.rules.versions.Unbounded,io.amichne.konditional.runtime.load,io.amichne.konditional.serialization.instance.Configuration,kotlin.test.Test,kotlin.test.assertEquals,kotlin.test.assertNotNull
type=io.amichne.konditional.core.FlagEntryTypeSafetyTest|kind=class|decl=class FlagEntryTypeSafetyTest
type=io.amichne.konditional.core.Features|kind=object|decl=private object Features : Namespace.TestNamespaceFacade("flag-entry-type-safety")
fields:
- val featureA by boolean<Context>(default = false)
- val featureB by boolean<Context>(default = true)
- val configA by string<Context>(default = "default")
- val configB by string<Context>(default = "config-b-default")
- val timeout by integer<Context>(default = 10)
methods:
- private fun ctx( idHex: String, locale: AppLocale = AppLocale.UNITED_STATES, platform: Platform = Platform.IOS, version: String = "1.0.0", )
- fun `Given FlagDefinition, When created, Then maintains type information correctly`()
- fun `Given ContextualFlagDefinition, When evaluating, Then returns correct value type`()
- fun `Given ContextualFlagDefinition with different value types, When evaluating, Then each returns correct type`()
- fun `Given Snapshot with ContextualFlagDefinition instances, When loading, Then all flags are accessible`()
