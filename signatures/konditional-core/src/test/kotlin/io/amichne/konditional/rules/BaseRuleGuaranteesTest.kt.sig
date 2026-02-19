file=konditional-core/src/test/kotlin/io/amichne/konditional/rules/BaseRuleGuaranteesTest.kt
package=io.amichne.konditional.rules
imports=io.amichne.konditional.context.AppLocale,io.amichne.konditional.context.Context,io.amichne.konditional.context.Platform,io.amichne.konditional.context.RampUp,io.amichne.konditional.context.Version,io.amichne.konditional.core.id.StableId,io.amichne.konditional.fixtures.core.id.TestStableId,io.amichne.konditional.rules.targeting.Targeting,io.amichne.konditional.rules.versions.LeftBound,kotlin.test.Test,kotlin.test.assertEquals,kotlin.test.assertFalse,kotlin.test.assertTrue
type=io.amichne.konditional.rules.RuleGuaranteesTest|kind=class|decl=class RuleGuaranteesTest
type=io.amichne.konditional.rules.CustomContext|kind=interface|decl=interface CustomContext :
fields:
- private val defaultContext
- private val customContext
methods:
- private fun subscriptionTargeting(requiredTier: String?): Targeting.Custom<CustomContext>
- fun `base rule with no restrictions matches any context`()
- fun `base rule locale restriction is always enforced`()
- fun `base rule platform restriction is always enforced`()
- fun `base rule version restriction is always enforced`()
- fun `custom targeting leaf cannot bypass base locale restriction`()
- fun `custom targeting leaf cannot bypass base platform restriction`()
- fun `custom targeting leaf requires both base and additional criteria to match`()
- fun `custom targeting leaf specificity includes both base and additional specificity`()
- fun `custom targeting leaf with no restrictions on base attributes still enforces custom criteria`()
