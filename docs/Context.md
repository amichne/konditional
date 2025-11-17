# Context: Evaluation Environment

The `Context` interface defines the evaluation environment for feature flags. It provides the standard targeting dimensions that determine which rules match and which values are returned during flag evaluation.

---

## Overview

Every feature flag evaluation requires a `Context` instance. The context represents the current execution environment, including user preferences, platform information, application version, and a stable identifier for deterministic rollouts.

```kotlin
import io.amichne.konditional.context.*

val context = Context(
    locale = AppLocale.EN_US,
    platform = Platform.IOS,
    appVersion = Version.parse("2.1.0"),
    stableId = StableId.of("a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4")
)

val isDarkMode = context.evaluate(AppFeatures.DARK_MODE)
```

**Key characteristics:**
- **Immutable**: Context instances should never change after creation
- **Thread-safe**: Safe to share across threads
- **Lightweight**: Contains only essential targeting information
- **Extensible**: Can be extended with custom fields for domain-specific targeting

---

## Context Interface

The base `Context` interface defines four required properties:

```kotlin
interface Context {
    val locale: AppLocale
    val platform: Platform
    val appVersion: Version
    val stableId: StableId
}
```

### Required Properties

| Property | Type | Purpose |
|----------|------|---------|
| `locale` | `AppLocale` | User's language and regional settings |
| `platform` | `Platform` | Platform where the app is running |
| `appVersion` | `Version` | Semantic version of the application |
| `stableId` | `StableId` | Unique identifier for deterministic bucketing |

---

## Creating Context Instances

### Factory Function

The simplest way to create a context is using the companion object factory:

```kotlin
val context = Context(
    locale = AppLocale.EN_US,
    platform = Platform.IOS,
    appVersion = Version.parse("2.1.0"),
    stableId = StableId.of("a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4")
)
```

This creates an anonymous implementation of the `Context` interface. It's perfect for quick use, testing, and simple applications.

### Data Class Implementation

For production applications, define a data class:

```kotlin
data class AppContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId
) : Context

val context = AppContext(
    locale = AppLocale.EN_US,
    platform = Platform.IOS,
    appVersion = Version.parse("2.1.0"),
    stableId = StableId.of("a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4")
)
```

**Benefits of data classes:**
- `copy()` for creating modified instances
- Structural equality for testing
- `toString()` for debugging
- Named properties improve clarity

---

## Context Components

### Platform

The `Platform` enum identifies where your application is running:

```kotlin
enum class Platform {
    IOS,      // iOS devices (iPhone, iPad)
    ANDROID,  // Android devices
    WEB       // Web browsers
}
```

**Usage:**
```kotlin
val context = Context(
    platform = Platform.IOS,
    // ... other properties
)
```

**Common patterns:**
```kotlin
// Platform-specific targeting
rule {
    platforms(Platform.IOS, Platform.ANDROID)
} implies mobileValue

// Single platform
rule {
    platforms(Platform.WEB)
} implies webValue
```

---

### AppLocale

The `AppLocale` enum represents language and regional settings:

```kotlin
enum class AppLocale {
    EN_US,  // English (United States)
    ES_US,  // Spanish (United States)
    EN_CA,  // English (Canada)
    HI_IN   // Hindi (India)
}
```

**Usage:**
```kotlin
val context = Context(
    locale = AppLocale.EN_US,
    // ... other properties
)
```

**Locale-based targeting:**
```kotlin
// Multiple locales
rule {
    locales(AppLocale.EN_US, AppLocale.EN_CA)
} implies englishValue

// Single locale
rule {
    locales(AppLocale.HI_IN)
} implies hindiValue
```

**Note:** Add new locales to the enum as your application expands to new markets.

---

### Version

The `Version` class represents semantic versioning (major.minor.patch):

```kotlin
data class Version(
    val major: Int,
    val minor: Int,
    val patch: Int
) : Comparable<Version>
```

