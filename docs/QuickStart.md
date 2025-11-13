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
import io.amichne.konditional.core.FeatureModule

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

| String-Based | Type-Safe (Konditional) |
|--------------|-------------------------|
| `config.getBoolean("dark_mode")` returns `Boolean?` | `context.evaluate(Features.DARK_MODE)` returns `Boolean` |
| Typos compile silently | Typos are compile errors |
| No IDE auto-complete | Full IDE support |
| Runtime ClassCastException | Compile-time type checking |

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

## Common Patterns

### Organizing by Domain

Organize flags by type and domain for clarity:

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

### Combining Multiple Criteria

Rules support platform, version, locale, and rollout conditions:

```kotlin
config {
    Features.PREMIUM_EXPORT with {
        default(false)

        rule {
            platforms(Platform.IOS)
            versions { min(2, 0, 0) }  // Version 2.0.0+
            locales(AppLocale.EN_US)
            rollout = Rollout.of(50.0)  // 50% of eligible users
        }.implies(true)
    }
}
```

**Rule Specificity:** Rules are automatically evaluated most-specific first (platform=1, locale=1, version=1, extension=1, max=4).

---

## What Makes This Type-Safe?

| Feature                  | String-Based                 | Konditional             |
|--------------------------|------------------------------|-------------------------|
| **Null returns**         | `Boolean?`                   | `Boolean` (non-null)    |
| **Type errors**          | Runtime `ClassCastException` | Compile error           |
| **Typos**                | Silent failure               | Compile error           |
| **Refactoring**          | Manual search-replace        | IDE symbol rename       |
| **Auto-complete**        | None                         | Full IDE support        |
| **Context requirements** | Invisible                    | Explicit type parameter |
| **Testing**              | Mock frameworks              | Simple data classes     |

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
