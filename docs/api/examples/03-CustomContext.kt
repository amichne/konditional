package examples

import io.amichne.konditional.builders.ConfigBuilder
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.context.evaluate
import io.amichne.konditional.core.Conditional
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.rules.evaluable.Evaluable

/**
 * Example 1: Basic Extended Context
 *
 * Extend Context with custom properties for domain-specific targeting.
 */
data class UserContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    val isPremium: Boolean,
    val accountAgeInDays: Int
) : Context

fun basicExtendedContext() {
    val PREMIUM_FEATURE: Conditional<Boolean, UserContext> =
        Conditional("premium_feature")

    ConfigBuilder.config {
        PREMIUM_FEATURE with {
            default(value = false)

            // Enable for premium users
            rule {
                extension {
                    object : Evaluable<UserContext>() {
                        override fun matches(context: UserContext): Boolean =
                            context.isPremium

                        override fun specificity(): Int = 1
                    }
                }
            } implies true
        }
    }

    val premiumUser = UserContext(
        locale = AppLocale.EN_US,
        platform = Platform.WEB,
        appVersion = Version(1, 0, 0),
        stableId = StableId.of("user-123"),
        isPremium = true,
        accountAgeInDays = 100
    )

    val freeUser = UserContext(
        locale = AppLocale.EN_US,
        platform = Platform.WEB,
        appVersion = Version(1, 0, 0),
        stableId = StableId.of("user-456"),
        isPremium = false,
        accountAgeInDays = 10
    )

    println("Premium user sees feature: ${premiumUser.evaluate(PREMIUM_FEATURE)}")  // true
    println("Free user sees feature: ${freeUser.evaluate(PREMIUM_FEATURE)}")  // false
}

/**
 * Example 2: Enterprise Context with Subscription Tiers
 *
 * Complex business logic based on subscription tiers.
 */
enum class SubscriptionTier {
    FREE, BASIC, PREMIUM, ENTERPRISE
}

data class EnterpriseContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    val subscriptionTier: SubscriptionTier,
    val organizationId: String,
    val organizationSize: Int
) : Context

fun enterpriseContext() {
    val ADVANCED_ANALYTICS: Conditional<Boolean, EnterpriseContext> =
        Conditional("advanced_analytics")

    ConfigBuilder.config {
        ADVANCED_ANALYTICS with {
            default(value = false)

            // Enable for Premium and Enterprise tiers
            rule {
                note("Premium and Enterprise subscribers")
                extension {
                    object : Evaluable<EnterpriseContext>() {
                        override fun matches(context: EnterpriseContext): Boolean =
                            context.subscriptionTier in setOf(
                                SubscriptionTier.PREMIUM,
                                SubscriptionTier.ENTERPRISE
                            )

                        override fun specificity(): Int = 1
                    }
                }
            } implies true

            // Also enable for large Basic tier organizations
            rule {
                note("Large Basic tier organizations (>100 users)")
                extension {
                    object : Evaluable<EnterpriseContext>() {
                        override fun matches(context: EnterpriseContext): Boolean =
                            context.subscriptionTier == SubscriptionTier.BASIC &&
                            context.organizationSize > 100

                        override fun specificity(): Int = 2  // More specific
                    }
                }
            } implies true
        }
    }

    val premiumContext = EnterpriseContext(
        locale = AppLocale.EN_US,
        platform = Platform.WEB,
        appVersion = Version(1, 0, 0),
        stableId = StableId.of("org-premium"),
        subscriptionTier = SubscriptionTier.PREMIUM,
        organizationId = "acme-corp",
        organizationSize = 50
    )

    val largBasicContext = EnterpriseContext(
        locale = AppLocale.EN_US,
        platform = Platform.WEB,
        appVersion = Version(1, 0, 0),
        stableId = StableId.of("org-large-basic"),
        subscriptionTier = SubscriptionTier.BASIC,
        organizationId = "big-company",
        organizationSize = 500
    )

    val smallBasicContext = EnterpriseContext(
        locale = AppLocale.EN_US,
        platform = Platform.WEB,
        appVersion = Version(1, 0, 0),
        stableId = StableId.of("org-small-basic"),
        subscriptionTier = SubscriptionTier.BASIC,
        organizationId = "small-company",
        organizationSize = 10
    )

    println("Premium org: ${premiumContext.evaluate(ADVANCED_ANALYTICS)}")  // true
    println("Large basic org: ${largBasicContext.evaluate(ADVANCED_ANALYTICS)}")  // true
    println("Small basic org: ${smallBasicContext.evaluate(ADVANCED_ANALYTICS)}")  // false
}