#### Creating Versions

**Direct construction:**
```kotlin
val version = Version(2, 1, 0)  // Version 2.1.0
```

**Using factory method:**
```kotlin
val version = Version.of(2, 1, 0)  // Version 2.1.0
```

**Parsing from string:**
```kotlin
val version = Version.parse("2.1.0")  // Version 2.1.0
val version = Version.parse("2.1")    // Version 2.1.0
val version = Version.parse("2")      // Version 2.0.0
```

**Default version:**
```kotlin
val default = Version.default  // Version(-1, -1, -1)
```

#### Version Comparison

`Version` implements `Comparable<Version>`, enabling natural ordering:

```kotlin
val v1 = Version(2, 0, 0)
val v2 = Version(2, 1, 0)
val v3 = Version(3, 0, 0)

println(v1 < v2)   // true
println(v2 <= v2)  // true
println(v3 > v2)   // true

// Use in version ranges
rule {
    versions {
        min(2, 0, 0)  // >= 2.0.0
        max(3, 0, 0)  // <= 3.0.0
    }
} implies newFeatureValue
```

**Comparison logic:**
1. Compare major versions first
2. If equal, compare minor versions
3. If equal, compare patch versions

---

### StableId

The `StableId` represents a unique, stable identifier used for deterministic rollout bucketing:

```kotlin
sealed interface StableId {
    val id: String
    val hexId: HexId
}
```

#### Creating StableIds

```kotlin
// From user ID
val stableId = StableId.of("a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4")

// From UUID (convert to hex first)
val uuid = UUID.randomUUID()
val stableId = StableId.of(uuid.toString().replace("-", ""))

// From any unique identifier
val stableId = StableId.of(userId.toHexString())
```

#### Validation

`StableId.of()` validates that the input is a valid hexadecimal string:

```kotlin
// Valid - hex string
StableId.of("abc123")  // OK

// Invalid - not hex
StableId.of("xyz123")  // Throws IllegalArgumentException

// Invalid - empty
StableId.of("")        // Throws IllegalArgumentException
```

**Requirements:**
- Must be a valid hexadecimal string (0-9, a-f, A-F)
- Must be non-empty
- Should be consistent for the same user/device across sessions

#### Best Practices

**Choose the right identifier level:**

```kotlin
// User-level: Same user sees same experience across devices
val stableId = StableId.of(userId)

// Device-level: Different devices see different experiences
val stableId = StableId.of(deviceId)

// Session-level: Different sessions see different experiences
val stableId = StableId.of(sessionId)

// Anonymous: For logged-out users
val anonymousId = getOrCreateAnonymousId()  // Store in localStorage/cookies
val stableId = StableId.of(anonymousId)
```

**Identifier requirements:**
- **Persistent**: Survives app restarts and sessions
- **Unique**: Different users/devices have different IDs
- **Consistent**: Same user/device always has the same ID
- **Stable**: Doesn't change over time

**What NOT to use:**
- Timestamps (changes every request)
- Random values (not stable)
- Sensitive data (hashed in logs)
- IP addresses (changes with network)

---

### Rollout

The `Rollout` value class represents a percentage (0-100%) for gradual feature deployment.

#### Creating Rollouts

**From Double:**
```kotlin
val rollout = Rollout.of(50.0)   // 50%
val rollout = Rollout.of(25.5)   // 25.5%
val rollout = Rollout.of(100.0)  // 100%
```

**From Int:**
```kotlin
val rollout = Rollout.of(50)   // 50%
val rollout = Rollout.of(100)  // 100%
```

**From String:**
```kotlin
val rollout = Rollout.of("50.0")   // 50%
val rollout = Rollout.of("75")     // 75%
```

**From another Rollout:**
```kotlin
val rollout1 = Rollout.of(50.0)
val rollout2 = Rollout.of(rollout1)  // Copy
```

#### Constants

