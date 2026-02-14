file=konditional-core/src/test/kotlin/io/amichne/konditional/core/ConditionEvaluationTest.kt
package=io.amichne.konditional.core
imports=io.amichne.konditional.api.KonditionalInternalApi,io.amichne.konditional.context.AppLocale,io.amichne.konditional.context.Context,io.amichne.konditional.context.Platform,io.amichne.konditional.context.RampUp,io.amichne.konditional.context.Version,io.amichne.konditional.core.id.StableId,io.amichne.konditional.fixtures.utilities.localeIds,io.amichne.konditional.fixtures.utilities.platformIds,io.amichne.konditional.rules.ConditionalValue.Companion.targetedBy,io.amichne.konditional.rules.Rule,io.amichne.konditional.rules.versions.Unbounded,kotlin.test.Test,kotlin.test.assertEquals,kotlin.test.assertTrue
type=io.amichne.konditional.core.ConditionEvaluationTest|kind=class|decl=class ConditionEvaluationTest
type=io.amichne.konditional.core.TestFlags|kind=object|decl=object TestFlags : Namespace.TestNamespaceFacade("condition-eval")
fields:
- val TEST_FLAG by string<Context>(default = "default")
methods:
- private fun ctx( idHex: String, locale: AppLocale = AppLocale.UNITED_STATES, platform: Platform = Platform.IOS, version: String = "1.0.0", )
- fun `Given condition with no matching rules, When evaluating, Then returns default value`()
- fun `Given condition with one matching rule, When evaluating, Then returns rule value`()
- fun `Given multiple rules, When evaluating, Then most specific rule wins`()
- fun `Given rules with same specificity, When evaluating, Then insertion order is used as tiebreaker`()
- fun `Given rule with 0 percent ramp-up, When evaluating, Then never matches`()
- fun `Given rule with 100 percent ramp-up, When evaluating, Then always matches`()
- fun `Given rule with 50 percent ramp-up, When evaluating many users, Then approximately half match`()
- fun `Given same user ID, When evaluating same condition, Then result is deterministic`()
- fun `Given different salts, When evaluating same user, Then bucketing is independent`()
- fun `Given rule not matching context constraints, When evaluating, Then skips to next rule regardless of ramp-up`()
- fun `Given rule matching but user not in bucket, When evaluating, Then continues to next rule`()
- fun `Given sorted surjections by specificity, When initializing condition, Then surjections are properly ordered`()
