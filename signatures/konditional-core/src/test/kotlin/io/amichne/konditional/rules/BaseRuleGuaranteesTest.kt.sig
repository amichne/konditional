file=konditional-core/src/test/kotlin/io/amichne/konditional/rules/BaseRuleGuaranteesTest.kt
package=io.amichne.konditional.rules
imports=io.amichne.konditional.context.AppLocale,io.amichne.konditional.context.Context,io.amichne.konditional.context.Platform,io.amichne.konditional.context.RampUp,io.amichne.konditional.context.Version,io.amichne.konditional.core.id.StableId,io.amichne.konditional.fixtures.core.id.TestStableId,io.amichne.konditional.fixtures.utilities.localeIds,io.amichne.konditional.fixtures.utilities.platformIds,io.amichne.konditional.rules.evaluable.Predicate,io.amichne.konditional.rules.evaluable.Predicate.Companion.factory,io.amichne.konditional.rules.versions.LeftBound,kotlin.test.Test,kotlin.test.assertEquals,kotlin.test.assertFalse,kotlin.test.assertTrue
type=io.amichne.konditional.rules.RuleGuaranteesTest|kind=class|decl=class RuleGuaranteesTest
type=io.amichne.konditional.rules.CustomContext|kind=interface|decl=interface CustomContext :
type=io.amichne.konditional.rules.SubscriptionRule|kind=class|decl=data class SubscriptionRule<C : CustomContext>( val requiredTier: String? = null, ) : Predicate<C> by factory({ context -> requiredTier == null || context.subscriptionTier == requiredTier })
fields:
- private val defaultContext
- private val customContext
methods:
- fun `base rule with no restrictions matches any context`()
- fun `base rule locale restriction is always enforced`()
- fun `base rule platform restriction is always enforced`()
- fun `base rule version restriction is always enforced`()
- fun `custom rule cannot bypass base locale restriction`()
- fun `custom rule cannot bypass base platform restriction`()
- fun `custom rule requires both base and additional criteria to match`()
- fun `custom rule specificity includes both base and additional specificity`()
- fun `custom rule with no restrictions on base attributes still enforces custom criteria`()