```kotlin
val max = Rollout.MAX          // 100%
val default = Rollout.default  // 100%
```

#### Range Validation

Rollout percentages must be between 0.0 and 100.0:

```kotlin
Rollout.of(0.0)    // OK - 0%
Rollout.of(50.0)   // OK - 50%
Rollout.of(100.0)  // OK - 100%
Rollout.of(150.0)  // Throws IllegalArgumentException
Rollout.of(-10.0)  // Throws IllegalArgumentException
```

#### Usage in Rules

```kotlin
rule {
    platforms(Platform.IOS)
    rollout = Rollout.of(25.0)  // 25% of iOS users
} implies true

rule {
    rollout = Rollout.MAX  // 100% of all users
} implies true
```

#### How Rollouts Work

Rollout bucketing is **deterministic** and **flag-specific**:

1. Hash the combination of `salt + flagKey + stableId` using SHA-256
2. Map hash to bucket (0-9999)
3. Compare bucket to rollout percentage × 100

```kotlin
// Example: 50% rollout
// User with stableId "abc123" → bucket 4752
// 4752 < 5000 (50% × 100) → User is in rollout ✓

// Same user, different flag → different bucket
// Different deterministic hash per flag
```

**Key properties:**
- **Deterministic**: Same user always sees same experience for a flag
- **Independent**: Each flag buckets users independently
- **Uniform**: Users distributed evenly across buckets
- **Stable**: Bucketing doesn't change unless salt changes

**Gradual rollout strategy:**
```kotlin
// Week 1: 10%
rollout = Rollout.of(10.0)

// Week 2: 25%
rollout = Rollout.of(25.0)

// Week 3: 50%
rollout = Rollout.of(50.0)

// Week 4: 100%
rollout = Rollout.MAX
```

Users in the 10% bucket stay enabled as you increase to 25%, 50%, and 100%.

---

## Custom Context Extensions

One of Konditional's most powerful features is the ability to extend `Context` with domain-specific fields. This enables targeting based on your business logic.

### Extension Pattern

```kotlin
data class EnterpriseContext(
    // Required base properties
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,

    // Custom enterprise fields
    val organizationId: String,
    val subscriptionTier: SubscriptionTier,
    val userRole: UserRole
) : Context

enum class SubscriptionTier { BASIC, PREMIUM, ENTERPRISE }
enum class UserRole { EDITOR, ADMIN, OWNER }
```

### Using Custom Contexts with Features

Define features that require your custom context:

```kotlin
object EnterpriseFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    val advanced_analytics by boolean<EnterpriseContext>(default = false)
    val custom_branding by boolean<EnterpriseContext>(default = false)
    val api_access by boolean<EnterpriseContext>(default = false)
}
```

### Custom Rules with Extensions

Create custom `Evaluable` implementations that access your context fields:

```kotlin
data class EnterpriseRule(
    val requiredTier: SubscriptionTier? = null,
    val requiredRole: UserRole? = null
) : Evaluable<EnterpriseContext> {
    override fun matches(context: EnterpriseContext): Boolean =
        (requiredTier == null || context.subscriptionTier >= requiredTier) &&
        (requiredRole == null || context.userRole >= requiredRole)
}

// Configure feature with custom rule
EnterpriseFeatures.advanced_analytics.update {
    default(false)
    rule {
        extension {
            EnterpriseRule(requiredTier = SubscriptionTier.ENTERPRISE)
        }
    } implies true
}
```

### Common Extension Patterns

#### Multi-Tenancy Context

```kotlin
data class TenantContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    val tenantId: String,
    val tenantRegion: String,
    val planFeatures: Set<String>
) : Context
```

#### Experimentation Context

```kotlin
data class ExperimentContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    val experimentGroups: Set<String>,
    val sessionId: String
) : Context
```

#### User Context

```kotlin
data class UserContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    val userId: String,
    val isAuthenticated: Boolean,
    val accountAge: Duration,
    val subscriptionStatus: SubscriptionStatus
) : Context
```

