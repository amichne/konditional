# Custom Extensions

Extension predicates let you express domain-specific targeting logic with full type safety. Extensions receive your custom `Context` type as the receiver, so business rules are checked at compile-time.

---

## Basic Extension

Use `extension { ... }` to add custom predicates to a rule:

```kotlin
data class EnterpriseContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    val subscriptionTier: SubscriptionTier,
    val employeeCount: Int
) : Context

enum class SubscriptionTier { FREE, PRO, ENTERPRISE }

object PremiumFeatures : Namespace("premium") {
    val ADVANCED_ANALYTICS by boolean<EnterpriseContext>(default = false) {
        rule(true) {
            extension {
                subscriptionTier == SubscriptionTier.ENTERPRISE && employeeCount > 100
            }
        }
    }
}
```

**What happens:**
- Feature is parameterized with `EnterpriseContext`
- Inside `extension { ... }`, `this` is `EnterpriseContext`
- Accessing `subscriptionTier` and `employeeCount` is type-safe
- Attempting to evaluate with `Context` instead of `EnterpriseContext` is a compile error

---

## Why Extensions Are Type-Safe

Traditional flag systems use stringly-typed predicates:

```kotlin
// ✗ Stringly-typed (no compile-time checks)
if (flagClient.getBool("premium_analytics", false)) {
    // Who knows what this flag means?
}
```

Konditional enforces the contract via the type system:

```kotlin
// ✓ Type-safe (contract enforced by compiler)
val ctx = EnterpriseContext(
    locale = AppLocale.UNITED_STATES,
    platform = Platform.IOS,
    appVersion = Version.of(2, 1, 0),
    stableId = StableId.of("user-123"),
    subscriptionTier = SubscriptionTier.ENTERPRISE,
    employeeCount = 150
)

val enabled = PremiumFeatures.ADVANCED_ANALYTICS(ctx)
```

**Compile error if you try:**

```kotlin
val basicCtx: Context = Context(...)
// PremiumFeatures.ADVANCED_ANALYTICS(basicCtx)  // Compile error
```

---

## Extension Specificity

Extensions contribute to a rule's specificity (default: 1):

```kotlin
rule(true) {
    extension {  // specificity contribution = 1
        subscriptionTier == SubscriptionTier.ENTERPRISE
    }
}
```

You can specify a custom specificity:

```kotlin
rule(true) {
    extension(specificity = 5) {
        subscriptionTier == SubscriptionTier.ENTERPRISE && employeeCount > 100
    }
}
```

**Use case:** Ensure complex business rules take precedence over simple targeting criteria.

---

## Combining Extensions with Base Criteria

Extensions combine with base criteria via AND semantics:

```kotlin
rule(true) {
    platforms(Platform.IOS)
    locales(AppLocale.UNITED_STATES)
    extension {
        subscriptionTier == SubscriptionTier.ENTERPRISE
    }
}
```

**For this rule to match:**
- Platform must be iOS
- **AND** locale must be US
- **AND** subscriptionTier must be ENTERPRISE

Total specificity = 1 (platform) + 1 (locale) + 1 (extension) = 3

---

## Common Extension Patterns

### Subscription Tier Gating

```kotlin
val PREMIUM_FEATURE by boolean<EnterpriseContext>(default = false) {
    rule(true) {
        extension {
            subscriptionTier == SubscriptionTier.ENTERPRISE ||
            subscriptionTier == SubscriptionTier.PRO
        }
    }
}
```

### Conditional Rollout Based on Business Logic

```kotlin
val EXPERIMENTAL_CHECKOUT by boolean<EnterpriseContext>(default = false) {
    rule(true) {
        extension {
            subscriptionTier == SubscriptionTier.ENTERPRISE && employeeCount > 50
        }
        rampUp { 50.0 }
    }

    rule(true) {
        extension {
            subscriptionTier == SubscriptionTier.PRO
        }
        rampUp { 25.0 }
    }
}
```

**Evaluation:**
- Enterprise with >50 employees: 50% ramp-up (most specific)
- Pro tier: 25% ramp-up
- Others: default (false)

### Multi-Condition Business Rules

```kotlin
val BULK_DISCOUNT by boolean<EnterpriseContext>(default = false) {
    rule(true) {
        extension {
            (subscriptionTier == SubscriptionTier.ENTERPRISE && employeeCount > 100) ||
            (subscriptionTier == SubscriptionTier.PRO && employeeCount > 500)
        }
    }
}
```

---

## Testing Extensions

Extensions are pure predicates, so they're easy to test:

```kotlin
@Test
fun `enterprise users with 100+ employees get advanced analytics`() {
    val ctx = EnterpriseContext(
        locale = AppLocale.UNITED_STATES,
        platform = Platform.IOS,
        appVersion = Version.of(2, 1, 0),
        stableId = StableId.of("user-123"),
        subscriptionTier = SubscriptionTier.ENTERPRISE,
        employeeCount = 150
    )

    val enabled = PremiumFeatures.ADVANCED_ANALYTICS(ctx)
    assertTrue(enabled)
}

@Test
fun `enterprise users with fewer than 100 employees do not get advanced analytics`() {
    val ctx = EnterpriseContext(
        locale = AppLocale.UNITED_STATES,
        platform = Platform.IOS,
        appVersion = Version.of(2, 1, 0),
        stableId = StableId.of("user-123"),
        subscriptionTier = SubscriptionTier.ENTERPRISE,
        employeeCount = 50
    )

    val enabled = PremiumFeatures.ADVANCED_ANALYTICS(ctx)
    assertFalse(enabled)
}
```

---

## Limitations

### Extensions Are Not Serialized

Extension predicates are Kotlin lambdas defined in code. They **cannot** be serialized to JSON or updated via remote configuration.

**What's serialized:**
- Platform, locale, version, axis criteria
- Ramp-up percentage
- Allowlists

**What's NOT serialized:**
- Extension predicate logic

**Implication:** Extension logic is statically defined; you can change ramp-up percentages remotely, but not the predicate itself.

### Extensions Require Custom Context

If you use extensions, all call sites must provide the custom `Context` type:

```kotlin
// ✓ Correct
val enterpriseCtx: EnterpriseContext = buildEnterpriseContext()
val enabled = PremiumFeatures.ADVANCED_ANALYTICS(enterpriseCtx)

// ✗ Incorrect
val basicCtx: Context = Context(...)
// PremiumFeatures.ADVANCED_ANALYTICS(basicCtx)  // Compile error
```

---

## Next Steps

- [Rule Composition](/rules-and-targeting/rule-composition) — Available criteria
- [Specificity System](/rules-and-targeting/specificity-system) — Rule ordering
- [Advanced: Custom Context Types](/advanced/custom-context-types) — Deep dive into Context extension
- [Fundamentals: Core Primitives](/fundamentals/core-primitives) — Context definition
