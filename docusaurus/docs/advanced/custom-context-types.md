# Custom Context Types

Extend `Context` with domain-specific fields for type-safe business logic in extension predicates.

---

## Why Custom Context Types

Standard `Context` provides basic targeting (locale, platform, version, stableId). For business-specific targeting, extend `Context`:

```kotlin
// ✗ Without custom context (stringly-typed)
val enabled = flagClient.getBool("premium_analytics", false)
if (enabled && user.subscriptionTier == "enterprise") {
    // No compile-time safety
}

// ✓ With custom context (type-safe)
val enabled = PremiumFeatures.ADVANCED_ANALYTICS.evaluate(enterpriseContext)
// If extension checks subscriptionTier, it's compile-time verified
```

---

## Defining a Custom Context

### Basic Extension

```kotlin
data class EnterpriseContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    val subscriptionTier: SubscriptionTier,
    val employeeCount: Int,
    val accountAge: Duration
) : Context

enum class SubscriptionTier { FREE, PRO, ENTERPRISE }
```

### Using Custom Fields in Extension Predicates

```kotlin
object PremiumFeatures : Namespace("premium") {
    val ADVANCED_ANALYTICS by boolean<EnterpriseContext>(default = false) {
        rule(true) {
            extension {
                subscriptionTier == SubscriptionTier.ENTERPRISE && employeeCount > 100
            }
        }
    }

    val PRIORITY_SUPPORT by boolean<EnterpriseContext>(default = false) {
        rule(true) {
            extension {
                subscriptionTier == SubscriptionTier.ENTERPRISE ||
                (subscriptionTier == SubscriptionTier.PRO && accountAge > 365.days)
            }
        }
    }
}
```

**Type safety:**
- Inside `extension { ... }`, `this` is `EnterpriseContext`
- Accessing `subscriptionTier`, `employeeCount`, `accountAge` is type-safe
- Typos or wrong types are compile errors

---

## Pattern: Context Hierarchy

Build a hierarchy for different domains:

```kotlin
// Base context
interface AppContext : Context {
    val userId: UserId
    val sessionId: SessionId
}

// Commerce-specific context
data class CommerceContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    override val userId: UserId,
    override val sessionId: SessionId,
    val cartValue: Money,
    val loyaltyTier: LoyaltyTier,
    val purchaseHistory: PurchaseHistory
) : AppContext

// Content-specific context
data class ContentContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    override val userId: UserId,
    override val sessionId: SessionId,
    val contentPreferences: Set<ContentCategory>,
    val watchHistory: WatchHistory
) : AppContext
```

### Usage

```kotlin
object CommerceFeatures : Namespace("commerce") {
    val FREE_SHIPPING by boolean<CommerceContext>(default = false) {
        rule(true) {
            extension {
                cartValue > Money.of(50, USD) || loyaltyTier == LoyaltyTier.PLATINUM
            }
        }
    }
}

object ContentFeatures : Namespace("content") {
    val PERSONALIZED_FEED by boolean<ContentContext>(default = false) {
        rule(true) {
            extension {
                contentPreferences.size >= 3 && watchHistory.itemCount > 10
            }
        }
    }
}
```

---

## Pattern: Computed Properties

Add derived fields to avoid repetition:

```kotlin
data class EnterpriseContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    val subscriptionTier: SubscriptionTier,
    val employeeCount: Int,
    val accountCreatedAt: Instant
) : Context {
    val accountAge: Duration
        get() = Duration.between(accountCreatedAt, Instant.now())

    val isEnterpriseQualified: Boolean
        get() = subscriptionTier == SubscriptionTier.ENTERPRISE && employeeCount > 50

    val isPremiumQualified: Boolean
        get() = subscriptionTier in setOf(SubscriptionTier.PRO, SubscriptionTier.ENTERPRISE)
}

// Usage in extension predicates
val BULK_EXPORT by boolean<EnterpriseContext>(default = false) {
    rule(true) {
        extension { isEnterpriseQualified && accountAge > 30.days }
    }
}
```

---

## Pattern: Context Builder

Build context from disparate sources:

