---
name: feature-definition
description: Define type-safe Konditional features with compile-time guarantees, proper targeting rules, and specificity-based precedence
---

# Konditional Feature Definition

## Instructions

### Choose Feature Type
| Use Case | Type | Method |
|----------|------|--------|
| On/off toggle | Boolean | `boolean(default = ...)` |
| Variants (A/B/C) | Enum | `enum(default = ...)` |
| URL/endpoint | String | `string(default = ...)` |
| Threshold/count | Int | `integer(default = ...)` |
| Timeout/rate | Double | `double(default = ...)` |
| Complex config | Data class | `custom(default = ...)` |

### Feature Definition Template
```kotlin
object [NamespaceName] : Namespace("[namespace-id]") {
    val [featureName] by [type]<[ContextType]>(default = [defaultValue]) {
        // Optional rules
        rule([value]) {
            [criteria]
            rollout { [percentage] }
        }
    }
}
```

### Targeting Criteria (Combined with AND)
- `platforms(Platform.IOS, Platform.ANDROID, ...)`
- `locales(AppLocale.UNITED_STATES, ...)`
- `versions { min(major, minor, patch); max(...) }`
- `rollout { percentage }` (0.0 to 100.0)
- `allowlist(StableId.of("user-id"))` (bypasses rollout)
- `axis("dimensionId", "value1", "value2")` (custom dimensions)
- `extension { customLogic }` (requires custom Context)

### Rule Precedence
Rules are sorted by **specificity** (number of criteria), most specific first:

```kotlin
// Evaluated in this order (specificity 3 → 2 → 1 → 0):
rule(value) { platforms(IOS); locales(US); versions { min(2,0,0) } } // 3
rule(value) { platforms(IOS); locales(US) }                          // 2
rule(value) { platforms(IOS) }                                       // 1
rule(value) { rollout { 50.0 } }                                     // 0
```

## Examples

### Boolean Feature with Platform Targeting
```kotlin
object AppFeatures : Namespace("app") {
    val darkMode by boolean<Context>(default = false) {
        rule(true) {
            platforms(Platform.IOS)
            rollout { 50.0 }
        }
    }
}

// Usage
val enabled: Boolean = AppFeatures.darkMode.evaluate(context)
```

### Enum for Variants (Avoid Boolean Explosion)
**Wrong** (boolean explosion):
```kotlin
val CHECKOUT_V1 by boolean(default = true)
val CHECKOUT_V2 by boolean(default = false)
val CHECKOUT_V3 by boolean(default = false)

// Usage requires complex logic
if (CHECKOUT_V1 && !CHECKOUT_V2 && !CHECKOUT_V3) { ... }
```

**Right** (typed variants):
```kotlin
enum class CheckoutVersion { V1, V2, V3 }

val checkoutVersion by enum<CheckoutVersion, Context>(default = CheckoutVersion.V1) {
    rule(CheckoutVersion.V2) { rollout { 33.0 } }
    rule(CheckoutVersion.V3) { rollout { 66.0 } }
}

// Usage is clean
when (AppFeatures.checkoutVersion.evaluate(context)) {
    CheckoutVersion.V1 -> v1Checkout()
    CheckoutVersion.V2 -> v2Checkout()
    CheckoutVersion.V3 -> v3Checkout()
}
```

### Custom Context with Extension Logic
```kotlin
data class EnterpriseContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    val subscriptionTier: SubscriptionTier,
    val employeeCount: Int
) : Context

object PremiumFeatures : Namespace("premium") {
    val advancedAnalytics by boolean<EnterpriseContext>(default = false) {
        rule(true) {
            extension {
                subscriptionTier == SubscriptionTier.ENTERPRISE && employeeCount > 100
            }
        }
    }
}

// Usage requires EnterpriseContext (compile-time enforced)
val enabled = PremiumFeatures.advancedAnalytics.evaluate(enterpriseContext)
```

### String Feature for Endpoints
```kotlin
val apiEndpoint by string<Context>(default = "https://api.example.com") {
    rule("https://api-ios.example.com") { platforms(Platform.IOS) }
    rule("https://api-android.example.com") { platforms(Platform.ANDROID) }
    rule("https://api-web.example.com") { platforms(Platform.WEB) }
}
```

### Allowlist for Internal Testing
```kotlin
val experimentalFeature by boolean<Context>(default = false) {
    allowlist(
        StableId.of("tester-1"),
        StableId.of("tester-2")
    )
    rule(true) {
        platforms(Platform.IOS)
        rollout { 5.0 }
    }
}

// Allowlisted users bypass rollout after criteria match
```

### Common Mistakes

**Mistake 1: Using String for variants**
**Wrong**: `val variant by string(default = "control")` with values "control", "v1", "v2"
**Right**: Define an enum and use `enum<Variant, Context>(default = Variant.CONTROL)`
**Why**: Enums prevent typos and make variants explicit

**Mistake 2: Multiple rules same specificity**
```kotlin
// Both have specificity 1 - evaluation order undefined
rule("value1") { platforms(Platform.IOS) }
rule("value2") { locales(AppLocale.UNITED_STATES) }

// Better: make one more specific
rule("value1") { platforms(Platform.IOS); locales(AppLocale.UNITED_STATES) }
rule("value2") { locales(AppLocale.UNITED_STATES) }
```
