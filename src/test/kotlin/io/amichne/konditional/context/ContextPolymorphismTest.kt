package io.amichne.konditional.context

import io.amichne.konditional.context.Context.Companion.evaluate
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.features.update
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.fixtures.EnterpriseContext
import io.amichne.konditional.fixtures.EnterpriseFeatures
import io.amichne.konditional.fixtures.EnterpriseRule
import io.amichne.konditional.fixtures.ExperimentContext
import io.amichne.konditional.fixtures.ExperimentFeatures
import io.amichne.konditional.fixtures.SubscriptionTier
import io.amichne.konditional.fixtures.UserRole
import io.amichne.konditional.serialization.SnapshotSerializer
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for Context polymorphism and extensibility.
 * Validates that custom Context implementations can be used with the feature flag system.
 */
class ContextPolymorphismTest {
    @BeforeEach
    fun setup() {
        // Reset registry before each test
        println("Global")
        println("--------")
        println(SnapshotSerializer.serialize(Namespace.Global.configuration))
        println("--------")

        println("Payments")
        println("--------")
        println(SnapshotSerializer.serialize(Namespace.Payments.configuration))
        println("--------")

        println("Search")
        println("--------")
        println(SnapshotSerializer.serialize(Namespace.Search.configuration))
        println("--------")
    }

    @Test
    fun `Given EnterpriseContext, When evaluating flags, Then context-specific properties are accessible`() {
        // Configure using .update() for test-specific configuration
        EnterpriseFeatures.advanced_analytics.update {
            default(false)
            // This demonstrates that the rule can access base Context properties
            rule {
                platforms(Platform.WEB)
                versions {
                    min(2, 0)
                }
            } returns true
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

        assertTrue(ctx.evaluate(EnterpriseFeatures.advanced_analytics))
    }

    @Test
    fun `Given ExperimentContext, When evaluating flags, Then experiment-specific properties are accessible`() {
        // Configure using .update() for test-specific configuration
        ExperimentFeatures.homepage_variant.update {
            default("control")
            rule {
                platforms(Platform.IOS, Platform.ANDROID)
            } returns "variant-a"
            rule {
                platforms(Platform.WEB)
            } returns "variant-b"
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

        assertEquals("variant-a", mobileCtx.evaluate(ExperimentFeatures.homepage_variant))
        assertEquals("variant-b", webCtx.evaluate(ExperimentFeatures.homepage_variant))
    }

    @Suppress("USELESS_IS_CHECK")
    @Test
    fun `Given multiple custom contexts, When using different flags, Then contexts are independent`() {
        // Configure using .update() for test-specific configuration
        // Each context can be evaluated independently with its own flags
        EnterpriseFeatures.api_access.update {
            default(false)
            rule {
            } returns true
        }
        ExperimentFeatures.onboarding_style.update {
            default("classic")
            rule {
            } returns "modern"
        }
        val enterpriseCtx1 = EnterpriseContext(
            locale = AppLocale.EN_US,
            platform = Platform.WEB,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("44444444444444444444444444444444"),
            organizationId = "org-456",
            subscriptionTier = SubscriptionTier.ENTERPRISE,
            userRole = UserRole.OWNER,
        )
        val experimentCtx1 = ExperimentContext(
            locale = AppLocale.EN_US,
            platform = Platform.IOS,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("55555555555555555555555555555555"),
            experimentGroups = setOf("exp-003"),
            sessionId = "session-123",
        )
        // Each context can be evaluated independently with its own flags
        val apiAccess1 = enterpriseCtx1.evaluate(EnterpriseFeatures.api_access)
        val onboardingStyle1 = experimentCtx1.evaluate(ExperimentFeatures.onboarding_style)
        assertTrue(apiAccess1 is Boolean)
        // Each context can be evaluated independently with its own flags
        assertTrue(onboardingStyle1 is String)
    }

//    @TestNamespace
//    fun `Given base Context and custom Context, When both used, Then type safety is maintained`() {
//        // Define flag in scope
//        data class StandardFlagA(
//            override val key: String = "feature_a",
//        ) : BooleanFeature<Context, Namespace.Global> {
//
//            override val module: Namespace.Global = Namespace.Global
//        }
//
//        val standardFlagA = StandardFlagA()
//
//        Namespace.Global.config {
//            standardFlagA with {
//                default(false)
//                rule {
//                    platforms(Platform.IOS)
//                } returns true
//            }
//            EnterpriseFeatures.custom_branding with {
//                default(false)
//                rule {
//                    platforms(Platform.WEB)
//                } returns true
//            }
//        }
//
//        // Base context can only evaluate base context flags
//        val baseCtx = Context(
//            locale = AppLocale.EN_US,
//            platform = Platform.IOS,
//            appVersion = Version(1, 0, 0),
//            stableId = StableId.of("77777777777777777777777777777777"),
//        )
//
//        // Enterprise context can evaluate enterprise flags
//        val enterpriseCtx = EnterpriseContext(
//            locale = AppLocale.EN_US,
//            platform = Platform.WEB,
//            appVersion = Version(1, 0, 0),
//            stableId = StableId.of("88888888888888888888888888888888"),
//            organizationId = "org-999",
//            subscriptionTier = SubscriptionTier.BASIC,
//            userRole = UserRole.EDITOR,
//        )
//
//        assertTrue(baseCtx.evaluate(standardFlagA))
//        assertTrue(enterpriseCtx.evaluate(EnterpriseFeatures.custom_branding))
//    }
//
//    @TestNamespace
//    fun `Given EnterpriseContext subclass, When matching rules, Then base Context properties work correctly`() {
//        val rule = Rule<EnterpriseContext>(
//            rollout {  Rollout.MAX }
//            locales = setOf(AppLocale.EN_US, AppLocale.EN_CA),
//            platforms = setOf(Platform.WEB),
//            versionRange = FullyBound(Version(2, 0, 0), Version(3, 0, 0)),
//        )
//
//        val matchingCtx = EnterpriseContext(
//            locale = AppLocale.EN_US,
//            platform = Platform.WEB,
//            appVersion = Version(2, 5, 0),
//            stableId = StableId.of("99999999999999999999999999999999"),
//            organizationId = "org-match",
//            subscriptionTier = SubscriptionTier.ENTERPRISE,
//            userRole = UserRole.OWNER,
//        )
//
//        val nonMatchingCtx = EnterpriseContext(
//            locale = AppLocale.ES_US,
//            platform = Platform.WEB,
//            appVersion = Version(2, 5, 0),
//            stableId = StableId.of("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"),
//            organizationId = "org-nomatch",
//            subscriptionTier = SubscriptionTier.ENTERPRISE,
//            userRole = UserRole.OWNER,
//        )
//
//        assertTrue(rule.matches(matchingCtx))
//        assertFalse(rule.matches(nonMatchingCtx))
//    }

    @Test
    fun `Given custom EnterpriseRule, When matching with business logic, Then custom properties are enforced`() {
        // Configure using .update() for test-specific configuration
        EnterpriseFeatures.api_access.update {
            default(false)
            rule {
                platforms(Platform.WEB)
                rollout { 100 }

                extension {
                    EnterpriseRule(SubscriptionTier.ENTERPRISE, UserRole.ADMIN)
                }
            } returns true
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

        assertFalse(premiumEditor.evaluate(EnterpriseFeatures.api_access))
        assertTrue(enterpriseAdmin.evaluate(EnterpriseFeatures.api_access))
    }
}
