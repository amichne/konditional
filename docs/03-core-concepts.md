# Core Concepts

Design flags for your domain. This guide covers Features, Context, and Namespaces—the building blocks of type-safe
feature flags.

---

## Features

Features are type-safe flag definitions. Define them once, use them everywhere with compiler guarantees.

### FeatureContainer Pattern (Recommended)

The delegation pattern gives you property access with zero boilerplate:

```kotlin
object AppFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
    val DARK_MODE by boolean(default = false)
    val API_ENDPOINT by string(default = "https://api.example.com")
    val MAX_RETRIES by int(default = 3)
    val TIMEOUT by double(default = 30.0)
}

// Property access, not method calls
val enabled = context.evaluate(AppFeatures.DARK_MODE)  // Type: Boolean
val endpoint = context.evaluate(AppFeatures.API_ENDPOINT)  // Type: String
```

**What you get:**

- Property names become flag keys automatically
- Type inference from default values
- IDE autocomplete
- Compile-time validation

### Supported Types

| Type    | DSL Method  | Kotlin Type   | Example Default |
|---------|-------------|---------------|-----------------|
| Boolean | `boolean()` | `Boolean`     | `false`         |
| String  | `string()`  | `String`      | `"production"`  |
| Integer | `int()`     | `Int`         | `42`            |
| Decimal | `double()`  | `Double`      | `3.14`          |
| Enum    | `enum<E>()` | `E : Enum<E>` | `LogLevel.INFO` |

### Enum Flags

For type-safe variants, use enums:

```kotlin
enum class LogLevel { DEBUG, INFO, WARN, ERROR }
enum class Theme { LIGHT, DARK, AUTO }

object AppConfig : FeatureContainer<Namespace.Global>(Namespace.Global) {
    val LOG_LEVEL by enum<LogLevel, Context>(default = LogLevel.INFO)
    val THEME by enum<Theme, Context>(default = Theme.LIGHT)
}

// Type-safe evaluation
val level: LogLevel = context.evaluate(AppConfig.LOG_LEVEL)
```

**vs string-based systems:**

```kotlin
// LaunchDarkly - strings, can typo
val level = client.stringVariation("log-level", "info")
when (level) {
    "debug" -> ...
    "deubg" -> ...  // Typo! Never matches
}

// Konditional - compiler validates
val level = context.evaluate(AppConfig.LOG_LEVEL)
when (level) {
    LogLevel.DEBUG -> ...
    LogLevel.DEUBG -> ...  // Compile error
}
```

---

## Context

Context provides the evaluation environment. Every evaluation requires context—it tells Konditional *who* is asking and
*where* they are.

### Standard Fields

All contexts must have these four fields:

```kotlin
data class Context(
    val locale: AppLocale,       // App language/region
    val platform: Platform,      // Device type
    val appVersion: Version,     // App version (semantic)
    val stableId: StableId       // User/device identifier (hex)
)
```

**Example:**

```kotlin
val context = Context(
    locale = AppLocale.UNITED_STATES,
    platform = Platform.IOS,
    appVersion = Version.parse("2.1.0"),
    stableId = StableId.of("a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6")
)
```

### Platform

Where your code runs:

```kotlin
enum class Platform {
    IOS,        // iOS apps
    ANDROID,    // Android apps
}
```

Use in rules:

```kotlin
rule { platforms(Platform.IOS, Platform.ANDROID) } returns mobileValue
```

### AppLocale

User's language and region. 27 supported locales:

```kotlin
AppLocale.UNITED_STATES  // en-US
AppLocale.CANADA         // en-CA
AppLocale.UNITED_KINGDOM // en-GB
AppLocale.FRANCE         // fr-FR
AppLocale.JAPAN          // ja-JP
// ... 22 more
```

Use in rules:

```kotlin
rule { locales(AppLocale.UNITED_STATES, AppLocale.CANADA) } returns "en" locale
```

### Version

Semantic versioning (major.minor.patch):

```kotlin
val version = Version.parse("2.1.0")  // ParseResult<Version>
val version = Version.of(2, 1, 0)     // Direct construction
```

Use in rules for version targeting:

```kotlin
rule {
    versions {
        min(2, 0, 0)  // Minimum 2.0.0
        max(3, 0, 0)  // Below 3.0.0
    }
} returns v2Feature
```

### StableId

