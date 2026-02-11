file=konditional-core/src/test/kotlin/io/amichne/konditional/rules/RuleMatchingTest.kt
package=io.amichne.konditional.rules
imports=io.amichne.konditional.context.AppLocale,io.amichne.konditional.context.Context,io.amichne.konditional.context.Platform,io.amichne.konditional.context.RampUp,io.amichne.konditional.context.Version,io.amichne.konditional.core.id.StableId,io.amichne.konditional.fixtures.core.id.TestStableId,io.amichne.konditional.fixtures.utilities.localeIds,io.amichne.konditional.fixtures.utilities.platformIds,io.amichne.konditional.rules.versions.FullyBound,io.amichne.konditional.rules.versions.Unbounded,kotlin.test.Test,kotlin.test.assertEquals,kotlin.test.assertFalse,kotlin.test.assertTrue
type=io.amichne.konditional.rules.RuleMatchingTest|kind=class|decl=class RuleMatchingTest
methods:
- private fun ctx( locale: AppLocale = AppLocale.UNITED_STATES, platform: Platform = Platform.IOS, version: String = "1.0.0", idHex: String = TestStableId.id, )
- fun `Given rule with no constraints, When matching, Then all contexts match`()
- fun `Given rule with single platform, When matching, Then only that platform matches`()
- fun `Given rule with multiple platforms, When matching, Then any of those platforms match`()
- fun `Given rule with single locale, When matching, Then only that locale matches`()
- fun `Given rule with multiple locales, When matching, Then any of those locales match`()
- fun `Given rule with version range, When matching, Then only versions in range match`()
- fun `Given rule with combined constraints, When matching, Then all constraints must match`()
- fun `Given rules with different specificity, When calculating specificity, Then more specific rules have higher scores`()
- fun `Given multiple locales or platforms, When calculating specificity, Then specificity counts presence not quantity`()
- fun `Given rules with notes, When comparing specificity, Then notes serve as tiebreaker`()
