package io.amichne.konditional.context

import io.amichne.konditional.builders.ConfigBuilder.Companion.config
import io.amichne.konditional.builders.FlagBuilder
import io.amichne.konditional.core.Conditional
import io.amichne.konditional.core.Flags.evaluate
import io.amichne.konditional.core.StableId
import io.amichne.konditional.rules.Rule
import io.amichne.konditional.rules.versions.FullyBound
import io.amichne.konditional.rules.versions.Unbounded
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for Context polymorphism and extensibility.
 * Validates that custom Context implementations can be used with the feature flag system.
 */
class ContextPolymorphismTest {

    // Custom context for enterprise features
    data class EnterpriseContext(
        override val locale: AppLocale,
        override val platform: Platform,
        override val appVersion: Version,
        override val stableId: StableId,
        val organizationId: String,
        val subscriptionTier: SubscriptionTier,
        val userRole: UserRole,
    ) : Context

    enum class SubscriptionTier {
        FREE, BASIC, PREMIUM, ENTERPRISE
    }

    enum class UserRole {
        VIEWER, EDITOR, ADMIN, OWNER
    }

    // Custom context for A/B testing
    data class ExperimentContext(
        override val locale: AppLocale,
        override val platform: Platform,
        override val appVersion: Version,
        override val stableId: StableId,
        val experimentGroups: Set<String>,
        val sessionId: String,
    ) : Context

    // Flags using EnterpriseContext
    enum class EnterpriseFlags(
        override val key: String,
    ) : Conditional<Boolean, EnterpriseContext> {
        ADVANCED_ANALYTICS("advanced_analytics"),
        BULK_EXPORT("bulk_export"),
        CUSTOM_BRANDING("custom_branding"),
        API_ACCESS("api_access"),
        ;

        override fun with(build: FlagBuilder<Boolean, EnterpriseContext>.() -> Unit) =
            update(FlagBuilder(this).apply(build).build())
    }

    // Flags using ExperimentContext
    enum class ExperimentFlags(
        override val key: String,
    ) : Conditional<String, ExperimentContext> {
        HOMEPAGE_VARIANT("homepage_variant"),
        CHECKOUT_FLOW("checkout_flow"),
        ONBOARDING_STYLE("onboarding_style"),
        ;

        override fun with(build: FlagBuilder<String, ExperimentContext>.() -> Unit) =
            update(FlagBuilder(this).apply(build).build())
    }

    // Custom rule that extends Rule for EnterpriseContext
    data class EnterpriseRule(
        val Rule: Rule<EnterpriseContext>,
        val requiredTier: SubscriptionTier? = null,
        val requiredRole: UserRole? = null,
    ) {
        fun matches(context: EnterpriseContext): Boolean {
            if (!Rule.matches(context)) return false
            if (requiredTier != null && context.subscriptionTier.ordinal < requiredTier.ordinal) return false
            if (requiredRole != null && context.userRole.ordinal < requiredRole.ordinal) return false
            return true
        }
    }

    @Test
    fun `Given EnterpriseContext, When evaluating flags, Then context-specific properties are accessible`() {
        config {
            EnterpriseFlags.ADVANCED_ANALYTICS with {
                default(false)
                // This demonstrates that the rule can access base Context properties
                rule {
                    platforms(Platform.WEB)
                    versions {
                        min(2, 0)
                    }
                } implies true
            }
        }

        val ctx = EnterpriseContext(
            locale = AppLocale.EN_US,
            platform = Platform.WEB,
            appVersion = Version(2, 5, 0),
            stableId = StableId.of("11111111111111111111111111111111"),
            organizationId = "org-123",
            subscriptionTier = SubscriptionTier.PREMIUM,
            userRole = UserRole.ADMIN,
        )

        assertTrue(ctx.evaluate(EnterpriseFlags.ADVANCED_ANALYTICS))
    }

    @Test
    fun `Given ExperimentContext, When evaluating flags, Then experiment-specific properties are accessible`() {
        config {
            ExperimentFlags.HOMEPAGE_VARIANT with {
                default("control")
                rule {
                    platforms(Platform.IOS, Platform.ANDROID)
                } implies "variant-a"
                rule {
                    platforms(Platform.WEB)
                } implies "variant-b"
            }
        }

        val mobileCtx = ExperimentContext(
            locale = AppLocale.EN_US,
            platform = Platform.IOS,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("22222222222222222222222222222222"),
            experimentGroups = setOf("exp-001", "exp-002"),
            sessionId = "session-abc",
        )

        val webCtx = ExperimentContext(
            locale = AppLocale.EN_US,
            platform = Platform.WEB,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("33333333333333333333333333333333"),
            experimentGroups = setOf("exp-001"),
            sessionId = "session-xyz",
        )

        assertEquals("variant-a", mobileCtx.evaluate(ExperimentFlags.HOMEPAGE_VARIANT))
        assertEquals("variant-b", webCtx.evaluate(ExperimentFlags.HOMEPAGE_VARIANT))
    }

    @Suppress("USELESS_IS_CHECK")
    @Test
    fun `Given multiple custom contexts, When using different flags, Then contexts are independent`() {
        config {
            EnterpriseFlags.API_ACCESS with {
                default(false)
                rule {
                } implies true
            }
            ExperimentFlags.ONBOARDING_STYLE with {
                default("classic")
                rule {
                } implies "modern"
            }
        }

        val enterpriseCtx = EnterpriseContext(
            locale = AppLocale.EN_US,
            platform = Platform.WEB,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("44444444444444444444444444444444"),
            organizationId = "org-456",
            subscriptionTier = SubscriptionTier.ENTERPRISE,
            userRole = UserRole.OWNER,
        )

        val experimentCtx = ExperimentContext(
            locale = AppLocale.EN_US,
            platform = Platform.IOS,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("55555555555555555555555555555555"),
            experimentGroups = setOf("exp-003"),
            sessionId = "session-123",
        )

        // Each context can be evaluated independently with its own flags
        val apiAccess = enterpriseCtx.evaluate(EnterpriseFlags.API_ACCESS)
        val onboardingStyle = experimentCtx.evaluate(ExperimentFlags.ONBOARDING_STYLE)

        assertTrue(apiAccess is Boolean)
        assertTrue(onboardingStyle is String)
    }