Stable user/device identifier for deterministic rollouts. Must be hexadecimal (32+ chars).

```kotlin
// Valid hex IDs
val id1 = StableId.of("a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6")
val id2 = StableId.of("deadbeefcafebabe1234567890abcdef")

// Invalid - will throw
val bad = StableId.of("user-123")  // Not hex!
```

**Convert existing IDs:**

```kotlin
fun toStableId(userId: String): StableId {
    val hash = MessageDigest.getInstance("SHA-256")
        .digest(userId.toByteArray())
        .joinToString("") { "%02x".format(it) }
    return StableId.of(hash)
}
```

**Why hex?** Ensures uniform distribution across rollout buckets (0-9999).

---

## Custom Contexts

Extend Context with business-specific fields for advanced targeting.

### Enterprise Example

```kotlin
data class EnterpriseContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    // Custom fields
    val subscriptionTier: SubscriptionTier,
    val organizationId: String,
    val userRole: UserRole,
    val employeeCount: Int
) : Context

enum class SubscriptionTier { FREE, PRO, ENTERPRISE }
enum class UserRole { VIEWER, EDITOR, ADMIN }

object PremiumFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
    val ADVANCED_ANALYTICS by boolean<EnterpriseContext>(default = false) {
        rule {
            extension {
                Evaluable.factory { ctx ->
                    ctx.subscriptionTier == SubscriptionTier.ENTERPRISE &&
                    ctx.employeeCount > 100
                }
            }
        } returns true
    }
}

// Evaluation with custom context
val ctx = EnterpriseContext(
    locale = AppLocale.UNITED_STATES,
    platform = Platform.WEB,
    appVersion = Version.parse("3.0.0"),
    stableId = StableId.of("..."),
    subscriptionTier = SubscriptionTier.ENTERPRISE,
    organizationId = "acme-corp",
    userRole = UserRole.ADMIN,
    employeeCount = 500
)

val enabled = ctx.evaluate(PremiumFeatures.ADVANCED_ANALYTICS)  // true
```

### Multi-Tenant Example

```kotlin
data class TenantContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    val tenantId: String,
    val featureAccess: Set<String>  // Purchased features
) : Context

object SaasFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
    val PDF_EXPORT by boolean<TenantContext>(default = false) {
        rule {
            extension {
                Evaluable.factory { ctx ->
                    "pdf-export" in ctx.featureAccess
                }
            }
        } returns true
    }
}
```

**Key principle:** Custom contexts enable type-safe business logic in rules without hardcoding user IDs or organization
names.

---

## Namespaces

Namespaces isolate features by domain. Each namespace has its own registry—features can't collide across namespaces.

### Built-In Namespaces

```kotlin
Namespace.Global            // Shared across app
Namespace.Authentication    // Login, SSO, 2FA
Namespace.Payments          // Billing, subscriptions
Namespace.Messaging         // Chat, notifications
Namespace.Search            // Search algorithms
Namespace.Recommendations   // Personalization
```

### Using Namespaces

```kotlin
object AuthFeatures : FeatureContainer<Namespace.Authentication>(
    Namespace.Authentication
) {
    val SOCIAL_LOGIN by boolean(default = false)
    val TWO_FACTOR_AUTH by boolean(default = true)
}

object PaymentFeatures : FeatureContainer<Namespace.Payments>(
    Namespace.Payments
) {
    val APPLE_PAY by boolean(default = false)
    val STRIPE_INTEGRATION by boolean(default = true)
}

// Each namespace isolated
Namespace.Authentication.load(authConfig)  // Only affects auth features
Namespace.Payments.load(paymentConfig)     // Only affects payment features
```

### Namespace Benefits

| Benefit                    | What It Means                                              |
|----------------------------|------------------------------------------------------------|
| **Compile-time isolation** | Can't accidentally use wrong namespace's features          |
| **Runtime isolation**      | Separate registries, independent configurations            |
| **Team ownership**         | Clear boundaries (Auth team owns Authentication namespace) |
| **Independent deployment** | Update one namespace without affecting others              |

### Custom Namespaces

For team-specific domains:

```kotlin
// Sealed class ensures compile-time exhaustiveness
sealed class TeamNamespace(id: String) : Namespace.Domain(id) {
    data object Recommendations : TeamNamespace("recommendations")
    data object Analytics : TeamNamespace("analytics")
}

object RecFeatures : FeatureContainer<TeamNamespace.Recommendations>(
    TeamNamespace.Recommendations
) {
    val COLLABORATIVE_FILTERING by boolean(default = true)
}
```

