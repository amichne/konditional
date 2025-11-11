# Why Type Safety Matters

## The Problem: String-Based Configuration

If your team currently uses string-based configuration or feature flags, you're likely experiencing these runtime errors regularly:

```kotlin
// Common string-based approach
val enabled = config.getBoolean("dark_mode")
val endpoint = config.getString("api_endpoint")
val timeout = config.getInt("timeout_ms")
```

### The Hidden Costs

Every string lookup carries **six critical failure modes** that your team deals with daily:

| Error Type | Example | Impact |
|------------|---------|--------|
| **Typos** | `"dakr_mode"` instead of `"dark_mode"` | Silent failures, wrong defaults used |
| **Type Mismatches** | `getBoolean("timeout_ms")` when it's an Int | Runtime crashes or ClassCastException |
| **Null Handling** | `getString("endpoint") ?: "default"` everywhere | Defensive code bloat, missed edge cases |
| **Refactoring Breaks** | Rename `"api_url"` → `"api_endpoint"` | Silent failures across codebase |
| **No Auto-Complete** | No IDE assistance for flag names | Slow development, copy-paste errors |
| **Invisible Dependencies** | What context does `"premium_feature"` need? | Runtime errors when context is wrong |

---

## The Solution: Compile-Time Guarantees

Konditional **eliminates all six failure modes** by making errors impossible to represent in the type system.

```kotlin
//  Type-safe approach
enum class Features(override val key: String) : Conditional<Boolean, Context> {
    DARK_MODE("dark_mode")
}

val enabled: Boolean = context.evaluate(Features.DARK_MODE)
```

### What You Get

#### 1. **Typos Become Compile Errors**

```kotlin
// String-based: Compiles, fails at runtime
if (config.getBoolean("dakr_mode")) { }

//  Type-safe: Won't compile
context.evaluate(Features.DAKR_MODE)  // Compile error: Unresolved reference
```

#### 2. **Type Mismatches Are Impossible**

```kotlin
// String-based: Runtime ClassCastException
val timeout: Int = config.getBoolean("timeout_ms")  // Compiles! 💣

//  Type-safe: Compiler prevents this
enum class Config(override val key: String) : Conditional<Int, Context> {
    TIMEOUT_MS("timeout_ms")
}

val timeout: Boolean = context.evaluate(Config.TIMEOUT_MS)  // ✗ Type mismatch
val timeout: Int = context.evaluate(Config.TIMEOUT_MS)      // ✓ Correct
```

#### 3. **No More Null Checks**

```kotlin
// String-based: Nullability everywhere
val endpoint = config.getString("api_endpoint") ?: "https://default.com"
val timeout = config.getInt("timeout_ms") ?: 5000

//  Type-safe: Defaults enforced at definition
enum class ApiConfig(override val key: String) : Conditional<String, Context> {
    ENDPOINT("api_endpoint")
}

config {
    ApiConfig.ENDPOINT with {
        default("https://default.com")  // Required at compile time
    }
}

// Guaranteed non-null
val endpoint: String = context.evaluate(ApiConfig.ENDPOINT)
```

#### 4. **Refactoring Is Safe**

```kotlin
// String-based: Rename breaks at runtime
config.getBoolean("dark_mode")  // Old name
config.getBoolean("darkMode")   // New name - silent failure

//  Type-safe: IDE refactoring updates all references
enum class Features(override val key: String) : Conditional<Boolean, Context> {
    DARK_MODE("dark_mode")  // Rename symbol → all usages update
}
```

#### 5. **IDE Auto-Complete**

```kotlin
// String-based: No IDE help
config.getBoolean("???")  // What flags exist?

//  Type-safe: Full IDE support
context.evaluate(Features.  // IDE shows: DARK_MODE, NEW_CHECKOUT, PREMIUM_EXPORT
```

#### 6. **Context Requirements Are Explicit**

