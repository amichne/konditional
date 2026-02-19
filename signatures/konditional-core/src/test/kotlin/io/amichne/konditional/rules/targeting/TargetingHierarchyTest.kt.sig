file=konditional-core/src/test/kotlin/io/amichne/konditional/rules/targeting/TargetingHierarchyTest.kt
package=io.amichne.konditional.rules.targeting
imports=io.amichne.konditional.context.AppLocale,io.amichne.konditional.context.Context,io.amichne.konditional.context.Platform,io.amichne.konditional.context.Version,io.amichne.konditional.core.id.StableId,io.amichne.konditional.fixtures.EnterpriseContext,io.amichne.konditional.fixtures.SubscriptionTier,io.amichne.konditional.fixtures.UserRole,io.amichne.konditional.fixtures.core.id.TestStableId,io.amichne.konditional.rules.Rule,io.amichne.konditional.rules.versions.LeftBound,io.amichne.konditional.rules.versions.Unbounded,kotlin.test.Test,kotlin.test.assertEquals,kotlin.test.assertFalse,kotlin.test.assertTrue
type=io.amichne.konditional.rules.targeting.TargetingHierarchyTest|kind=class|decl=class TargetingHierarchyTest
fields:
- private val usIosContext
- private val caAndroidContext
- private val minimalContext
- private val premiumIosUsContext
- private val basicAndroidUsContext
methods:
- fun `Locale leaf matches context with matching locale id`()
- fun `Locale leaf rejects context with non-matching locale id`()
- fun `Platform leaf matches context with matching platform`()
- fun `Platform leaf returns false for non-matching platform`()
- fun `Version leaf matches context within range`()
- fun `Version leaf rejects context outside range`()
- fun `Version leaf with Unbounded contributes zero specificity`()
- fun `Version leaf with bound contributes one specificity`()
- fun `Custom leaf evaluates block correctly`()
- fun `Guarded returns false when context lacks capability`()
- fun `Guarded returns true when context has capability and matches`()
- fun `Guarded platform returns false when context lacks PlatformContext`()
- fun `Guarded version returns false when context lacks VersionContext`()
- fun `All with empty targets matches everything`()
- fun `All specificity is sum of leaves`()
- fun `All short-circuits on first non-matching leaf`()
- fun `All plus combines two conjunctions`()
- fun `catchAll has zero specificity`()
- fun `multi-dimensional rule matches premium iOS US user`()
- fun `multi-dimensional rule specificity is 3`()
- fun `whenContext returns false for context lacking the capability`()
- fun `whenContext fires correctly when context implements R`()
- fun `whenContext contributes correct specificity`()
- fun `same inputs produce same result across repeated evaluations`()
- fun `localesOrEmpty extracts locale ids from Guarded-Locale leaves`()
- fun `platformsOrEmpty extracts platform ids from Guarded-Platform leaves`()
- fun `versionRangeOrNull extracts version range from Guarded-Version leaf`()
- fun `versionRangeOrNull returns null when no version leaf present`()
- fun `axesOrEmpty extracts axis constraints`()
- fun `customLeafCount counts Custom and Guarded-Custom leaves`()