---

## Context Polymorphism

Konditional's type system enforces context requirements at compile time through generic type parameters.

### Type Safety Guarantees

```kotlin
// Feature requiring base Context
object AppFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    val DARK_MODE by boolean<Context>(default = false)
}

// Feature requiring EnterpriseContext
object EnterpriseFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    val ADVANCED_ANALYTICS by boolean<EnterpriseContext>(default = false)
}
```

### Evaluation Type Safety

```kotlin
// Base context
val baseContext: Context = Context(
    locale = AppLocale.EN_US,
    platform = Platform.WEB,
    appVersion = Version.parse("2.0.0"),
    stableId = StableId.of("abc123def456abc123def456abc123de")
)

// Can evaluate features requiring Context
baseContext.evaluate(AppFeatures.DARK_MODE)  // ✓ OK

// Cannot evaluate features requiring EnterpriseContext
// baseContext.evaluate(EnterpriseFeatures.ADVANCED_ANALYTICS)  // ✗ Compile error!
```

### Subtype Polymorphism

Extended contexts can evaluate base context features:

```kotlin
// Enterprise context (extends Context)
val enterpriseContext: EnterpriseContext = EnterpriseContext(
    locale = AppLocale.EN_US,
    platform = Platform.WEB,
    appVersion = Version.parse("2.0.0"),
    stableId = StableId.of("abc123def456abc123def456abc123de"),
    organizationId = "org-123",
    subscriptionTier = SubscriptionTier.ENTERPRISE,
    userRole = UserRole.ADMIN
)

// Can evaluate base Context features (covariance)
enterpriseContext.evaluate(AppFeatures.DARK_MODE)  // ✓ OK

// Can evaluate EnterpriseContext features
enterpriseContext.evaluate(EnterpriseFeatures.ADVANCED_ANALYTICS)  // ✓ OK
```

**Why this works:** `EnterpriseContext` is a subtype of `Context`, so it can be used wherever `Context` is required.

---

## Context Patterns and Best Practices

### Immutability

Always use immutable data classes with `val` properties:

```kotlin
// Good: Immutable
data class AppContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId
) : Context

// Bad: Mutable - NOT thread-safe
class MutableContext(
    override var locale: AppLocale,
    override var platform: Platform,
    override var appVersion: Version,
    override var stableId: StableId
) : Context  // Don't do this!
```

### Context Factories

Create factory functions for common context construction patterns:

```kotlin
object ContextFactory {
    fun fromRequest(request: HttpRequest): AppContext =
        AppContext(
            locale = parseLocale(request.headers["Accept-Language"]),
            platform = parsePlatform(request.headers["User-Agent"]),
            appVersion = parseVersion(request.headers["X-App-Version"]),
            stableId = StableId.of(
                request.cookies["user_id"] ?: generateAnonymousId()
            )
        )

    fun fromUser(user: User, device: Device): AppContext =
        AppContext(
            locale = user.preferredLocale,
            platform = device.platform,
            appVersion = device.appVersion,
            stableId = StableId.of(user.id)
        )

    fun forTesting(
        locale: AppLocale = AppLocale.EN_US,
        platform: Platform = Platform.WEB,
        version: String = "1.0.0",
        userId: String = "test-user"
    ): AppContext =
        AppContext(
            locale = locale,
            platform = platform,
            appVersion = Version.parse(version),
            stableId = StableId.of(userId.md5())
        )
}
```

### Context Builders

For complex contexts with many fields, consider a builder pattern:

