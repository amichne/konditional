# Error Prevention Reference

## Why Konditional?

Traditional feature flag systems use string-based APIs that compile successfully but fail at runtime. Konditional uses Kotlin's type system to make entire classes of errors **impossible to write**. These aren't bugs caught earlier—they literally cannot exist in your code.

**The value**: Ship features faster with confidence. Move error detection from production monitoring to compile time. Let the compiler catch bugs, not your users.

---

## Error Classes Eliminated

Konditional's type system completely prevents these 11 categories of runtime errors:

| Error Type | What Goes Wrong | How Konditional Prevents It |
|------------|----------------|----------------------------|
| **NullPointerException** | Feature flag getters return nullable types; forgotten null checks crash at runtime | `evaluate()` returns non-null values; compiler enforces default values at configuration time |
| **ClassCastException** | Wrong type getter used (e.g., `getBoolean()` on an integer flag) causes runtime cast failures | Generic type parameters ensure returned value always matches declared type; mismatches are compile errors |
| **KeyNotFoundException** | Typos in string-based flag names compile successfully but return null/crash at runtime | Flag names are enum members; typos become "unresolved reference" compile errors |
| **Wrong Context Type** | Context requirements are invisible; missing required fields discovered at runtime | Context type declared in generic parameter; compiler enforces compatible context types |
| **Configuration Inconsistency** | Related config values updated separately; partial updates create invalid states (e.g., dark mode with light colors) | Compound types updated atomically; impossible to have inconsistent related values |
| **Validation Duplication** | Validation logic scattered across codebase; different services validate same config differently or forget validation | Value types enforce invariants at construction; parse once, use everywhere safely |
| **Rollout Bucketing Bugs** | Manual bucketing uses platform-dependent hashing; inconsistent bucketing across restarts or platforms | SHA-256 based deterministic bucketing; stable, platform-independent, per-flag independence |
| **Refactoring Breaks** | Renaming flags requires manual string search-replace across codebase; easy to miss usages | IDE refactoring renames all usages atomically; missed updates become compile errors |
| **Testing Complexity** | Tests require mock frameworks with verbose setup; string literals duplicated; brittle mock verification | Simple data class construction; no mocking needed; type-safe test contexts |
| **JSON Parsing Errors** | Invalid JSON types silently create wrong behavior or crash during evaluation | Parse errors caught before applying configuration; type-safe deserialization with explicit error handling |
| **Hidden Dependencies** | Context requirements exist only in documentation; missing fields discovered at evaluation time | Context requirements self-documented in type signatures; compiler enforces all required fields present |

---

## Representative Examples

### Example 1: NullPointerException Eliminated

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

### Example 2: ClassCastException Eliminated

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

### Example 3: Configuration Inconsistency Eliminated

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