```kotlin
// String-based: Runtime error if wrong context
val enabled = config.getBoolean("enterprise_analytics")  // What context needed?

//  Type-safe: Compiler enforces context type
data class EnterpriseContext(
    val subscriptionTier: Tier,
    // ... other enterprise fields
) : Context

enum class EnterpriseFeatures(override val key: String)
    : Conditional<Boolean, EnterpriseContext> {  // ← Type parameter
    ADVANCED_ANALYTICS("enterprise_analytics")
}

val basicContext: Context = // ...
val enterpriseContext: EnterpriseContext = // ...

// Compile error: Type mismatch
basicContext.evaluate(EnterpriseFeatures.ADVANCED_ANALYTICS)  // ✗

// Compiles: Context type matches
enterpriseContext.evaluate(EnterpriseFeatures.ADVANCED_ANALYTICS)  // ✓
```

---

## What Errors Are Completely Eliminated?

### Eliminated: Runtime Errors

| Error Class | String-Based | Type-Safe |
|-------------|--------------|-----------|
| **NullPointerException** | Missing flag returns `null` | Default value required at compile time |
| **ClassCastException** | Wrong type getter used | Generic type parameter enforced |
| **KeyNotFoundException** | Typo in flag name | Enum reference or compile error |
| **Wrong Context Errors** | Flag needs enterprise context, basic context passed | Context type parameter enforced |
| **Silent Wrong Defaults** | Typo returns `null`, default used silently | No such code compiles |

###  New Guarantees You Can Make

| Statement | Confidence |
|-----------|------------|
| **"This flag always returns a non-null value"** | 100% - Compiler enforces default |
| **"This flag's type matches the variable"** | 100% - Generic type parameter checked |
| **"This flag name exists"** | 100% - Enum member or compile error |
| **"This evaluation has the right context"** | 100% - Type parameter enforced |
| **"Refactoring updated all usages"** | 100% - IDE symbol rename |

---

## Support for Your Existing Types

Konditional isn't just for booleans. It supports:

### Primitive Types
```kotlin
enum class Flags : Conditional<Boolean, Context> { ENABLED }
enum class ApiConfig : Conditional<String, Context> { ENDPOINT }
enum class Limits : Conditional<Int, Context> { MAX_RETRIES }
enum class Thresholds : Conditional<Double, Context> { TIMEOUT_SECONDS }
```

### Complex Data Classes
```kotlin
data class ThemeConfig(
    val primaryColor: String,
    val fontSize: Int,
    val darkMode: Boolean
)

enum class Theme(override val key: String) : Conditional<ThemeConfig, Context> {
    APP_THEME("app_theme")
}

// Type-safe configuration
config {
    Theme.APP_THEME with {
        default(ThemeConfig("#FFFFFF", 14, false))

        rule {
            platforms(Platform.IOS)
        }.implies(ThemeConfig("#1E1E1E", 16, true))
    }
}
```

### Custom Domain Types
```kotlin
@JvmInline
value class UserId(val value: String)

enum class UserConfig(override val key: String)
    : Conditional<UserId, Context> {
    ADMIN_ID("admin_user_id")
}
```

---

## The "Parse, Don't Validate" Principle

Konditional follows the **"Parse, Don't Validate"** pattern:

> **Invalid states are unrepresentable in the type system.**

```kotlin
// String-based: Validation everywhere
fun processConfig() {
    val value = config.getString("threshold")
    if (value == null) throw ConfigException("Missing threshold")

    val parsed = value.toDoubleOrNull()
    if (parsed == null) throw ConfigException("Invalid threshold")

    if (parsed < 0 || parsed > 100) throw ConfigException("Out of range")

    // Finally use it
    applyThreshold(parsed)
}

//  Type-safe: Invalid states don't compile
enum class Config(override val key: String) : Conditional<Double, Context> {
    THRESHOLD("threshold")
}

config {
    Config.THRESHOLD with {
        default(50.0)  // Guaranteed at compile time
    }
}

fun processConfig() {
    val value: Double = context.evaluate(Config.THRESHOLD)
    applyThreshold(value)  // No validation needed!
}
```

