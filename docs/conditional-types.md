# Conditional Value Types

Konditional supports any non-nullable type as a flag value. This document explores the different types you can use and when each is appropriate.

## The Type Parameter

`Conditional<S : Any, C : Context>` uses `S` to represent the value type:

- `S : Any` means any non-nullable type
- Primitives, collections, data classes, enums - all supported
- Type safety is maintained end-to-end

## Boolean Flags

The most common type - simple on/off switches.

```kotlin
enum class Features(override val key: String) : Conditional<Boolean, Context> {
    DARK_MODE("dark_mode"),
    NEW_CHECKOUT("new_checkout"),
    ANALYTICS_ENABLED("analytics_enabled"),
    ;

    override fun with(build: FlagBuilder<Boolean, Context>.() -> Unit) =
        update(FlagBuilder(this).apply(build).build())
}

config {
    Features.DARK_MODE with {
        default(false)
        rule {
            platforms(Platform.IOS, Platform.ANDROID)
        } implies true
    }
}

// Usage
if (context.evaluate(Features.DARK_MODE)) {
    applyDarkTheme()
}
```

**When to use**: Simple enable/disable decisions.

## String Flags

Useful for configuration values, endpoints, and variants.

```kotlin
enum class Config(override val key: String) : Conditional<String, Context> {
    API_ENDPOINT("api_endpoint"),
    THEME_NAME("theme_name"),
    LOG_LEVEL("log_level"),
    ;

    override fun with(build: FlagBuilder<String, Context>.() -> Unit) =
        update(FlagBuilder(this).apply(build).build())
}

config {
    Config.API_ENDPOINT with {
        default("https://api.prod.example.com")
        rule {
            platforms(Platform.WEB)
        } implies "https://api.staging.example.com"
    }

    Config.THEME_NAME with {
        default("light")
        rule {
            locales(AppLocale.EN_US)
        } implies "dark"
        rule {
            locales(AppLocale.HI_IN)
        } implies "vibrant"
    }
}

// Usage
val endpoint = context.evaluate(Config.API_ENDPOINT)
apiClient.configure(baseUrl = endpoint)
```

**When to use**:
- Configuration values that vary by environment
- A/B test variants
- URLs, keys, or other string configuration

## Integer Flags

For numeric configuration like timeouts, limits, or sizes.

```kotlin
enum class Limits(override val key: String) : Conditional<Int, Context> {
    MAX_CONNECTIONS("max_connections"),
    TIMEOUT_SECONDS("timeout_seconds"),
    CACHE_SIZE_MB("cache_size_mb"),
    ;

    override fun with(build: FlagBuilder<Int, Context>.() -> Unit) =
        update(FlagBuilder(this).apply(build).build())
}

config {
    Limits.MAX_CONNECTIONS with {
        default(10)
        rule {
            platforms(Platform.WEB)
        } implies 50
        rule {
            platforms(Platform.WEB)
            versions {
                min(3, 0)
            }
        } implies 100
    }

    Limits.TIMEOUT_SECONDS with {
        default(30)
        rule {
            platforms(Platform.ANDROID)
            versions {
                max(5, 0, 0)  // Legacy devices
            }
        } implies 60  // More generous timeout
    }
}

// Usage
val maxConnections = context.evaluate(Limits.MAX_CONNECTIONS)
connectionPool.setMaxSize(maxConnections)
```

**When to use**:
- Numeric limits or thresholds
- Timeouts and intervals
- Buffer sizes and limits
- Retry counts

## Enum Flags

Type-safe multi-variant flags.

```kotlin
enum class LogLevel {
    DEBUG, INFO, WARN, ERROR, NONE
}

enum class LogConfig(override val key: String) : Conditional<LogLevel, Context> {
    APP_LOG_LEVEL("app_log_level"),
    NETWORK_LOG_LEVEL("network_log_level"),
    ;

    override fun with(build: FlagBuilder<LogLevel, Context>.() -> Unit) =
        update(FlagBuilder(this).apply(build).build())
}

config {
    LogConfig.APP_LOG_LEVEL with {
        default(LogLevel.INFO)
        rule {
            platforms(Platform.WEB)
        } implies LogLevel.DEBUG
        rule {
            versions {
                max(1, 0)  // Old versions
            }
        } implies LogLevel.ERROR
    }
}

// Usage
val logLevel = context.evaluate(LogConfig.APP_LOG_LEVEL)
logger.setLevel(logLevel)
```

