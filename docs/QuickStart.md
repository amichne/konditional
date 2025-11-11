# Quick Start Guide

Get your first type-safe feature flag running in 5 minutes.

---

## Installation

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.amichne:konditional:1.0.0")
}
```

---

## Your First Type-Safe Flag (2 Minutes)

### Step 1: Define Your Flag

```kotlin
import io.amichne.konditional.core.Conditional
import io.amichne.konditional.context.Context

enum class Features(override val key: String) : Conditional<Boolean, Context> {
    DARK_MODE("dark_mode")
}
```

**What this gives you:**

- IDE auto-complete for `Features.DARK_MODE`
- Compile error if you typo the name
- Type-safe: Always returns `Boolean`, never null

### Step 2: Configure the Flag

```kotlin
import io.amichne.konditional.builders.config

val configuration = config {
    Features.DARK_MODE with {
        default(false)  // Required - no nulls possible
    }
}
```

**What this gives you:**

- Compiler enforces default value
- Type-safe: `default(false)` must be Boolean

### Step 3: Load Configuration

```kotlin
import io.amichne.konditional.registry.FlagRegistry

FlagRegistry.load(configuration)
```

### Step 4: Evaluate the Flag

```kotlin
import io.amichne.konditional.context.basicContext
import io.amichne.konditional.context.evaluate

// Create a context
val context = basicContext(
    platform = Platform.ANDROID,
    stableId = StableId.of("user-123")
)

// Evaluate the flag
val isDarkMode: Boolean = context.evaluate(Features.DARK_MODE)

// Use it
if (isDarkMode) {
    applyDarkTheme()
}
```

**What this gives you:**

- Non-null result guaranteed
- Type-safe: `isDarkMode` is always `Boolean`

---

## Before & After Comparison

### String-Based (Your Current System)

```kotlin
// Definition: Scattered string literals
val enabled = config.getBoolean("dark_mode")

// Problems:
// - Returns Boolean? (nullable)
// - Typo "dakr_mode" compiles
// - No IDE auto-complete
// - ClassCastException if wrong type
```

### Type-Safe (Konditional)

```kotlin
// Definition: Type-safe enum
enum class Features(override val key: String) : Conditional<Boolean, Context> {
    DARK_MODE("dark_mode")
}

val enabled: Boolean = context.evaluate(Features.DARK_MODE)

// Benefits:
// - Returns Boolean (non-null)
// - Typo won't compile
// - Full IDE auto-complete
// - Type mismatch is compile error
```

---

## Adding Platform-Specific Rules (3 Minutes)

```kotlin
config {
    Features.DARK_MODE with {
        default(false)  // Default for all platforms

        // Enable on iOS only
        rule {
            platforms(Platform.IOS)
        }.implies(true)
    }
}

// Evaluation: Automatic platform handling
val context = basicContext(
    platform = Platform.IOS,
    stableId = StableId.of("user-123")
)

val enabled = context.evaluate(Features.DARK_MODE)
// Returns: true (iOS rule matched)
```

**What you get:**

- Platform logic declarative in config

- No if/when statements in application code

- Easy to test by changing context

---

## Adding Gradual Rollout (2 Minutes)

```kotlin
config {
    Features.DARK_MODE with {
        default(false)

        // 25% rollout on Android
        rule {
            platforms(Platform.ANDROID)
            rollout = Rollout.of(25.0)  // 25% of users
        }.implies(true)

        // Full rollout on iOS
        rule {
            platforms(Platform.IOS)
        }.implies(true)
    }
}

// Evaluation: Automatic bucketing via stableId
val context = basicContext(
    platform = Platform.ANDROID,
    stableId = StableId.of("user-123")  // Deterministic bucketing
)

val enabled = context.evaluate(Features.DARK_MODE)
// Returns: true or false based on SHA-256(stableId + flag_key)
```

**What you get:**

- Deterministic: Same user always gets same result
- Independent: Each flag has its own bucket
- Platform-stable: Works across JVM, Android, iOS

---

## Beyond Booleans: String Configuration

```kotlin
// Define string flag
enum class ApiConfig(override val key: String) : Conditional<String, Context> {
    ENDPOINT("api_endpoint")
}

