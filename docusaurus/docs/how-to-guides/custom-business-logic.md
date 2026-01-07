# How-To: Integrate Custom Business Logic

## Problem

You need to:
- Target features based on business-specific attributes (subscription tier, account age, credit score)
- Maintain type safety for custom context fields
- Ensure business rules are checked at compile-time
- Integrate domain logic without string-based predicates

## Solution

### Step 1: Define Custom Context Type

```kotlin
data class BusinessContext(
    // Required Context fields
    override val stableId: StableId,
    override val platform: Platform,
    override val locale: Locale,
    override val appVersion: Version,

    // Custom business fields
    val subscriptionTier: SubscriptionTier,
    val accountAgeMonths: Int,
    val lifetimeRevenue: Double,
    val isEmployee: Boolean
) : Context

enum class SubscriptionTier { FREE, PRO, ENTERPRISE }
```

**Why custom context:** Business logic requires access to domain-specific attributes that aren't in the base `Context`.

### Step 2: Define Features with Custom Context

```kotlin
object PremiumFeatures : Namespace("premium") {
    val advancedAnalytics by boolean<BusinessContext>(default = false) {
        rule(true) {
            extension {
                subscriptionTier == SubscriptionTier.ENTERPRISE &&
                lifetimeRevenue > 10_000.0
            }
        }
    }

    val prioritySupport by boolean<BusinessContext>(default = false) {
        rule(true) {
            extension {
                subscriptionTier == SubscriptionTier.PRO ||
                subscriptionTier == SubscriptionTier.ENTERPRISE
            }
        }
    }

    val betaFeatures by boolean<BusinessContext>(default = false) {
        rule(true) {
            extension {
                isEmployee || (subscriptionTier == SubscriptionTier.ENTERPRISE && accountAgeMonths > 6)
            }
        }
    }
}
```

**How it works:**
- `extension { ... }` block receives `BusinessContext` as `this`
- Inside the block, you can access `subscriptionTier`, `accountAgeMonths`, etc.
- Type safety enforced at compile-time

### Step 3: Evaluate with Custom Context

```kotlin
fun evaluateFeatures(user: User): FeaturesForUser {
    val ctx = BusinessContext(
        stableId = StableId(user.id),
        platform = user.platform,
        locale = user.locale,
        appVersion = user.appVersion,
        subscriptionTier = user.subscriptionTier,
        accountAgeMonths = user.accountAgeMonths,
        lifetimeRevenue = user.lifetimeRevenue,
        isEmployee = user.isEmployee
    )

    return FeaturesForUser(
        advancedAnalytics = PremiumFeatures.advancedAnalytics.evaluate(ctx),
        prioritySupport = PremiumFeatures.prioritySupport.evaluate(ctx),
        betaFeatures = PremiumFeatures.betaFeatures.evaluate(ctx)
    )
}
```

### Step 4: Combine with Standard Predicates

```kotlin
val experimentalCheckout by boolean<BusinessContext>(default = false) {
    // Standard predicates
    rule(true) {
        platforms(Platform.IOS, Platform.ANDROID)  // Mobile only
        versions { min(2, 0, 0) }                   // Version 2.0.0+

        // Custom business logic
        extension {
            subscriptionTier == SubscriptionTier.PRO &&
            accountAgeMonths > 3
        }
    }

    // Ramp-up for qualified users
    rule(true) {
        extension { subscriptionTier == SubscriptionTier.ENTERPRISE }
        rampUp { 50.0 }
    }
}
```

**All predicates must match** (AND semantics). User must be on mobile, version 2.0.0+, PRO tier, and account age > 3 months.

## Guarantees

- **Type safety**: Custom context fields are type-checked at compile-time
  - **Mechanism**: Generic type parameter flows from feature definition to evaluation
  - **Boundary**: Must evaluate with correct context type (compiler enforces)

- **Compile-time errors for wrong context**: Can't evaluate with base `Context`
  - **Mechanism**: Type system prevents calling `evaluate(Context)` on a `Feature<T, BusinessContext, N>`
  - **Boundary**: Runtime serialization still accepts any Context subtype

- **Extension blocks are pure**: No side effects allowed
  - **Mechanism**: Extension blocks are simple boolean expressions
  - **Boundary**: You can call functions, but they shouldn't mutate state