/**
 * Example 3: Reusable Custom Evaluables
 *
 * Create reusable evaluable classes for common targeting logic.
 */
class PremiumUserEvaluable : Evaluable<UserContext>() {
    override fun matches(context: UserContext): Boolean =
        context.isPremium

    override fun specificity(): Int = 1
}

class LoyalUserEvaluable(private val minDays: Int) : Evaluable<UserContext>() {
    override fun matches(context: UserContext): Boolean =
        context.accountAgeInDays >= minDays

    override fun specificity(): Int = 1
}

class PremiumLoyalUserEvaluable(private val minDays: Int) : Evaluable<UserContext>() {
    override fun matches(context: UserContext): Boolean =
        context.isPremium && context.accountAgeInDays >= minDays

    override fun specificity(): Int = 2
}

fun reusableEvaluables() {
    val LOYALTY_REWARD: Conditional<String, UserContext> =
        Conditional("loyalty_reward")

    ConfigBuilder.config {
        LOYALTY_REWARD with {
            default(value = "no-reward")

            // Loyal users (90+ days) get bronze
            rule {
                extension { LoyalUserEvaluable(minDays = 90) }
            } implies "bronze"

            // Premium users get silver
            rule {
                extension { PremiumUserEvaluable() }
            } implies "silver"

            // Premium + loyal users (365+ days) get gold
            rule {
                extension { PremiumLoyalUserEvaluable(minDays = 365) }
            } implies "gold"
        }
    }

    val newFreeUser = UserContext(
        locale = AppLocale.EN_US,
        platform = Platform.WEB,
        appVersion = Version(1, 0, 0),
        stableId = StableId.of("user-1"),
        isPremium = false,
        accountAgeInDays = 10
    )

    val loyalFreeUser = UserContext(
        locale = AppLocale.EN_US,
        platform = Platform.WEB,
        appVersion = Version(1, 0, 0),
        stableId = StableId.of("user-2"),
        isPremium = false,
        accountAgeInDays = 120
    )

    val newPremiumUser = UserContext(
        locale = AppLocale.EN_US,
        platform = Platform.WEB,
        appVersion = Version(1, 0, 0),
        stableId = StableId.of("user-3"),
        isPremium = true,
        accountAgeInDays = 30
    )

    val loyalPremiumUser = UserContext(
        locale = AppLocale.EN_US,
        platform = Platform.WEB,
        appVersion = Version(1, 0, 0),
        stableId = StableId.of("user-4"),
        isPremium = true,
        accountAgeInDays = 400
    )

    println("New free user: ${newFreeUser.evaluate(LOYALTY_REWARD)}")  // no-reward
    println("Loyal free user: ${loyalFreeUser.evaluate(LOYALTY_REWARD)}")  // bronze
    println("New premium user: ${newPremiumUser.evaluate(LOYALTY_REWARD)}")  // silver
    println("Loyal premium user: ${loyalPremiumUser.evaluate(LOYALTY_REWARD)}")  // gold
}

/**
 * Example 4: Combining Standard and Custom Targeting
 *
 * Use both base targeting and custom extensions together.
 */