**Why sealed?** Governance—new namespaces require code review and PR approval.

---

## Organizational Patterns

### Pattern 1: By Feature Domain

Organize containers by business capability:

```kotlin
object UserManagement : FeatureContainer<Namespace.Global>(Namespace.Global) {
    val PROFILE_EDITING by boolean(default = true)
    val ACCOUNT_DELETION by boolean(default = true)
}

object ContentModeration : FeatureContainer<Namespace.Global>(Namespace.Global) {
    val AUTO_MODERATION by boolean(default = false)
    val MANUAL_REVIEW by boolean(default = true)
}
```

### Pattern 2: By Platform

Separate mobile and web flags:

```kotlin
object MobileFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
    val OFFLINE_MODE by boolean(default = true)
    val PUSH_NOTIFICATIONS by boolean(default = true)
}

object WebFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
    val DESKTOP_NOTIFICATIONS by boolean(default = false)
    val PROGRESSIVE_WEB_APP by boolean(default = false)
}
```

### Pattern 3: By Team

Use namespaces for team isolation:

```kotlin
// Team 1: Growth
object GrowthFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
    val REFERRAL_PROGRAM by boolean(default = false)
}

// Team 2: Monetization
object MonetizationFeatures : FeatureContainer<Namespace.Payments>(
    Namespace.Payments
) {
    val SUBSCRIPTION_UPSELL by boolean(default = true)
}
```

---

## Type Safety in Action

Konditional's type system prevents entire classes of errors:

### Impossible Runtime Type Errors

```kotlin
// LaunchDarkly - wrong type method
val retries = client.boolVariation("max-retries", false)  // Oops! Should be int
processWithRetries(retries)  // Type error at runtime

// Konditional - compiler catches it
object Config : FeatureContainer<Namespace.Global>(Namespace.Global) {
    val MAX_RETRIES by int(default = 3)
}
val retries = context.evaluate(Config.MAX_RETRIES)  // Type: Int, guaranteed
```

### Impossible Context Mismatches

```kotlin
// Wrong context type
val basicContext: Context = Context(...)
basicContext.evaluate(PremiumFeatures.ADVANCED_ANALYTICS)  // Compile error!
// Required: EnterpriseContext, Found: Context

// Correct
val enterpriseContext: EnterpriseContext = EnterpriseContext(...)
enterpriseContext.evaluate(PremiumFeatures.ADVANCED_ANALYTICS)  // ✓
```

### Impossible Namespace Collisions

```kotlin
// Can't mix features across namespaces
Namespace.Authentication.load(paymentConfig)  // Compile error!
// Type mismatch
```

---

## Best Practices

### 1. Use FeatureContainer for Everything

Unless you have a specific reason (like enum-based patterns), FeatureContainer delegation is the best API.

### 2. Design Contexts for Your Domain

Don't stuff everything into one mega-context. Create focused contexts:

```kotlin
// Good - focused
data class ShoppingContext(..., val cartTotal: Double) : Context
data class CheckoutContext(..., val paymentMethod: PaymentMethod) : Context

// Bad - kitchen sink
data class AppContext(..., val cartTotal: Double?, val paymentMethod: PaymentMethod?, ...) : Context
```

### 3. Organize by Team/Domain

Use namespaces to reflect your org structure. Each team should own a namespace.

### 4. Start Simple, Extend Later

Begin with basic Context, add custom fields only when needed:

```kotlin
// Start
val context = Context(locale, platform, version, stableId)

// Later, when needed
data class CustomContext(..., val newField: String) : Context
```

### 5. Use Enums for Variants

Don't use strings when enums make sense:

```kotlin
// Bad
val THEME by string(default = "light")  // Can typo "lite"

// Good
enum class Theme { LIGHT, DARK }
val THEME by enum<Theme, Context>(default = Theme.LIGHT)
```

---

## Next Steps

**Need advanced targeting?** See [Targeting & Rollouts](04-targeting-rollouts.md) for rules, specificity, and rollout
strategies.

**Want to evaluate flags?** See [Evaluation](05-evaluation.md) for evaluation methods and error handling.

**Loading from JSON?** See [Remote Configuration](06-remote-config.md) for serialization.