```kotlin
class EnterpriseContextBuilder {
    private var locale: AppLocale = AppLocale.EN_US
    private var platform: Platform = Platform.WEB
    private var appVersion: Version = Version(1, 0, 0)
    private var stableId: StableId? = null
    private var organizationId: String? = null
    private var subscriptionTier: SubscriptionTier = SubscriptionTier.BASIC
    private var userRole: UserRole = UserRole.EDITOR

    fun locale(locale: AppLocale) = apply { this.locale = locale }
    fun platform(platform: Platform) = apply { this.platform = platform }
    fun version(version: Version) = apply { this.appVersion = version }
    fun stableId(id: String) = apply { this.stableId = StableId.of(id) }
    fun organization(id: String) = apply { this.organizationId = id }
    fun tier(tier: SubscriptionTier) = apply { this.subscriptionTier = tier }
    fun role(role: UserRole) = apply { this.userRole = role }

    fun build(): EnterpriseContext =
        EnterpriseContext(
            locale = locale,
            platform = platform,
            appVersion = appVersion,
            stableId = checkNotNull(stableId) { "stableId is required" },
            organizationId = checkNotNull(organizationId) { "organizationId is required" },
            subscriptionTier = subscriptionTier,
            userRole = userRole
        )
}

// Usage
val context = EnterpriseContextBuilder()
    .locale(AppLocale.EN_US)
    .platform(Platform.WEB)
    .version(Version.parse("2.0.0"))
    .stableId("abc123def456abc123def456abc123de")
    .organization("org-123")
    .tier(SubscriptionTier.ENTERPRISE)
    .role(UserRole.ADMIN)
    .build()
```

### Default Test Contexts

Create reusable test contexts to reduce boilerplate:

```kotlin
object TestContexts {
    val default = Context(
        locale = AppLocale.EN_US,
        platform = Platform.WEB,
        appVersion = Version(1, 0, 0),
        stableId = StableId.of("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
    )

    val mobile = default.copy(platform = Platform.IOS)
    val android = default.copy(platform = Platform.ANDROID)
    val web = default.copy(platform = Platform.WEB)

    fun withVersion(version: String) = default.copy(
        appVersion = Version.parse(version)
    )

    fun withStableId(id: String) = default.copy(
        stableId = StableId.of(id)
    )
}

// Usage in tests
@Test
fun `test iOS behavior`() {
    val result = TestContexts.mobile.evaluate(Features.MOBILE_FEATURE)
    assertTrue(result)
}
```

### Context Middleware

Extract context from HTTP requests consistently:

```kotlin
class FeatureFlagMiddleware : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val context = extractContext(request)

        // Store in thread-local or request scope
        ContextHolder.set(context)

        return chain.proceed(request)
    }

    private fun extractContext(request: Request): AppContext =
        AppContext(
            locale = parseLocale(request.header("Accept-Language")),
            platform = parsePlatform(request.header("User-Agent")),
            appVersion = parseVersion(request.header("X-App-Version") ?: "1.0.0"),
            stableId = StableId.of(
                request.cookie("user_id") ?: generateAnonymousId()
            )
        )
}
```

---

## Common Pitfalls

### Using Non-Stable IDs

```kotlin
// Bad: Timestamp changes every request
val stableId = StableId.of(System.currentTimeMillis().toString(16))

// Bad: Random value is not stable
val stableId = StableId.of(UUID.randomUUID().toString().replace("-", ""))

// Good: Persistent user ID
val stableId = StableId.of(userId.md5())

// Good: Persistent device ID
val stableId = StableId.of(deviceId)
```

### Mutable Context

```kotlin
// Bad: Mutable properties break thread safety
var currentContext = Context(...)
currentContext = newContext  // Race condition!

// Good: Immutable with new instances
val context1 = Context(...)
val context2 = context1.copy(platform = Platform.ANDROID)
```

### Missing Hex Validation

```kotlin
// Bad: Not hex - throws exception at runtime
StableId.of("not-a-hex-string")

// Good: Validate before creating StableId
fun createStableId(id: String): StableId =
    StableId.of(id.md5())  // Convert to hex first
```

### Forgetting Context Requirements

