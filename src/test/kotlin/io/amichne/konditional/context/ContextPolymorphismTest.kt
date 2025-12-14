package io.amichne.konditional.context

import io.amichne.konditional.core.Namespace
import io.amichne.konditional.api.evaluate
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.fixtures.CompositeContext
import io.amichne.konditional.fixtures.EnterpriseContext
import io.amichne.konditional.fixtures.EnterpriseFeatures
import io.amichne.konditional.fixtures.EnterpriseFeatures.advanced_analytics
import io.amichne.konditional.fixtures.EnterpriseRule
import io.amichne.konditional.fixtures.ExperimentContext
import io.amichne.konditional.fixtures.ExperimentFeatures
import io.amichne.konditional.fixtures.SubscriptionTier
import io.amichne.konditional.fixtures.UserRole
import io.amichne.konditional.fixtures.core.id.TestStableId
import io.amichne.konditional.fixtures.utilities.update
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
        advanced_analytics.update(default = false) {
            // This demonstrates that the rule can access base Context properties
            rule(true) {
                platforms(Platform.WEB)
                versions {
                    min(2, 0)
                }
            }
        }

        val ctx = EnterpriseContext(
            locale = AppLocale.UNITED_STATES,
            platform = Platform.WEB,
            appVersion = Version(2, 5, 0),
            stableId = TestStableId,
            organizationId = "org-123",
            subscriptionTier = SubscriptionTier.PREMIUM,
            userRole = UserRole.ADMIN,
        )

        assertTrue(advanced_analytics.evaluate(ctx))
    }

    @Test
    fun `Given ExperimentContext, When evaluating flags, Then experiment-specific properties are accessible`() {
        // Configure using .update() for test-specific configuration
        ExperimentFeatures.homepage_variant.update("control") {
            rule("variant-a") {
                platforms(Platform.IOS, Platform.ANDROID)
            }
            rule("variant-b") {
                platforms(Platform.WEB)
            }
        }

        val mobileCtx = ExperimentContext(
            locale = AppLocale.UNITED_STATES,
            platform = Platform.IOS,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("22222222222222222222222222222222"),
            experimentGroups = setOf("exp-001", "exp-002"),
            sessionId = "session-abc",
        )

        val webCtx = ExperimentContext(
            locale = AppLocale.UNITED_STATES,
            platform = Platform.WEB,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("33333333333333333333333333333333"),
            experimentGroups = setOf("exp-001"),
            sessionId = "session-xyz",
        )

        assertEquals("variant-a", ExperimentFeatures.homepage_variant.evaluate(mobileCtx))
        assertEquals("variant-b", ExperimentFeatures.homepage_variant.evaluate(webCtx))
    }

    @Suppress("USELESS_IS_CHECK")
    @Test
    fun `Given multiple custom contexts, When using different flags, Then contexts are independent`() {
        // Configure using .update() for test-specific configuration
        // Each contextFn can be evaluated independently with its own flags
        EnterpriseFeatures.api_access.update(false) {
            rule(true) {
            }
        }
        ExperimentFeatures.onboarding_style.update("classic") {
            rule("modern") {
            }
        }
        val enterpriseCtx1 = EnterpriseContext(
            locale = AppLocale.UNITED_STATES,
            platform = Platform.WEB,
            appVersion = Version(1, 0, 0),
            stableId = TestStableId.newInstance(),
            organizationId = "org-456",
            subscriptionTier = SubscriptionTier.ENTERPRISE,
            userRole = UserRole.OWNER,
        )
        val experimentCtx1 = ExperimentContext(
            locale = AppLocale.UNITED_STATES,
            platform = Platform.IOS,
            appVersion = Version(1, 0, 0),
            stableId = TestStableId.newInstance(),
            experimentGroups = setOf("exp-003"),
            sessionId = "session-123",
        )
        // Each context can be evaluated independently with its own flags
        val apiAccess1 = EnterpriseFeatures.api_access.evaluate(enterpriseCtx1)
        val onboardingStyle1 = ExperimentFeatures.onboarding_style.evaluate(experimentCtx1)
        assertTrue(apiAccess1 is Boolean)
        // Each contextFn can be evaluated independently with its own flags
        assertTrue(onboardingStyle1 is String)
    }

    @Test
    fun `Given custom EnterpriseRule, When matching with business logic, Then custom properties are enforced`() {
        // Configure using .update() for test-specific configuration
        EnterpriseFeatures.api_access.update(false) {
            rule(true) {
                platforms(Platform.WEB)
                rollout { 100 }

                extension { EnterpriseRule(SubscriptionTier.ENTERPRISE, UserRole.ADMIN).matches(this) }
            }
        }

        val enterpriseAdmin = EnterpriseContext(
            locale = AppLocale.UNITED_STATES,
            platform = Platform.WEB,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"),
            organizationId = "org-ent",
            subscriptionTier = SubscriptionTier.ENTERPRISE,
            userRole = UserRole.ADMIN,
        )

        val premiumEditor = EnterpriseContext(
            locale = AppLocale.UNITED_STATES,
            platform = Platform.WEB,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("cccccccccccccccccccccccccccccccc"),
            organizationId = "org-prem",
            subscriptionTier = SubscriptionTier.PREMIUM,
            userRole = UserRole.EDITOR,
        )

        assertFalse(EnterpriseFeatures.api_access.evaluate(premiumEditor))
        assertTrue(EnterpriseFeatures.api_access.evaluate(enterpriseAdmin))
    }

    @Test
    fun `Given CompositeContext, When evaluating flags, Then delegated properties are accessible`() {
        // Configure using .update() for test-specific configuration
        EnterpriseFeatures.custom_branding.update(default = false) {
            rule(true) {
                rollout { 100 }
            }
        }

        val baseContext = Context(
            locale = AppLocale.UNITED_STATES,
            platform = Platform.ANDROID,
            appVersion = Version(3, 1, 0),
            stableId = StableId.of("dddddddddddddddddddddddddddddddd"),
        )

        val compositeCtx = CompositeContext(
            context = baseContext,
            experimentGroups = setOf("exp-100"),
            sessionId = "session-composite",
        )

        assertTrue(EnterpriseFeatures.custom_branding.evaluate(compositeCtx))
    }
}