```kotlin
class EnterpriseContextBuilder(
    private val userService: UserService,
    private val subscriptionService: SubscriptionService,
    private val deviceInfo: DeviceInfo
) {
    suspend fun build(userId: UserId): EnterpriseContext {
        val user = userService.getUser(userId)
        val subscription = subscriptionService.getSubscription(user.subscriptionId)
        val company = subscriptionService.getCompany(subscription.companyId)

        return EnterpriseContext(
            locale = user.preferredLocale,
            platform = deviceInfo.platform,
            appVersion = deviceInfo.appVersion,
            stableId = StableId.of(user.id.value),
            subscriptionTier = subscription.tier,
            employeeCount = company.employeeCount,
            accountAge = Duration.between(user.createdAt, Instant.now())
        )
    }
}

// Usage
val contextBuilder = EnterpriseContextBuilder(userService, subscriptionService, deviceInfo)
val context = contextBuilder.build(userId)
val enabled = PremiumFeatures.ADVANCED_ANALYTICS.evaluate(context)
```

---

## Pattern: Context with Nullable Fields (Graceful Degradation)

For optional data, use nullable fields:

```kotlin
data class EnhancedContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    val userId: UserId?,  // Nullable for anonymous users
    val subscriptionTier: SubscriptionTier?,  // Nullable if not loaded
    val employeeCount: Int?  // Nullable if not available
) : Context

// Extension predicates handle nulls
val PREMIUM_FEATURE by boolean<EnhancedContext>(default = false) {
    rule(true) {
        extension {
            subscriptionTier == SubscriptionTier.ENTERPRISE &&
            (employeeCount ?: 0) > 100
        }
    }
}
```

---

## Testing Custom Context

### Unit Tests with Fixed Context

```kotlin
@Test
fun `enterprise users with 100+ employees get advanced analytics`() {
    val ctx = EnterpriseContext(
        locale = AppLocale.UNITED_STATES,
        platform = Platform.IOS,
        appVersion = Version.of(2, 1, 0),
        stableId = StableId.of("user-123"),
        subscriptionTier = SubscriptionTier.ENTERPRISE,
        employeeCount = 150,
        accountAge = 365.days
    )

    val enabled = PremiumFeatures.ADVANCED_ANALYTICS.evaluate(ctx)
    assertTrue(enabled)
}

@Test
fun `pro users with fewer than 100 employees do not get advanced analytics`() {
    val ctx = EnterpriseContext(
        locale = AppLocale.UNITED_STATES,
        platform = Platform.IOS,
        appVersion = Version.of(2, 1, 0),
        stableId = StableId.of("user-456"),
        subscriptionTier = SubscriptionTier.PRO,
        employeeCount = 50,
        accountAge = 30.days
    )

    val enabled = PremiumFeatures.ADVANCED_ANALYTICS.evaluate(ctx)
    assertFalse(enabled)
}
```

### Parameterized Tests

```kotlin
@ParameterizedTest
@MethodSource("enterpriseContexts")
fun `advanced analytics rules`(ctx: EnterpriseContext, expected: Boolean) {
    val actual = PremiumFeatures.ADVANCED_ANALYTICS.evaluate(ctx)
    assertEquals(expected, actual)
}

companion object {
    @JvmStatic
    fun enterpriseContexts() = listOf(
        Arguments.of(
            EnterpriseContext(..., subscriptionTier = SubscriptionTier.ENTERPRISE, employeeCount = 150),
            true
        ),
        Arguments.of(
            EnterpriseContext(..., subscriptionTier = SubscriptionTier.ENTERPRISE, employeeCount = 50),
            false
        ),
        Arguments.of(
            EnterpriseContext(..., subscriptionTier = SubscriptionTier.PRO, employeeCount = 150),
            false
        )
    )
}
```

---

## Limitations

### Extensions Are Not Serialized

Extension predicates are Kotlin lambdas. They **cannot** be serialized to JSON or updated via remote configuration.

**What's serialized:**
- Platform, locale, version, axis criteria
- Ramp-up percentage
- Allowlists

**What's NOT serialized:**
- Extension predicate logic

**Workaround:** If you need runtime-updatable business logic, use `axis(...)` with enumerated values instead of extensions.

### Type Constraint Propagates to Call Sites

If a feature uses `EnterpriseContext`, all call sites must provide `EnterpriseContext`:

```kotlin
// ✓ Correct
val enterpriseCtx: EnterpriseContext = buildEnterpriseContext()
PremiumFeatures.ADVANCED_ANALYTICS.evaluate(enterpriseCtx)

// ✗ Incorrect
val basicCtx: Context = Context(...)
// PremiumFeatures.ADVANCED_ANALYTICS.evaluate(basicCtx)  // Compile error
```

---

## Next Steps

- [Rules & Targeting: Custom Extensions](/rules-and-targeting/custom-extensions) — Extension predicate basics
- [Fundamentals: Core Primitives](/fundamentals/core-primitives) — Context definition
- [Advanced: Testing Strategies](/advanced/testing-strategies) — Testing patterns
