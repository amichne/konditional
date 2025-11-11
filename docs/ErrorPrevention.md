# Error Prevention Reference

This document catalogs every class of runtime error that Konditional's type system **completely eliminates**. These errors are not just caught early—they're impossible to write.

---

## Eliminated: NullPointerException

### String-Based System

```kotlin
// All getters return nullable types
val enabled: Boolean? = config.getBoolean("dark_mode")
val endpoint: String? = config.getString("api_endpoint")
val timeout: Int? = config.getInt("timeout_ms")

// NPE risk at every usage site
if (enabled) { }  // ⚠️ Compilation warning: nullable in boolean context

// Defensive code everywhere
val safeEnabled = enabled ?: false
val safeEndpoint = endpoint ?: "https://default.com"
val safeTimeout = timeout ?: 5000

// Still possible to forget
fun processConfig() {
    val value = config.getString("endpoint")
    httpClient.setEndpoint(value)  // 💣 NPE if value is null
}
```

### Type-Safe System

```kotlin
// Default value REQUIRED at compile time
enum class Features(override val key: String) : Conditional<Boolean, Context> {
    DARK_MODE("dark_mode")
}

config {
    Features.DARK_MODE with {
        default(false)  // ✓ Compiler enforces this
    }
}

// Evaluation returns non-null
val enabled: Boolean = context.evaluate(Features.DARK_MODE)  // Never null

// No defensive code needed
if (enabled) { }  // ✓ Direct usage

// Impossible to forget
val endpoint: String = context.evaluate(ApiConfig.ENDPOINT)  // Always non-null
httpClient.setEndpoint(endpoint)  // ✓ Safe
```

**Guarantee**: `context.evaluate()` **never returns null**. The compiler enforces default values.

---

## Eliminated: ClassCastException

### String-Based System

```kotlin
// Type is in method name, not type system
val value1 = config.getBoolean("timeout_ms")  // Wrong type getter
val value2 = config.getInt("dark_mode")       // Wrong type getter
val value3 = config.getString("max_retries")  // Wrong type getter

// Compiles fine, crashes at runtime
fun configure() {
    val timeout: Int = config.getBoolean("timeout_ms") as Int  // 💣 ClassCastException
}

// Or silent wrong behavior
val enabled = config.getInt("dark_mode")  // Returns null, not crash
if (enabled == 1) { }  // Silently broken
```

### Type-Safe System

```kotlin
// Type is in generic parameter
enum class Config(override val key: String) : Conditional<Int, Context> {
    TIMEOUT_MS("timeout_ms")  // ← Int type enforced here
}

enum class Features(override val key: String) : Conditional<Boolean, Context> {
    DARK_MODE("dark_mode")  // ← Boolean type enforced here
}

// Type mismatch is compile error
val timeout: Boolean = context.evaluate(Config.TIMEOUT_MS)  // ✗ Type mismatch
val timeout: Int = context.evaluate(Config.TIMEOUT_MS)      // ✓ Correct

// Wrong flag type won't compile
val enabled: Int = context.evaluate(Features.DARK_MODE)  // ✗ Type mismatch
val enabled: Boolean = context.evaluate(Features.DARK_MODE)  // ✓ Correct
```

**Guarantee**: The returned value **always matches** the declared type. Type mismatches are compile errors.

---

## Eliminated: KeyNotFoundException / Silent Failures

### String-Based System

```kotlin
// Typos compile successfully
val enabled = config.getBoolean("dakr_mode")  // ✓ Compiles - "dark_mode" typo
val endpoint = config.getString("api_edpoint")  // ✓ Compiles - "endpoint" typo
val timeout = config.getInt("timout_ms")  // ✓ Compiles - "timeout" typo

// Returns null, uses wrong default
val enabled = config.getBoolean("dakr_mode") ?: false  // Silent failure
// User intended dark mode, gets light mode

// No IDE help
config.getBoolean("???")  // What flags exist?
```

### Type-Safe System

```kotlin
// Typos are compile errors
val enabled = context.evaluate(Features.DAKR_MODE)  // ✗ Unresolved reference

// IDE shows all available flags
context.evaluate(Features.  // Auto-complete: DARK_MODE, NEW_CHECKOUT, ...

// Refactoring updates all usages
enum class Features(override val key: String) : Conditional<Boolean, Context> {
    DARK_MODE("dark_mode")  // ← Rename this symbol
}

// IDE rename refactoring updates every usage site automatically
```

**Guarantee**: Flag names **must exist** as enum members, or code won't compile.

---

## Eliminated: Wrong Context Type

### String-Based System