// Configure with environment-specific rules
config {
    ApiConfig.ENDPOINT with {
        default("https://api.prod.example.com")

        rule {
            platforms(Platform.IOS)
        }.implies("https://api-ios.prod.example.com")

        rule {
            platforms(Platform.ANDROID)
        }.implies("https://api-android.prod.example.com")
    }
}

// Usage: Type-safe, non-null
val endpoint: String = context.evaluate(ApiConfig.ENDPOINT)
httpClient.setBaseUrl(endpoint)
```

**What you get:**

- Type-safe: Always returns `String`, never null
- Platform-specific URLs declarative
- No string matching in application code

---

## Complex Types: Data Classes

```kotlin
// Define your type
data class ThemeConfig(
    val primaryColor: String,
    val fontSize: Int,
    val darkMode: Boolean
)

// Define flag for that type
enum class Theme(override val key: String) : Conditional<ThemeConfig, Context> {
    APP_THEME("app_theme")
}

// Configure complete themes
config {
    Theme.APP_THEME with {
        default(
            ThemeConfig(
                primaryColor = "#FFFFFF",
                fontSize = 14,
                darkMode = false
            )
        )

        rule {
            platforms(Platform.IOS)
        }.implies(
            ThemeConfig(
                primaryColor = "#000000",
                fontSize = 16,
                darkMode = true
            )
        )
    }
}

// Usage: Atomic, consistent theme
val theme: ThemeConfig = context.evaluate(Theme.APP_THEME)
applyTheme(theme.primaryColor, theme.fontSize, theme.darkMode)
```

**What you get:**

- Atomic updates: All theme values consistent
- Type-safe: Guaranteed ThemeConfig, never null
- No validation needed: Type enforces structure

---

## Custom Context for Business Logic

```kotlin
// Define context with business domain
data class AppContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    val subscriptionTier: SubscriptionTier  // Custom field
) : Context

enum class SubscriptionTier {
    FREE, PROFESSIONAL, ENTERPRISE
}

// Define flag requiring custom context
enum class PremiumFeatures(override val key: String)
    : Conditional<Boolean, AppContext> {  // ← Requires AppContext
    DATA_EXPORT("export_enabled")
}

// Configure with custom logic
config {
    PremiumFeatures.DATA_EXPORT with {
        default(false)

        // Enable for enterprise tier
        rule {
            extension {
                object : Evaluable<AppContext>() {
                    override fun matches(context: AppContext): Boolean =
                        context.subscriptionTier == SubscriptionTier.ENTERPRISE
                    override fun specificity(): Int = 1
                }
            }
        }.implies(true)
    }
}

// Usage: Type-safe context required
val context = AppContext(
    locale = AppLocale.EN_US,
    platform = Platform.ANDROID,
    appVersion = Version(1, 0, 0),
    stableId = StableId.of("user-123"),
    subscriptionTier = SubscriptionTier.ENTERPRISE
)

val canExport: Boolean = context.evaluate(PremiumFeatures.DATA_EXPORT)
// Returns: true (enterprise tier matched)

// Wrong context type won't compile:
val basicContext: Context = basicContext(...)
basicContext.evaluate(PremiumFeatures.DATA_EXPORT)  // ✗ Compile error
```

**What you get:**

- Business logic declarative in config
- Context requirements explicit in types
- Compiler enforces correct context

---

## Testing: Simple Data Classes

```kotlin
@Test
fun `dark mode enabled on iOS`() {
    // Create test context
    val context = basicContext(
        platform = Platform.IOS,
        stableId = StableId.of("test-user")
    )

    // Evaluate
    val enabled = context.evaluate(Features.DARK_MODE)

    // Assert
    assertTrue(enabled)
}

@Test
fun `dark mode disabled on Android`() {
    val context = basicContext(
        platform = Platform.ANDROID,
        stableId = StableId.of("test-user")
    )

    assertFalse(context.evaluate(Features.DARK_MODE))
}

// Reusable test context factory
fun testContext(
    platform: Platform = Platform.ANDROID,
    tier: SubscriptionTier = SubscriptionTier.FREE
) = AppContext(
    locale = AppLocale.EN_US,
    platform = platform,
    appVersion = Version(1, 0, 0),
    stableId = StableId.of("test-user"),
    subscriptionTier = tier
)

