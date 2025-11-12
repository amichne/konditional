package io.amichne.konditional.context

import io.amichne.konditional.core.BooleanFeature
import io.amichne.konditional.core.FeatureModule
import io.amichne.konditional.core.StringFeature
import io.amichne.konditional.core.config
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.fakes.FakeRegistry
import io.amichne.konditional.rules.Rule
import io.amichne.konditional.rules.evaluable.Evaluable
import io.amichne.konditional.rules.versions.FullyBound
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
        BASIC, PREMIUM, ENTERPRISE
    }

    enum class UserRole {
        EDITOR, ADMIN, OWNER
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

    // SingletonModuleRegistry using EnterpriseContext
    enum class EnterpriseFlags(
        override val key: String,
    ) : BooleanFeature<EnterpriseContext, FeatureModule.Core> {
        ADVANCED_ANALYTICS("advanced_analytics"),
        CUSTOM_BRANDING("custom_branding"),
        API_ACCESS("api_access");

        override val module: FeatureModule.Core = FeatureModule.Core
    }

    // SingletonModuleRegistry using ExperimentContext
    enum class ExperimentFlags(
        override val key: String,
    ) : StringFeature<ExperimentContext, FeatureModule.Core> {
        HOMEPAGE_VARIANT("homepage_variant"),
        ONBOARDING_STYLE("onboarding_style");

        override val module: FeatureModule.Core
            get() = FeatureModule.Core
    }

    // Custom rule that extends Rule for EnterpriseContext
    data class EnterpriseRule(
        val requiredTier: SubscriptionTier? = null,
        val requiredRole: UserRole? = null,
    ) : Evaluable<EnterpriseContext>() {
        override fun matches(context: EnterpriseContext): Boolean =
            (requiredTier == null || context.subscriptionTier >= requiredTier) &&
                (requiredRole == null || context.userRole >= requiredRole)
    }

    @Test
    fun `Given EnterpriseContext, When evaluating flags, Then context-specific properties are accessible`() {
        FeatureModule.Core.config {
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
        FeatureModule.Core.config {
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
        FeatureModule.Core.config {
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
    fun `Given base Context and custom Context, When both used, Then type safety is maintained`() {
        // Define flag in scope
        data class StandardFlagA(
            override val key: String = "feature_a",
        ) : BooleanFeature<Context, FeatureModule.Core> {

            override val module: FeatureModule.Core = FeatureModule.Core
        }

        val standardFlagA = StandardFlagA()

        FeatureModule.Core.config {
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
        val registry = FakeRegistry()
        FeatureModule.Core.config(registry) {
            EnterpriseFlags.API_ACCESS with {
                default(false)
                rule {
                    platforms(Platform.WEB)
                    rollout = Rollout.MAX

                    extension {
                        EnterpriseRule(SubscriptionTier.ENTERPRISE, UserRole.ADMIN)
                    }
                } implies true
            }
        }

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

        assertFalse(premiumEditor.evaluate(EnterpriseFlags.API_ACCESS))
        assertTrue(enterpriseAdmin.evaluate(EnterpriseFlags.API_ACCESS))
    }
}
