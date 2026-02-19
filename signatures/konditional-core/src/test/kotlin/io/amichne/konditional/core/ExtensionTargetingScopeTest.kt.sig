file=konditional-core/src/test/kotlin/io/amichne/konditional/core/ExtensionTargetingScopeTest.kt
package=io.amichne.konditional.core
imports=io.amichne.konditional.api.KonditionalInternalApi,io.amichne.konditional.api.evaluate,io.amichne.konditional.context.AppLocale,io.amichne.konditional.context.Context,io.amichne.konditional.context.Platform,io.amichne.konditional.context.Version,io.amichne.konditional.core.dsl.rules.targeting.scopes.whenContext,io.amichne.konditional.core.id.StableId,io.amichne.konditional.fixtures.EnterpriseContext,io.amichne.konditional.fixtures.SubscriptionTier,io.amichne.konditional.fixtures.UserRole,io.amichne.konditional.fixtures.utilities.update,kotlin.test.Test,kotlin.test.assertEquals
type=io.amichne.konditional.core.ExtensionTargetingScopeTest|kind=class|decl=class ExtensionTargetingScopeTest
type=io.amichne.konditional.core.Features|kind=object|decl=private object Features : Namespace.TestNamespaceFacade("extension-targeting-scope-test")
fields:
- val extensionComposition by boolean<EnterpriseContext>(default = false)
- val specificityOrdering by string<EnterpriseContext>(default = "default")
- val enterpriseOnlyOnBaseContext by boolean<Context>(default = false)
methods:
- private fun baseContext(idHex: String): Context
- fun `multiple extension blocks increase specificity cumulatively`()
- fun `whenContext evaluates only when runtime context supports the requested capability`()