```kotlin
// Context requirements invisible
fun canExport(config: ConfigService): Boolean {
    return config.getBoolean("premium_export") ?: false
    // ⚠️ How does it know user tier? Runtime surprise!
}

// Runtime error when wrong context passed
val basicContext = mapOf("platform" to "ios")
val exportEnabled = evaluator.evaluate("premium_export", basicContext)  // 💣 Missing "tier"
```

### Type-Safe System

```kotlin
// Context requirements explicit in type parameter
enum class PremiumFeatures(override val key: String)
    : Conditional<Boolean, AppContext> {  // ← Requires AppContext
    PREMIUM_EXPORT("premium_export")
}

data class AppContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    val subscriptionTier: SubscriptionTier  // ← Required field
) : Context

// Wrong context type won't compile
val basicContext: Context = // ...
val enabled = basicContext.evaluate(PremiumFeatures.PREMIUM_EXPORT)  // ✗ Type mismatch

// Correct context type required
val appContext: AppContext = // ...
val enabled = appContext.evaluate(PremiumFeatures.PREMIUM_EXPORT)  // ✓ Compiles
```

**Guarantee**: Evaluation **requires compatible context type**, enforced at compile time.

---

## Eliminated: Configuration Inconsistency

### String-Based System

```kotlin
// Related config split across multiple keys
class ThemeManager(private val config: ConfigService) {
    fun getTheme(): Theme {
        val primaryColor = config.getString("theme_primary") ?: "#FFF"
        val secondaryColor = config.getString("theme_secondary") ?: "#000"
        val fontSize = config.getInt("theme_font_size") ?: 14
        val darkMode = config.getBoolean("theme_dark_mode") ?: false

        return Theme(primaryColor, secondaryColor, fontSize, darkMode)
    }
}

// Problem: Inconsistent partial updates
// Server sends:
// { "theme_primary": "#000", "theme_dark_mode": true }
// But forgets "theme_secondary": "#FFF"
//
// Result: Dark mode with light secondary color - visually broken
```

### Type-Safe System

```kotlin
// Single atomic type for related config
data class ThemeConfig(
    val primaryColor: String,
    val secondaryColor: String,
    val fontSize: Int,
    val darkMode: Boolean
)

enum class Theme(override val key: String) : Conditional<ThemeConfig, Context> {
    APP_THEME("app_theme")
}

config {
    Theme.APP_THEME with {
        default(
            ThemeConfig("#FFFFFF", "#000000", 14, false)
        )

        rule {
            platforms(Platform.IOS)
        }.implies(
            ThemeConfig("#000000", "#FFFFFF", 16, true)  // All values together
        )
    }
}

// Evaluation returns complete, consistent theme
val theme: ThemeConfig = context.evaluate(Theme.APP_THEME)
// ✓ primaryColor, secondaryColor, fontSize, darkMode always consistent
```

**Guarantee**: Related configuration values are **updated atomically** as a single unit.

---

## Eliminated: Validation Duplication

### String-Based System

```kotlin
// Validation scattered across codebase
class PaymentService(private val config: ConfigService) {
    fun getMaxRetries(): Int {
        val value = config.getInt("max_retries") ?: 3
        require(value in 1..10) { "max_retries must be 1-10" }
        return value
    }
}

class NetworkService(private val config: ConfigService) {
    fun getMaxRetries(): Int {
        val value = config.getInt("max_retries") ?: 3
        // ⚠️ Forgot validation here - inconsistent!
        return value
    }
}

class BackgroundWorker(private val config: ConfigService) {
    fun getMaxRetries(): Int {
        val value = config.getInt("max_retries") ?: 3
        require(value >= 1) { "max_retries must be positive" }
        // ⚠️ Different validation rules!
        return value
    }
}
```

### Type-Safe System

```kotlin
// Validation once at configuration time
@JvmInline
value class Retries(val value: Int) {
    init {
        require(value in 1..10) { "Retries must be 1-10" }
    }
}

enum class Config(override val key: String) : Conditional<Retries, Context> {
    MAX_RETRIES("max_retries")
}

config {
    Config.MAX_RETRIES with {
        default(Retries(3))  // ✓ Validated at construction

        rule {
            platforms(Platform.IOS)
        }.implies(Retries(5))  // ✓ Validated at construction
    }
}

// Usage: No validation needed
val retries: Retries = context.evaluate(Config.MAX_RETRIES)
// ✓ Guaranteed valid by type system
```

**Guarantee**: Invalid values are **unrepresentable** in the type system. Parse once, use everywhere.

---

## Eliminated: Rollout Bucketing Bugs

### String-Based System