## Advanced Patterns

### Pattern: Hierarchical Context

```kotlin
interface AccountContext : Context {
    val accountTier: AccountTier
    val accountCreatedAt: Instant
}

interface PaymentContext : AccountContext {
    val paymentMethod: PaymentMethod
    val hasValidCard: Boolean
}

// Features can target any level of the hierarchy
val basicFeature by boolean<AccountContext>(default = false) {
    rule(true) {
        extension { accountTier == AccountTier.PREMIUM }
    }
}

val paymentFeature by boolean<PaymentContext>(default = false) {
    rule(true) {
        extension { hasValidCard && paymentMethod == PaymentMethod.CREDIT_CARD }
    }
}
```

### Pattern: Computed Properties in Context

```kotlin
data class BusinessContext(
    override val stableId: StableId,
    /* ... other fields ... */
    val subscriptionTier: SubscriptionTier,
    val lifetimeRevenue: Double
) : Context {
    // Computed property available in extension blocks
    val isHighValueCustomer: Boolean
        get() = subscriptionTier == SubscriptionTier.ENTERPRISE && lifetimeRevenue > 50_000.0
}

val vipFeature by boolean<BusinessContext>(default = false) {
    rule(true) {
        extension { isHighValueCustomer }
    }
}
```

### Pattern: External Service Integration

```kotlin
data class RiskContext(
    override val stableId: StableId,
    /* ... */
    val riskScore: Double,  // From fraud service
    val isBlacklisted: Boolean  // From abuse service
) : Context

val highRiskCheckout by boolean<RiskContext>(default = false) {
    rule(true) {
        extension {
            riskScore < 0.5 && !isBlacklisted
        }
    }
}
```

**Important:** Risk score should be fetched *before* building context. Don't call external services inside extension blocks.

## What Can Go Wrong?

### Calling External Services Inside Extensions

```kotlin
// ✗ DON'T: External call inside extension block
val feature by boolean<BusinessContext>(default = false) {
    rule(true) {
        extension {
            val score = fraudService.getRiskScore(stableId)  // Slow, non-deterministic
            score < 0.5
        }
    }
}

// ✓ DO: Fetch data before building context
val riskScore = fraudService.getRiskScore(userId)
val ctx = BusinessContext(/* ... */, riskScore = riskScore)
val enabled = feature.evaluate(ctx)
```

**Why:** Extension blocks are evaluated on every call. External services introduce latency and non-determinism.

### Mutating State in Extensions

```kotlin
// ✗ DON'T: Side effects in extension block
var evaluationCount = 0
val feature by boolean<BusinessContext>(default = false) {
    rule(true) {
        extension {
            evaluationCount++  // Side effect!
            subscriptionTier == SubscriptionTier.PRO
        }
    }
}

// ✓ DO: Use observability hooks for side effects
AppFeatures.hooks.afterEvaluation.add { event ->
    evaluationCount++
}
```

### Using Base Context When Custom Required

```kotlin
val businessFeature by boolean<BusinessContext>(default = false) {
    rule(true) {
        extension { subscriptionTier == SubscriptionTier.PRO }
    }
}

// ✗ DON'T: Evaluate with base Context
val ctx: Context = Context(stableId = StableId("user"))
businessFeature.evaluate(ctx)  // Compile error: Context != BusinessContext

// ✓ DO: Evaluate with BusinessContext
val ctx: BusinessContext = buildBusinessContext(user)
businessFeature.evaluate(ctx)
```

### Forgetting to Check Null for Optional Fields

```kotlin
data class BusinessContext(
    /* ... */
    val companyName: String?  // Nullable
) : Context

val enterpriseFeature by boolean<BusinessContext>(default = false) {
    rule(true) {
        extension {
            companyName.startsWith("Acme")  // NullPointerException if null!
        }
    }
}

// ✓ FIX: Handle null
val enterpriseFeature by boolean<BusinessContext>(default = false) {
    rule(true) {
        extension {
            companyName?.startsWith("Acme") == true
        }
    }
}
```

## Testing Custom Business Logic

### Test Extension Evaluation

