# Business Logic Targeting with Custom Context + Extension

Use strongly-typed extensions for domain logic that should not be remotely mutable.

```kotlin
data class EnterpriseContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    val subscriptionTier: SubscriptionTier,
    val employeeCount: Int,
) : Context

enum class SubscriptionTier { FREE, PRO, ENTERPRISE }

object PremiumFeatures : Namespace("premium") {
    val advancedAnalytics by boolean<EnterpriseContext>(default = false) {
        rule(true) {
            extension { subscriptionTier == SubscriptionTier.ENTERPRISE && employeeCount > 100 }
        }
    }
}
```

- **Guarantee**: Extension predicates are type-safe and enforced at compile time.
- **Mechanism**: `Feature<T, EnterpriseContext>` makes the extension receiver strongly typed.
- **Boundary**: Extension logic is not serialized; only its rule parameters (e.g., ramp-up) can be updated remotely.

---