@Test
fun `premium export enabled for enterprise`() {
    val context = testContext(tier = SubscriptionTier.ENTERPRISE)
    assertTrue(context.evaluate(PremiumFeatures.DATA_EXPORT))
}
```

**What you get:**

- No mocking frameworks needed
- Simple data class construction
- Reusable test factories

---

## Common Patterns

### Multiple Flags in One Enum

```kotlin
enum class Features(override val key: String) : Conditional<Boolean, Context> {
    DARK_MODE("dark_mode"),
    NEW_CHECKOUT("new_checkout"),
    ANALYTICS("analytics_enabled")
}
```

### Organizing by Domain

```kotlin
enum class UiFeatures(override val key: String) : Conditional<Boolean, Context> {
    DARK_MODE("dark_mode"),
    NEW_NAVIGATION("new_navigation")
}

enum class ApiConfig(override val key: String) : Conditional<String, Context> {
    ENDPOINT("api_endpoint"),
    TIMEOUT_URL("timeout_url")
}

enum class Limits(override val key: String) : Conditional<Int, Context> {
    MAX_RETRIES("max_retries"),
    BATCH_SIZE("batch_size")
}
```

### Version-Based Rules

```kotlin
config {
    Features.NEW_CHECKOUT with {
        default(false)

        rule {
            versions { min(2, 0, 0) }  // Version 2.0.0+
        }.implies(true)
    }
}
```

### Locale-Based Rules

```kotlin
config {
    Features.METRIC_UNITS with {
        default(false)

        rule {
            locales(AppLocale.EN_GB, AppLocale.FR_FR)  // UK and France
        }.implies(true)
    }
}
```

### Multiple Criteria

```kotlin
config {
    Features.PREMIUM_EXPORT with {
        default(false)

        // All conditions must match
        rule {
            platforms(Platform.IOS)
            versions { min(2, 0, 0) }
            locales(AppLocale.EN_US)
            rollout = Rollout.of(50.0)  // 50% of eligible users
        }.implies(true)
    }
}
```

---

## Rule Specificity (Automatic Ordering)

Rules are evaluated **most-specific first**:

```kotlin
config {
    Features.THEME with {
        default("light")

        // Specificity = 2 (platform + locale) - checked first
        rule {
            platforms(Platform.IOS)
            locales(AppLocale.EN_US)
        }.implies("dark-us-ios")

        // Specificity = 1 (platform only) - checked second
        rule {
            platforms(Platform.IOS)
        }.implies("dark-ios")

        // Specificity = 1 (locale only) - checked third
        rule {
            locales(AppLocale.EN_US)
        }.implies("light-us")
    }
}

// Context: iOS + EN_US
val context = basicContext(
    platform = Platform.IOS,
    locale = AppLocale.EN_US
)

val theme = context.evaluate(Features.THEME)
// Returns: "dark-us-ios" (most specific rule wins)
```

**Specificity scoring:**
- Platform: +1
- Locale: +1
- Version: +1
- Custom extension: +1
- **Total: 0-4**

---

## What Makes This Type-Safe?

| Feature | String-Based | Konditional |
|---------|--------------|-------------|
| **Null returns** | `Boolean?` | `Boolean` (non-null) |
| **Type errors** | Runtime `ClassCastException` | Compile error |
| **Typos** | Silent failure | Compile error |
| **Refactoring** | Manual search-replace | IDE symbol rename |
| **Auto-complete** | None | Full IDE support |
| **Context requirements** | Invisible | Explicit type parameter |
| **Testing** | Mock frameworks | Simple data classes |

---

## Next Steps

**Now you've seen type safety in action. Go deeper:**

1. **[Why Type Safety](./WhyTypeSafety.md)** - Understand the complete value proposition
2. **[Error Prevention](./ErrorPrevention.md)** - See all eliminated error classes
3. **[Migration Guide](./Migration.md)** - Migrate your existing flags
4. **[Context Guide](./Context.md)** - Design custom context types
5. **[Builders Guide](./Builders.md)** - Master the configuration DSL
6. **[Rules Guide](./Rules.md)** - Advanced targeting and rollouts

---

## Key Takeaways

**Define once, use safely everywhere**
**Compiler catches errors, not users**
**IDE provides full tooling support**
**Business logic declarative in config**
**No null checks, no type errors, no typos**

**Core Principle**: If it compiles, it works.