**When to use**:
- Multiple exclusive options (log levels, modes, strategies)
- Type-safe variants in A/B tests
- Enums provide exhaustive when expressions

## Data Class Flags

Complex configuration objects.

```kotlin
data class ApiConfig(
    val baseUrl: String,
    val timeout: Int,
    val retries: Int,
    val useHttps: Boolean,
)

enum class NetworkConfig(override val key: String) : Conditional<ApiConfig, Context> {
    PRIMARY_API("primary_api"),
    BACKUP_API("backup_api"),
    ;

    override fun with(build: FlagBuilder<ApiConfig, Context>.() -> Unit) =
        update(FlagBuilder(this).apply(build).build())
}

config {
    NetworkConfig.PRIMARY_API with {
        default(ApiConfig(
            baseUrl = "https://api.prod.example.com",
            timeout = 30,
            retries = 3,
            useHttps = true,
        ))
        rule {
            platforms(Platform.WEB)
        } implies ApiConfig(
            baseUrl = "http://api.dev.example.com",
            timeout = 60,
            retries = 1,
            useHttps = false,
        )
    }
}

// Usage
val apiConfig = context.evaluate(NetworkConfig.PRIMARY_API)
apiClient.configure(
    baseUrl = apiConfig.baseUrl,
    timeout = apiConfig.timeout,
    retries = apiConfig.retries,
)
```

**When to use**:
- Related configuration values that should change together
- Complex objects with multiple fields
- Configuration that varies as a unit

## List Flags

Collections of values.

```kotlin
enum class ModuleConfig(override val key: String) : Conditional<List<String>, Context> {
    ENABLED_FEATURES("enabled_features"),
    BETA_MODULES("beta_modules"),
    ;

    override fun with(build: FlagBuilder<List<String>, Context>.() -> Unit) =
        update(FlagBuilder(this).apply(build).build())
}

config {
    ModuleConfig.ENABLED_FEATURES with {
        default(listOf("core", "basic"))
        rule {
            versions {
                min(2, 0)
            }
        } implies listOf("core", "basic", "advanced", "analytics")
    }
}

// Usage
val enabledFeatures = context.evaluate(ModuleConfig.ENABLED_FEATURES)
for (feature in enabledFeatures) {
    moduleRegistry.enable(feature)
}
```

**When to use**:
- Variable sets of enabled modules
- Lists of allowed values
- Configuration arrays

## Map Flags

Key-value configuration.

```kotlin
enum class FeatureToggles(override val key: String) : Conditional<Map<String, String>, Context> {
    TOGGLES("feature_toggles"),
    ;

    override fun with(build: FlagBuilder<Map<String, String>, Context>.() -> Unit) =
        update(FlagBuilder(this).apply(build).build())
}

config {
    FeatureToggles.TOGGLES with {
        default(mapOf(
            "feature1" to "off",
            "feature2" to "off",
        ))
        rule {
            locales(AppLocale.EN_US, AppLocale.EN_CA)
        } implies mapOf(
            "feature1" to "on",
            "feature2" to "on",
            "feature3" to "beta",
        )
    }
}

// Usage
val toggles = context.evaluate(FeatureToggles.TOGGLES)
val feature1Status = toggles["feature1"] ?: "off"
```

**When to use**:
- Flexible key-value configuration
- Dynamic sets of toggles
- Configuration dictionaries

## Nested Data Structures

Combine types for complex configuration:

```kotlin
data class ThemeConfig(
    val primaryColor: String,
    val secondaryColor: String,
    val fontSizes: Map<String, Int>,
    val enabledFeatures: List<String>,
)

enum class AppConfig(override val key: String) : Conditional<ThemeConfig, Context> {
    THEME("app_theme"),
    ;

    override fun with(build: FlagBuilder<ThemeConfig, Context>.() -> Unit) =
        update(FlagBuilder(this).apply(build).build())
}

config {
    AppConfig.THEME with {
        default(ThemeConfig(
            primaryColor = "#FFFFFF",
            secondaryColor = "#F0F0F0",
            fontSizes = mapOf(
                "body" to 14,
                "heading" to 20,
            ),
            enabledFeatures = listOf("animations"),
        ))
        rule {
            locales(AppLocale.EN_US)
        } implies ThemeConfig(
            primaryColor = "#1E1E1E",
            secondaryColor = "#2D2D2D",
            fontSizes = mapOf(
                "body" to 16,
                "heading" to 24,
            ),
            enabledFeatures = listOf("animations", "dark-mode", "high-contrast"),
        )
    }
}
```