```kotlin
// Manual bucketing logic
class RolloutManager(private val config: ConfigService) {
    fun isEnabled(flagKey: String, userId: String): Boolean {
        val baseEnabled = config.getBoolean(flagKey) ?: false
        if (!baseEnabled) return false

        val percentage = config.getInt("${flagKey}_rollout_pct") ?: 0

        // Bug: hashCode() differs across platforms
        val hash = userId.hashCode()
        val bucket = (hash % 100).absoluteValue
        return bucket < percentage
    }
}

// Problems:
// 1. hashCode() not stable across JVM restarts
// 2. Different bucketing per flag leads to correlation
// 3. Easy to forget absoluteValue - negative buckets
// 4. Percentage stored separately from flag
```

### Type-Safe System

```kotlin
enum class Features(override val key: String) : Conditional<Boolean, Context> {
    NEW_CHECKOUT("new_checkout")
}

config {
    Features.NEW_CHECKOUT with {
        default(false)
        rule {
            rollout = Rollout.of(25.0)  // 25% rollout
        }.implies(true)
    }
}

// Usage: Automatic, deterministic bucketing
val enabled = context.evaluate(Features.NEW_CHECKOUT)

// Bucketing properties:
// ✓ SHA-256 based (deterministic, platform-independent)
// ✓ Independent buckets per flag (no correlation)
// ✓ Stable across sessions via context.stableId
// ✓ Percentage stored with flag definition
```

**Guarantee**: Rollouts use **cryptographic hashing** for deterministic, independent bucketing.

---

## Eliminated: Refactoring Breaks

### String-Based System

```kotlin
// Flag name used in 50 places
class FeatureA { config.getBoolean("dark_mode") }
class FeatureB { config.getBoolean("dark_mode") }
// ... 48 more usages

// Developer renames flag
// Changes backend config: "dark_mode" → "darkMode"

// Update code:
class FeatureA { config.getBoolean("darkMode") }  // ✓ Updated
class FeatureB { config.getBoolean("dark_mode") }  // ✗ Forgot to update!
// ... 48 more to manually update

// Runtime failures in production
```

### Type-Safe System

```kotlin
// Flag name as enum member
enum class Features(override val key: String) : Conditional<Boolean, Context> {
    DARK_MODE("dark_mode")
}

// Used in 50 places
class FeatureA { context.evaluate(Features.DARK_MODE) }
class FeatureB { context.evaluate(Features.DARK_MODE) }
// ... 48 more usages

// Developer renames: Right-click → Rename Symbol
enum class Features(override val key: String) : Conditional<Boolean, Context> {
    DARK_MODE("darkMode")  // IDE updates all 50 usages automatically
}

// ✓ All usages updated atomically
// ✓ Compile error if any usage missed
```

**Guarantee**: IDE refactoring **updates all usages** atomically. Missed updates = compile errors.

---

## Eliminated: Testing Complexity

### String-Based System

```kotlin
// Test requires mocking config service
@Test
fun `test premium feature enabled`() {
    val mockConfig = mock<ConfigService>()

    // Mock setup for every related flag
    whenever(mockConfig.getBoolean("premium_export")).thenReturn(true)
    whenever(mockConfig.getBoolean("premium_analytics")).thenReturn(true)
    whenever(mockConfig.getString("premium_tier")).thenReturn("enterprise")

    val manager = FeatureManager(mockConfig)
    assertTrue(manager.canExport())

    // Verify mock interactions
    verify(mockConfig).getBoolean("premium_export")
}

// Problems:
// - Mocking framework required
// - String literals duplicated
// - Setup boilerplate
// - Mock verification noise
```

### Type-Safe System

```kotlin
// Test uses simple data class
@Test
fun `test premium feature enabled`() {
    val context = AppContext(
        locale = AppLocale.EN_US,
        platform = Platform.IOS,
        appVersion = Version(1, 0, 0),
        stableId = StableId.of("test-user"),
        subscriptionTier = SubscriptionTier.ENTERPRISE
    )

    val enabled = context.evaluate(PremiumFeatures.DATA_EXPORT)

    assertTrue(enabled)
}

// Reusable test factory
fun testContext(
    tier: SubscriptionTier = SubscriptionTier.FREE
) = AppContext(
    locale = AppLocale.EN_US,
    platform = Platform.ANDROID,
    appVersion = Version(1, 0, 0),
    stableId = StableId.of("test-user"),
    subscriptionTier = tier
)

@Test
fun `test enterprise features`() {
    val context = testContext(tier = SubscriptionTier.ENTERPRISE)
    assertTrue(context.evaluate(PremiumFeatures.DATA_EXPORT))
}
```

**Guarantee**: Tests use **simple data classes**, no mocking frameworks needed.

---

## Eliminated: JSON Parsing Errors

### String-Based System

```kotlin
// Manual JSON parsing
val json = """{ "dark_mode": "true" }"""  // ⚠️ String instead of boolean

val config = mutableMapOf<String, Any>()
val jsonObj = JSONObject(json)
jsonObj.keys().forEach { key ->
    config[key] = jsonObj.get(key)
}

// Runtime crash
val enabled = config["dark_mode"] as Boolean  // 💣 ClassCastException: String cannot be cast to Boolean

// Or silent wrong behavior
val enabled = config["dark_mode"] as? Boolean ?: false  // Returns false for "true"
```