```kotlin
@Test
fun `enterprise users with high revenue get advanced analytics`() {
    val ctx = BusinessContext(
        stableId = StableId("user-123"),
        platform = Platform.IOS,
        locale = Locale.US,
        appVersion = Version.of(2, 0, 0),
        subscriptionTier = SubscriptionTier.ENTERPRISE,
        accountAgeMonths = 12,
        lifetimeRevenue = 15_000.0,
        isEmployee = false
    )

    val enabled = PremiumFeatures.advancedAnalytics.evaluate(ctx)

    assertTrue(enabled)
}

@Test
fun `pro users with low revenue do not get advanced analytics`() {
    val ctx = BusinessContext(
        stableId = StableId("user-456"),
        platform = Platform.IOS,
        locale = Locale.US,
        appVersion = Version.of(2, 0, 0),
        subscriptionTier = SubscriptionTier.PRO,  // PRO, not ENTERPRISE
        accountAgeMonths = 12,
        lifetimeRevenue = 5_000.0,  // Below threshold
        isEmployee = false
    )

    val enabled = PremiumFeatures.advancedAnalytics.evaluate(ctx)

    assertFalse(enabled)
}
```

### Parameterized Tests for Edge Cases

```kotlin
@ParameterizedTest
@CsvSource(
    "ENTERPRISE, 15000.0, true",   // Qualifies
    "ENTERPRISE, 9999.0, false",   // Revenue too low
    "PRO, 15000.0, false",         // Wrong tier
    "FREE, 15000.0, false"         // Wrong tier
)
fun `advanced analytics requires enterprise tier and high revenue`(
    tier: SubscriptionTier,
    revenue: Double,
    expected: Boolean
) {
    val ctx = BusinessContext(
        stableId = StableId("user"),
        platform = Platform.IOS,
        locale = Locale.US,
        appVersion = Version.of(2, 0, 0),
        subscriptionTier = tier,
        accountAgeMonths = 12,
        lifetimeRevenue = revenue,
        isEmployee = false
    )

    assertEquals(expected, PremiumFeatures.advancedAnalytics.evaluate(ctx))
}
```

### Test Extension + Standard Predicates

```kotlin
@Test
fun `experimental checkout requires mobile, v2+, PRO tier, and 3+ months`() {
    // All criteria match
    val qualifiedCtx = BusinessContext(
        stableId = StableId("user-1"),
        platform = Platform.IOS,  // Mobile ✓
        locale = Locale.US,
        appVersion = Version.of(2, 1, 0),  // v2+ ✓
        subscriptionTier = SubscriptionTier.PRO,  // PRO ✓
        accountAgeMonths = 6,  // 3+ months ✓
        lifetimeRevenue = 0.0,
        isEmployee = false
    )
    assertTrue(PremiumFeatures.experimentalCheckout.evaluate(qualifiedCtx))

    // Missing one criterion (wrong platform)
    val unqualifiedCtx = qualifiedCtx.copy(platform = Platform.WEB)
    assertFalse(PremiumFeatures.experimentalCheckout.evaluate(unqualifiedCtx))
}
```

## Real-World Example

### E-Commerce Targeting

```kotlin
data class EcommerceContext(
    override val stableId: StableId,
    override val platform: Platform,
    override val locale: Locale,
    override val appVersion: Version,

    val cartValue: Double,
    val orderCount: Int,
    val daysSinceLastOrder: Int?,
    val hasActiveLoyaltyMembership: Boolean
) : Context

object CheckoutFeatures : Namespace("checkout") {
    val expressCheckout by boolean<EcommerceContext>(default = false) {
        // High-value customers
        rule(true) {
            extension {
                cartValue > 200.0 && orderCount > 5
            }
        }

        // Loyal members, regardless of cart value
        rule(true) {
            extension {
                hasActiveLoyaltyMembership && orderCount > 3
            }
        }
    }

    val winbackDiscount by boolean<EcommerceContext>(default = false) {
        // Lapsed customers (30+ days since last order)
        rule(true) {
            extension {
                daysSinceLastOrder?.let { it > 30 } == true && orderCount > 0
            }
        }
    }
}
```

## Next Steps

- [Core Primitives](/fundamentals/core-primitives) — Understanding Context and Feature types
- [Type Safety](/fundamentals/type-safety) — How type safety works across boundaries
- [Namespace Isolation](/how-to-guides/namespace-isolation) — Organizing features by domain
- [Testing Features](/how-to-guides/testing-features) — Comprehensive testing patterns