## Sealed Classes

Use sealed classes for type-safe variants:

```kotlin
sealed class PaymentMethod {
    data class CreditCard(val last4: String) : PaymentMethod()
    data class PayPal(val email: String) : PaymentMethod()
    data class BankTransfer(val accountNumber: String) : PaymentMethod()
}

enum class PaymentConfig(override val key: String) : Conditional<PaymentMethod, Context> {
    DEFAULT_METHOD("default_payment_method"),
    ;

    override fun with(build: FlagBuilder<PaymentMethod, Context>.() -> Unit) =
        update(FlagBuilder(this).apply(build).build())
}

config {
    PaymentConfig.DEFAULT_METHOD with {
        default(PaymentMethod.CreditCard("****"))
        rule {
            locales(AppLocale.EN_US)
        } implies PaymentMethod.PayPal("default@example.com")
    }
}

// Usage
when (val method = context.evaluate(PaymentConfig.DEFAULT_METHOD)) {
    is PaymentMethod.CreditCard -> processCreditCard(method.last4)
    is PaymentMethod.PayPal -> processPayPal(method.email)
    is PaymentMethod.BankTransfer -> processBankTransfer(method.accountNumber)
}
```

## Type Safety Benefits

### Compile-Time Verification

```kotlin
enum class Features(override val key: String) : Conditional<Boolean, Context> {
    FEATURE_A("feature_a"),
    ;

    override fun with(build: FlagBuilder<Boolean, Context>.() -> Unit) =
        update(FlagBuilder(this).apply(build).build())
}

config {
    Features.FEATURE_A with {
        default(false)
        rule {
        } implies true  // ✓ Type matches (Boolean)
        // } implies "invalid"  // ✗ Compile error: type mismatch
    }
}

// Evaluation is type-safe
val result: Boolean = context.evaluate(Features.FEATURE_A)  // ✓ Correct type
// val result: String = context.evaluate(Features.FEATURE_A)  // ✗ Compile error
```

### IDE Support

- Auto-completion for all flag names
- Type inference for evaluation results
- Refactoring support (rename, extract, etc.)
- Inline documentation

## Best Practices

### 1. Use the Right Type for the Job

```kotlin
// Good: Boolean for on/off decisions
enum class Features(...) : Conditional<Boolean, Context>

// Good: Enum for multiple options
enum class LogLevel { DEBUG, INFO, WARN }
enum class Config(...) : Conditional<LogLevel, Context>

// Bad: String when enum would be better
// enum class Config(...) : Conditional<String, Context>  // "debug", "info" prone to typos
```

### 2. Keep Value Types Simple

```kotlin
// Good: Simple data class
data class ApiConfig(
    val url: String,
    val timeout: Int,
)

// Bad: Includes behavior or mutable state
data class ApiConfig(
    val url: String,
    var timeout: Int,  // Mutable
    val client: HttpClient,  // Heavy object
)
```

### 3. Use Data Classes for Immutability

```kotlin
// Good: Immutable data class
data class Config(val value: String)

// Bad: Mutable class
class Config {
    var value: String = ""
}
```

### 4. Document Complex Types

```kotlin
/**
 * Configuration for the analytics system.
 * @property endpoint The analytics endpoint URL
 * @property batchSize Number of events to batch before sending
 * @property enabled Whether analytics is enabled at all
 */
data class AnalyticsConfig(
    val endpoint: String,
    val batchSize: Int,
    val enabled: Boolean,
)
```

### 5. Consider Sealed Classes for Variants

When you have multiple mutually exclusive options with different data, sealed classes are better than enums:

```kotlin
// Good: Sealed class with associated data
sealed class CacheStrategy {
    data class InMemory(val maxSize: Int) : CacheStrategy()
    data class OnDisk(val path: String, val maxSize: Long) : CacheStrategy()
    object None : CacheStrategy()
}

// Less ideal: Separate flags for each option
// enum class CacheType : Conditional<String, Context>  // "inmemory", "disk", "none"
// enum class CacheSize : Conditional<Int, Context>  // Only relevant for some types
```

## Summary

Konditional's support for arbitrary value types allows you to:

- **Use the right type** for each use case
- **Maintain type safety** throughout your codebase
- **Get compile-time errors** instead of runtime failures
- **Leverage IDE features** like auto-completion and refactoring
- **Express complex configuration** with nested data structures

Choose types that accurately model your domain and let Konditional's type system ensure correctness.