### Type-Safe System

```kotlin
// Type-safe deserialization with error handling
val json = """
{
  "flags": [
    {
      "key": "dark_mode",
      "value_type": "boolean",
      "default": "true"
    }
  ]
}
"""

when (val result = SnapshotSerializer.default.deserialize(json)) {
    is ParseResult.Success -> {
        FlagRegistry.load(result.value)
        // ✓ Type-safe configuration loaded
    }
    is ParseResult.Failure -> {
        logger.error("Parse error: ${result.error}")
        // ⚠️ Error caught before applying bad config
    }
}
```

**Guarantee**: Invalid JSON is **rejected before application**, not at evaluation time.

---

## Eliminated: Hidden Dependencies

### String-Based System

```kotlin
// What context does this flag need?
fun canAccessPremiumFeature(userId: String): Boolean {
    return config.getBoolean("premium_feature") ?: false
    // ⚠️ How does backend know user tier?
    // ⚠️ What other context is needed?
    // ⚠️ Documentation only source of truth
}

// Runtime surprise
val canAccess = canAccessPremiumFeature("user-123")
// 💣 Backend evaluation fails: Missing required field "subscription_tier"
```

### Type-Safe System

```kotlin
// Context requirements explicit in types
data class AppContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    val subscriptionTier: SubscriptionTier  // ← Visible requirement
) : Context

enum class PremiumFeatures(override val key: String)
    : Conditional<Boolean, AppContext> {  // ← Type documents requirement
    ADVANCED_ANALYTICS("premium_feature")
}

// Compiler enforces requirements
fun canAccessPremiumFeature(context: AppContext): Boolean {
    return context.evaluate(PremiumFeatures.ADVANCED_ANALYTICS)
    // ✓ All required context fields present
}

// IDE shows type signature
val context: AppContext = buildAppContext(...)  // ✓ Explicit construction
val canAccess = canAccessPremiumFeature(context)
```

**Guarantee**: Context requirements are **self-documenting** via type system.

---

## Summary: Error Classes Eliminated

| Error Class | String-Based Risk | Type-Safe Guarantee |
|-------------|-------------------|---------------------|
| **NullPointerException** | Nullable returns, forgotten null checks | Non-null returns, compiler-enforced defaults |
| **ClassCastException** | Wrong type getter used | Generic type parameter enforced |
| **KeyNotFoundException** | Typos in flag names | Enum member or compile error |
| **Wrong Context** | Context requirements invisible | Context type parameter enforced |
| **Inconsistent Config** | Partial updates of related values | Atomic updates of compound types |
| **Validation Errors** | Validation scattered/duplicated | Type invariants enforced once |
| **Rollout Bugs** | Manual bucketing, platform differences | SHA-256 deterministic bucketing |
| **Refactoring Breaks** | String search-replace, manual updates | IDE symbol refactoring |
| **Test Complexity** | Mock framework required | Simple data class construction |
| **Parse Errors** | Runtime crashes on bad JSON | Parse errors before application |
| **Hidden Dependencies** | Documentation only | Type signatures self-document |

---

## What You Can Now Guarantee

With Konditional, you can make these statements with **100% confidence**:

1.  **"This flag evaluation will never return null"**
   - Default value required at compile time

2.  **"This flag's type matches my variable"**
   - Generic type parameter enforced

3.  **"This flag name exists in our system"**
   - Enum member or code won't compile

4.  **"This evaluation has the correct context"**
   - Context type parameter enforced

5.  **"Refactoring updated all usages"**
   - IDE symbol rename guarantees it

6.  **"Related config values are consistent"**
   - Atomic compound types

7.  **"Rollout bucketing is deterministic"**
   - SHA-256 based with stable IDs

8.  **"Invalid config won't reach production"**
   - Parse errors caught before application

9.  **"Tests don't need mocking frameworks"**
   - Simple context data classes

10.  **"Context requirements are documented"**
    - Type signatures are self-documenting

---

## The Core Principle

> **"Parse, Don't Validate"**
>
> Invalid states are **unrepresentable** in the type system.
>
> If it compiles, it works.

This isn't just "fewer errors" — entire error classes **cannot exist** in type-safe code.

---

## Next Steps

- **[Migration Guide](./Migration.md)** - Eliminate these errors from your codebase
- **[Why Type Safety](./WhyTypeSafety.md)** - Understand the value proposition
- **[Quick Start](./QuickStart.md)** - See type safety in action

**The Result**: Ship faster with confidence. Let the compiler catch errors, not your users.