```kotlin
// Bad: Can't evaluate enterprise feature with base context
val baseContext: Context = Context(...)
// baseContext.evaluate(EnterpriseFeatures.ADVANCED_ANALYTICS)  // Compile error!

// Good: Use correct context type
val enterpriseContext: EnterpriseContext = EnterpriseContext(...)
enterpriseContext.evaluate(EnterpriseFeatures.ADVANCED_ANALYTICS)  // OK
```

---

## Examples

### Basic Context Creation

```kotlin
import io.amichne.konditional.context.*

val context = Context(
    locale = AppLocale.EN_US,
    platform = Platform.IOS,
    appVersion = Version.parse("2.1.0"),
    stableId = StableId.of("a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4")
)

val result = context.evaluate(AppFeatures.DARK_MODE)
```

### Custom Enterprise Context

```kotlin
data class EnterpriseContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    val organizationId: String,
    val subscriptionTier: SubscriptionTier,
    val userRole: UserRole
) : Context

enum class SubscriptionTier { BASIC, PREMIUM, ENTERPRISE }
enum class UserRole { EDITOR, ADMIN, OWNER }

val context = EnterpriseContext(
    locale = AppLocale.EN_US,
    platform = Platform.WEB,
    appVersion = Version.parse("2.0.0"),
    stableId = StableId.of("abc123def456abc123def456abc123de"),
    organizationId = "org-456",
    subscriptionTier = SubscriptionTier.ENTERPRISE,
    userRole = UserRole.ADMIN
)

val hasAnalytics = context.evaluate(EnterpriseFeatures.advanced_analytics)
```

### Multi-Platform Context Factory

```kotlin
object PlatformContext {
    fun forIOS(userId: String, version: String) = Context(
        locale = AppLocale.EN_US,
        platform = Platform.IOS,
        appVersion = Version.parse(version),
        stableId = StableId.of(userId)
    )

    fun forAndroid(userId: String, version: String) = Context(
        locale = AppLocale.EN_US,
        platform = Platform.ANDROID,
        appVersion = Version.parse(version),
        stableId = StableId.of(userId)
    )

    fun forWeb(sessionId: String, version: String = "1.0.0") = Context(
        locale = AppLocale.EN_US,
        platform = Platform.WEB,
        appVersion = Version.parse(version),
        stableId = StableId.of(sessionId)
    )
}

// Usage
val iosContext = PlatformContext.forIOS("user-123", "2.1.0")
val webContext = PlatformContext.forWeb("session-abc")
```

### Testing with Contexts

```kotlin
class FeatureFlagTest {
    private fun ctx(
        locale: AppLocale = AppLocale.EN_US,
        platform: Platform = Platform.IOS,
        version: String = "1.0.0",
        idHex: String = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    ) = Context(locale, platform, Version.parse(version), StableId.of(idHex))

    @Test
    fun `Given iOS context, When evaluating mobile feature, Then returns true`() {
        val context = ctx(platform = Platform.IOS)
        val result = context.evaluate(Features.MOBILE_FEATURE)
        assertTrue(result)
    }

    @Test
    fun `Given version 2_0_0, When evaluating new feature, Then returns true`() {
        val context = ctx(version = "2.0.0")
        val result = context.evaluate(Features.NEW_FEATURE)
        assertTrue(result)
    }
}
```

---

## Next Steps

- **[Rules](Rules.md)**: Learn how rules use context for targeting
- **[Evaluation](Evaluation.md)**: Understand how context flows through evaluation
- **[Quick Start](QuickStart.md)**: Get started with your first feature flag
- **[Overview](index.md)**: Complete API overview

---

**Related Topics:**
- **Platform Targeting**: See [Rules](Rules.md#platform-targeting)
- **Version Ranges**: See [Rules](Rules.md#version-targeting)
- **Rollout Bucketing**: See [Evaluation](Evaluation.md#rollout-bucketing)
- **Custom Evaluables**: See [Rules](Rules.md#custom-evaluables)