---

## Real-World Impact

### Before: String-Based System

```kotlin
class FeatureManager(private val config: ConfigService) {
    fun isDarkModeEnabled(): Boolean {
        // Risk: Typo, null handling, wrong type
        return config.getBoolean("dark_mode") ?: false
    }

    fun getApiEndpoint(platform: String): String {
        // Risk: String matching, multiple lookups, nulls
        return when (platform) {
            "ios" -> config.getString("ios_api_endpoint") ?: DEFAULT_IOS
            "android" -> config.getString("android_api_endpoint") ?: DEFAULT_ANDROID
            else -> config.getString("api_endpoint") ?: DEFAULT_GLOBAL
        }
    }

    fun canExportData(userId: String, tier: String): Boolean {
        // Risk: Business logic mixed with config, unclear requirements
        val baseEnabled = config.getBoolean("export_enabled") ?: false
        val premiumOnly = config.getBoolean("export_premium_only") ?: true

        return baseEnabled && (!premiumOnly || tier == "premium")
    }
}
```

**Issues:**
- 6 different flags that could have typos
- 6 null checks required
- Business logic scattered across config lookups
- No IDE help for flag names
- Context requirements unclear
- Testing requires mocking entire config service

### After: Type-Safe System

```kotlin
enum class Features(override val key: String) : Conditional<Boolean, Context> {
    DARK_MODE("dark_mode")
}

enum class ApiConfig(override val key: String) : Conditional<String, Context> {
    ENDPOINT("api_endpoint")
}

data class EnterpriseContext(
    val subscriptionTier: Tier,
    // ... base context fields
) : Context

enum class EnterpriseFeatures(override val key: String)
    : Conditional<Boolean, EnterpriseContext> {
    DATA_EXPORT("export_enabled")
}

class FeatureManager(private val context: Context) {
    fun isDarkModeEnabled(): Boolean =
        context.evaluate(Features.DARK_MODE)  // ✓ Type-safe, non-null

    fun getApiEndpoint(): String =
        context.evaluate(ApiConfig.ENDPOINT)  // ✓ Platform handled in rules
}

class EnterpriseFeatureManager(private val context: EnterpriseContext) {
    fun canExportData(): Boolean =
        context.evaluate(EnterpriseFeatures.DATA_EXPORT)  // ✓ Business logic in rules
}

// Configuration defined separately
config {
    Features.DARK_MODE with {
        default(false)
        rule {
            platforms(Platform.IOS)
        }.implies(true)
    }

    ApiConfig.ENDPOINT with {
        default("https://api.prod.example.com")
        rule {
            platforms(Platform.IOS)
        }.implies("https://api-ios.prod.example.com")
        rule {
            platforms(Platform.ANDROID)
        }.implies("https://api-android.prod.example.com")
    }

    EnterpriseFeatures.DATA_EXPORT with {
        default(false)
        rule {
            extension {
                object : Evaluable<EnterpriseContext>() {
                    override fun matches(context: EnterpriseContext): Boolean =
                        context.subscriptionTier == Tier.PREMIUM
                    override fun specificity(): Int = 1
                }
            }
        }.implies(true)
    }
}
```

**Benefits:**
-  Zero null checks
-  Zero type errors possible
-  Full IDE auto-complete
-  Business logic declarative in rules
-  Context requirements explicit in types
-  Testing uses simple context objects

---

## Next Steps

Ready to eliminate runtime config errors from your codebase?

1. **[Migration Guide](./Migration.md)** - Step-by-step migration from string-based config
2. **[Quick Start](./QuickStart.md)** - Get running in 5 minutes
3. **[Error Prevention Reference](./ErrorPrevention.md)** - Complete catalog of eliminated errors

**Key Principle**: If it compiles, it works. No runtime configuration errors.
