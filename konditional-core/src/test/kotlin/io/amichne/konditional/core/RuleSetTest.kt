package io.amichne.konditional.core

import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.dsl.ruleSet
import io.amichne.konditional.core.dsl.rules.RuleSet
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.fixtures.EnterpriseContext
import io.amichne.konditional.fixtures.SubscriptionTier
import io.amichne.konditional.fixtures.UserRole
import io.amichne.konditional.fixtures.core.id.TestStableId
import io.amichne.konditional.fixtures.utilities.update
import kotlin.test.Test
import kotlin.test.assertEquals

class RuleSetTest {

    private enum class CheckoutVariant {
        CLASSIC,
        OPTIMIZED,
        EXPERIMENTAL,
    }

    private object CheckoutFlags : Namespace.TestNamespaceFacade("rule-set-checkout") {
        val checkoutVariant by enum<CheckoutVariant, EnterpriseContext>(default = CheckoutVariant.CLASSIC)
    }

    private object CheckoutRuleSets {
        private val feature: Feature<CheckoutVariant, EnterpriseContext, Namespace> = CheckoutFlags.checkoutVariant

        val core = feature.ruleSet {
            rule(CheckoutVariant.OPTIMIZED) {
                extension { subscriptionTier == SubscriptionTier.PREMIUM }
            }
        }

        val platform = feature.ruleSet(Context::class) {
            rule(CheckoutVariant.EXPERIMENTAL) {
                ios()
            }
        }
    }

    private data class AdminContext(
        override val locale: AppLocale,
        override val platform: Platform,
        override val appVersion: Version,
        override val stableId: StableId,
        val role: String,
    ) : Context,
        Context.LocaleContext,
        Context.PlatformContext,
        Context.VersionContext,
        Context.StableIdContext

    private fun coreContext(platform: Platform) = Context(
        locale = AppLocale.UNITED_STATES,
        platform = platform,
        appVersion = Version.of(1, 0, 0),
        stableId = TestStableId.newInstance(),
    )

    private fun adminContext(platform: Platform) = AdminContext(
        locale = AppLocale.UNITED_STATES,
        platform = platform,
        appVersion = Version.of(1, 0, 0),
        stableId = TestStableId.newInstance(),
        role = "admin",
    )

    @Test
    fun `include preserves rule set order for equal specificity`() {
        val namespace = object : Namespace.TestNamespaceFacade("rule-set-include-order") {
            val flag by string<Context>(default = "default")
        }
        val feature: Feature<String, Context, Namespace> = namespace.flag

        val first = feature.ruleSet {
            rule("first") { ios() }
        }

        val second = feature.ruleSet {
            rule("second") { ios() }
        }

        feature.update("default") {
            include(first)
            include(second)
        }

        assertEquals("first", feature.evaluate(coreContext(Platform.IOS)))
    }

    @Test
    fun `rule set plus preserves left to right rule ordering`() {
        val namespace = object : Namespace.TestNamespaceFacade("rule-set-plus-order") {
            val flag by string<Context>(default = "default")
        }
        val feature: Feature<String, Context, Namespace> = namespace.flag

        val first = feature.ruleSet {
            rule("first") { ios() }
        }

        val second = feature.ruleSet {
            rule("second") { ios() }
        }

        val combined = first + second

        feature.update("default") {
            include(combined)
        }

        assertEquals("first", feature.evaluate(coreContext(Platform.IOS)))
    }

    @Test
    fun `rule set can be declared against a supertype context`() {
        val namespace = object : Namespace.TestNamespaceFacade("rule-set-contravariant") {
            val flag by boolean<AdminContext>(default = false)
        }
        val feature: Feature<Boolean, AdminContext, Namespace> = namespace.flag

        val baseRuleSet = feature.ruleSet(Context::class) {
            rule(true) { ios() }
        }

        feature.update(false) {
            include(baseRuleSet)
        }

        assertEquals(true, feature.evaluate(adminContext(Platform.IOS)))
    }

    @Test
    fun `rule set empty is identity and plus is associative`() {
        val namespace = object : Namespace.TestNamespaceFacade("rule-set-monoid") {
            val flag by string<Context>(default = "default")
        }
        val feature: Feature<String, Context, Namespace> = namespace.flag

        val alpha = feature.ruleSet { rule("alpha") }
        val beta = feature.ruleSet { rule("beta") }
        val gamma = feature.ruleSet { rule("gamma") }
        val empty = RuleSet.empty(feature)

        val left = (alpha + beta) + gamma
        val right = alpha + (beta + gamma)

        assertEquals(listOf("alpha", "beta", "gamma"), left.rules.map { it.value })
        assertEquals(listOf("alpha", "beta", "gamma"), right.rules.map { it.value })
        assertEquals(left.rules.map { it.value }, (empty + left).rules.map { it.value })
        assertEquals(left.rules.map { it.value }, (left + empty).rules.map { it.value })
    }

    @Test
    fun `Real world example of rule composition`() {
        CheckoutFlags.checkoutVariant.update(CheckoutVariant.CLASSIC) {
            include(CheckoutRuleSets.core)
            include(CheckoutRuleSets.platform)
        }

        val baseContext = EnterpriseContext(
            locale = AppLocale.UNITED_STATES,
            platform = Platform.IOS,
            appVersion = Version.of(1, 0, 0),
            stableId = StableId.of("11111111111111111111111111111111"),
            organizationId = "org-123",
            subscriptionTier = SubscriptionTier.BASIC,
            userRole = UserRole.ADMIN,
        )
        val premiumIos = baseContext.copy(subscriptionTier = SubscriptionTier.PREMIUM)
        val premiumAndroid = baseContext.copy(
            platform = Platform.ANDROID,
            subscriptionTier = SubscriptionTier.PREMIUM,
            stableId = StableId.of("22222222222222222222222222222222"),
        )
        val basicAndroid = baseContext.copy(
            platform = Platform.ANDROID,
            stableId = StableId.of("33333333333333333333333333333333"),
        )

        assertEquals(CheckoutVariant.OPTIMIZED, CheckoutFlags.checkoutVariant.evaluate(premiumIos))
        assertEquals(CheckoutVariant.OPTIMIZED, CheckoutFlags.checkoutVariant.evaluate(premiumAndroid))
        assertEquals(CheckoutVariant.EXPERIMENTAL, CheckoutFlags.checkoutVariant.evaluate(baseContext))
        assertEquals(CheckoutVariant.CLASSIC, CheckoutFlags.checkoutVariant.evaluate(basicAndroid))
    }
}
