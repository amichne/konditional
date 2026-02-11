file=konditional-core/src/test/kotlin/io/amichne/konditional/core/RuleSetTest.kt
package=io.amichne.konditional.core
imports=io.amichne.konditional.api.evaluate,io.amichne.konditional.context.AppLocale,io.amichne.konditional.context.Context,io.amichne.konditional.context.Platform,io.amichne.konditional.context.Version,io.amichne.konditional.core.dsl.ruleSet,io.amichne.konditional.core.dsl.rules.RuleSet,io.amichne.konditional.core.features.Feature,io.amichne.konditional.core.id.StableId,io.amichne.konditional.fixtures.EnterpriseContext,io.amichne.konditional.fixtures.SubscriptionTier,io.amichne.konditional.fixtures.UserRole,io.amichne.konditional.fixtures.core.id.TestStableId,io.amichne.konditional.fixtures.utilities.update,kotlin.test.Test,kotlin.test.assertEquals
type=io.amichne.konditional.core.RuleSetTest|kind=class|decl=class RuleSetTest
type=io.amichne.konditional.core.CheckoutVariant|kind=enum|decl=private enum class CheckoutVariant
type=io.amichne.konditional.core.CheckoutFlags|kind=object|decl=private object CheckoutFlags : Namespace.TestNamespaceFacade("rule-set-checkout")
type=io.amichne.konditional.core.CheckoutRuleSets|kind=object|decl=private object CheckoutRuleSets
type=io.amichne.konditional.core.AdminContext|kind=class|decl=private data class AdminContext( override val locale: AppLocale, override val platform: Platform, override val appVersion: Version, override val stableId: StableId, val role: String, ) : Context,
fields:
- val checkoutVariant by enum<CheckoutVariant, EnterpriseContext>(default = CheckoutVariant.CLASSIC)
- private val feature: Feature<CheckoutVariant, EnterpriseContext, Namespace>
- val core
- val platform
methods:
- private fun coreContext(platform: Platform)
- fun `rule set plus preserves left to right rule ordering`()
- fun `rule set can be declared against a supertype context`()
- fun `rule set empty is identity and plus is associative`()
- fun `Real world example of rule composition`()