    @Test
    fun `Given custom context, When evaluating all flags, Then returns all flags for that context`() {
        config {
            EnterpriseFlags.ADVANCED_ANALYTICS with {
                default(false)
            }
            EnterpriseFlags.BULK_EXPORT with {
                default(true)
            }
            EnterpriseFlags.CUSTOM_BRANDING with {
                default(false)
                rule {
                    platforms(Platform.WEB)
                } implies true
            }
        }

        val ctx = EnterpriseContext(
            locale = AppLocale.EN_US,
            platform = Platform.WEB,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("66666666666666666666666666666666"),
            organizationId = "org-789",
            subscriptionTier = SubscriptionTier.PREMIUM,
            userRole = UserRole.ADMIN,
        )

        val allFlags = ctx.evaluate()

        assertEquals(3, allFlags.size)
        assertTrue(allFlags.containsKey(EnterpriseFlags.ADVANCED_ANALYTICS))
        assertTrue(allFlags.containsKey(EnterpriseFlags.BULK_EXPORT))
        assertTrue(allFlags.containsKey(EnterpriseFlags.CUSTOM_BRANDING))
    }

    @Test
    fun `Given base Context and custom Context, When both used, Then type safety is maintained`() {
        // Define flag in scope
        data class StandardFlagA(override val key: String = "feature_a") : Conditional<Boolean, Context> {
            override fun with(build: FlagBuilder<Boolean, Context>.() -> Unit) =
                update(FlagBuilder(this).apply(build).build())
        }

        val standardFlagA = StandardFlagA()

        config {
            standardFlagA with {
                default(false)
                rule {
                    platforms(Platform.IOS)
                } implies true
            }
            EnterpriseFlags.CUSTOM_BRANDING with {
                default(false)
                rule {
                    platforms(Platform.WEB)
                } implies true
            }
        }

        // Base context can only evaluate base context flags
        val baseCtx = Context(
            locale = AppLocale.EN_US,
            platform = Platform.IOS,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("77777777777777777777777777777777"),
        )

        // Enterprise context can evaluate enterprise flags
        val enterpriseCtx = EnterpriseContext(
            locale = AppLocale.EN_US,
            platform = Platform.WEB,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("88888888888888888888888888888888"),
            organizationId = "org-999",
            subscriptionTier = SubscriptionTier.BASIC,
            userRole = UserRole.EDITOR,
        )

        assertTrue(baseCtx.evaluate(standardFlagA))
        assertTrue(enterpriseCtx.evaluate(EnterpriseFlags.CUSTOM_BRANDING))
    }

    @Test
    fun `Given EnterpriseContext subclass, When matching rules, Then base Context properties work correctly`() {
        val rule = Rule<EnterpriseContext>(
            rollout = Rollout.MAX,
            locales = setOf(AppLocale.EN_US, AppLocale.EN_CA),
            platforms = setOf(Platform.WEB),
            versionRange = FullyBound(Version(2, 0, 0), Version(3, 0, 0)),
        )

        val matchingCtx = EnterpriseContext(
            locale = AppLocale.EN_US,
            platform = Platform.WEB,
            appVersion = Version(2, 5, 0),
            stableId = StableId.of("99999999999999999999999999999999"),
            organizationId = "org-match",
            subscriptionTier = SubscriptionTier.ENTERPRISE,
            userRole = UserRole.OWNER,
        )

        val nonMatchingCtx = EnterpriseContext(
            locale = AppLocale.ES_US,
            platform = Platform.WEB,
            appVersion = Version(2, 5, 0),
            stableId = StableId.of("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"),
            organizationId = "org-nomatch",
            subscriptionTier = SubscriptionTier.ENTERPRISE,
            userRole = UserRole.OWNER,
        )

        assertTrue(rule.matches(matchingCtx))
        assertFalse(rule.matches(nonMatchingCtx))
    }

    @Test
    fun `Given custom EnterpriseRule, When matching with business logic, Then custom properties are enforced`() {
        val enterpriseOnlyRule = EnterpriseRule(
            Rule = Rule(
                rollout = Rollout.MAX,
                locales = emptySet(),
                platforms = setOf(Platform.WEB),
                versionRange = Unbounded,
            ),
            requiredTier = SubscriptionTier.ENTERPRISE,
            requiredRole = UserRole.ADMIN,
        )

        val enterpriseAdmin = EnterpriseContext(
            locale = AppLocale.EN_US,
            platform = Platform.WEB,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"),
            organizationId = "org-ent",
            subscriptionTier = SubscriptionTier.ENTERPRISE,
            userRole = UserRole.ADMIN,
        )

        val premiumEditor = EnterpriseContext(
            locale = AppLocale.EN_US,
            platform = Platform.WEB,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("cccccccccccccccccccccccccccccccc"),
            organizationId = "org-prem",
            subscriptionTier = SubscriptionTier.PREMIUM,
            userRole = UserRole.EDITOR,
        )

        assertTrue(enterpriseOnlyRule.matches(enterpriseAdmin))
        assertFalse(enterpriseOnlyRule.matches(premiumEditor))
    }
}