fun combinedTargeting() {
    val BETA_FEATURE: Conditional<Boolean, UserContext> =
        Conditional("beta_feature")

    ConfigBuilder.config {
        BETA_FEATURE with {
            default(value = false)

            // iOS + Premium users
            rule {
                note("iOS premium users")
                platforms(Platform.IOS)
                extension { PremiumUserEvaluable() }
            } implies true

            // v2+ users with 30+ day accounts
            rule {
                note("Version 2+ loyal users")
                versions { min(Version(2, 0, 0)) }
                extension { LoyalUserEvaluable(minDays = 30) }
            } implies true
        }
    }

    val iosPremium = UserContext(
        locale = AppLocale.EN_US,
        platform = Platform.IOS,
        appVersion = Version(1, 5, 0),
        stableId = StableId.of("user-1"),
        isPremium = true,
        accountAgeInDays = 10
    )

    val androidLoyalV2 = UserContext(
        locale = AppLocale.EN_US,
        platform = Platform.ANDROID,
        appVersion = Version(2, 1, 0),
        stableId = StableId.of("user-2"),
        isPremium = false,
        accountAgeInDays = 100
    )

    val webFreeNewV1 = UserContext(
        locale = AppLocale.EN_US,
        platform = Platform.WEB,
        appVersion = Version(1, 0, 0),
        stableId = StableId.of("user-3"),
        isPremium = false,
        accountAgeInDays = 5
    )

    println("iOS Premium: ${iosPremium.evaluate(BETA_FEATURE)}")  // true
    println("Android Loyal v2+: ${androidLoyalV2.evaluate(BETA_FEATURE)}")  // true
    println("Web Free New: ${webFreeNewV1.evaluate(BETA_FEATURE)}")  // false
}

/**
 * Example 5: Context Hierarchies
 *
 * Create context hierarchies for different application areas.
 */
interface AppContext : Context

data class AuthenticatedContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    val userId: String,
    val accountType: String
) : AppContext

data class AdminContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    val userId: String,
    val accountType: String,
    val adminLevel: Int,
    val permissions: Set<String>
) : AuthenticatedContext(locale, platform, appVersion, stableId, userId, accountType)

fun contextHierarchies() {
    val PUBLIC_FEATURE: Conditional<Boolean, AppContext> =
        Conditional("public_feature")

    val USER_FEATURE: Conditional<Boolean, AuthenticatedContext> =
        Conditional("user_feature")

    val ADMIN_FEATURE: Conditional<Boolean, AdminContext> =
        Conditional("admin_feature")

    ConfigBuilder.config {
        PUBLIC_FEATURE with {
            default(value = true)
        }

        USER_FEATURE with {
            default(value = false)

            rule {
                extension {
                    object : Evaluable<AuthenticatedContext>() {
                        override fun matches(context: AuthenticatedContext): Boolean =
                            context.accountType == "premium"

                        override fun specificity(): Int = 1
                    }
                }
            } implies true
        }

        ADMIN_FEATURE with {
            default(value = false)

            rule {
                extension {
                    object : Evaluable<AdminContext>() {
                        override fun matches(context: AdminContext): Boolean =
                            context.adminLevel >= 2 &&
                            "manage_features" in context.permissions

                        override fun specificity(): Int = 1
                    }
                }
            } implies true
        }
    }

    val adminCtx = AdminContext(
        locale = AppLocale.EN_US,
        platform = Platform.WEB,
        appVersion = Version(1, 0, 0),
        stableId = StableId.of("admin-123"),
        userId = "admin-123",
        accountType = "admin",
        adminLevel = 3,
        permissions = setOf("manage_features", "view_analytics")
    )

    // Admin context can evaluate all levels of flags
    println("Admin - Public feature: ${adminCtx.evaluate(PUBLIC_FEATURE)}")
    println("Admin - User feature: ${adminCtx.evaluate(USER_FEATURE)}")
    println("Admin - Admin feature: ${adminCtx.evaluate(ADMIN_FEATURE)}")
}

fun main() {
    println("=== Basic Extended Context ===")
    basicExtendedContext()
    println("\n=== Enterprise Context ===")
    enterpriseContext()
    println("\n=== Reusable Evaluables ===")
    reusableEvaluables()
    println("\n=== Combined Targeting ===")
    combinedTargeting()
    println("\n=== Context Hierarchies ===")
    contextHierarchies()
}
