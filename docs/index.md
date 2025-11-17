# Konditional

Type-safe, deterministic feature configuration for Kotlin

Konditional provides a type-safe, composable API for feature flag management. This document introduces the core concepts and demonstrates common usage patterns.

## Core Concepts

### Features

A **Feature** represents a configurable flag with a specific value type and evaluation context. Features are the primary interface for defining and evaluating flags.

Features are typically organized as enum members for type safety and IDE support:

```kotlin
enum class AppFeatures(override val key: String) : Feature<EncodableValue.BooleanEncodeable, Boolean, Context> {
    DARK_MODE("dark_mode"),
    NEW_CHECKOUT("new_checkout"),
    ADVANCED_SEARCH("advanced_search");

    override val registry: FlagRegistry = FlagRegistry
}
```

### Context

**Context** provides the evaluation environment for feature flags. It contains standard targeting dimensions (locale, platform, version, stable ID) and can be extended with custom fields:

```kotlin
// Standard context
val context = Context(
    locale = AppLocale.EN_US,
    platform = Platform.IOS,
    appVersion = Version(2, 5, 0),
    stableId = StableId.of("user-123")
)

// Custom context with domain-specific fields
data class EnterpriseContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    val organizationId: String,
    val subscriptionTier: SubscriptionTier
) : Context
```

### Rules

**Rules** define targeting criteria and rollout strategies. Each rule specifies conditions that must be met for a particular value to be returned:

```kotlin
rule {
    platforms(Platform.IOS, Platform.ANDROID)
    locales(AppLocale.EN_US)
    versions {
        min(2, 0, 0)
    }
    rollout = Rollout.of(50.0)  // 50% gradual rollout
    note("Mobile-only feature, 50% rollout")
}.implies(true)
```

### Flag Definitions

A **FlagDefinition** combines a feature with its configured rules and default value. Definitions are created through the DSL and evaluated against a context:

```kotlin
config {
    AppFeatures.DARK_MODE with {
        default(false)

        rule {
            platforms(Platform.IOS)
            rollout = Rollout.of(100.0)
        }.implies(true)
    }
}
```

## Configuration

### Using the DSL

The primary way to configure flags is through the `config` DSL:

```kotlin
config {
    // Configure a boolean flag
    AppFeatures.DARK_MODE with {
        default(false)
        salt("v2")  // Optional: change to redistribute rollout buckets

        rule {
            platforms(Platform.IOS)
        }.implies(true)
    }

    // Configure a complex type flag
    AppConfig.API_SETTINGS with {
        default(ApiSettings(url = "https://prod.api.example.com", timeout = 30))

        rule {
            platforms(Platform.WEB)
            rollout = Rollout.of(25.0)
        }.implies(ApiSettings(url = "https://beta.api.example.com", timeout = 60))
    }
}
```

### Configuration Snapshots

For testing or external configuration management, export configuration snapshots from the registry:

```kotlin
// Define features using FeatureContainer
object AppFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    val DARK_MODE by boolean(default = true)
}

// Get current configuration snapshot
val snapshot = Taxonomy.Global.konfig()

// Load into a test registry
val testRegistry = ModuleRegistry.create(snapshot)
```

## Evaluation

### Basic Evaluation

Evaluate flags by calling extension functions on your context:

```kotlin
// Returns EvaluationResult (Success, FlagNotFound, or EvaluationError)
val result: EvaluationResult<Boolean> = context.evaluateSafe(AppFeatures.DARK_MODE)

when (result) {
    is EvaluationResult.Success -> println("Dark mode: ${result.value}")
    is EvaluationResult.FlagNotFound -> println("Flag not registered: ${result.key}")
    is EvaluationResult.EvaluationError -> println("Evaluation failed: ${result.error}")
}
```

### Convenience Methods

For simpler use cases, convenience methods are available:

```kotlin
// Returns null on any failure
val enabled: Boolean? = context.evaluateOrNull(AppFeatures.DARK_MODE)

// Returns default value on failure
val enabled: Boolean = context.evaluateOrDefault(AppFeatures.DARK_MODE, default = false)

// Throws exception on failure (use sparingly)
val enabled: Boolean = context.evaluateOrThrow(AppFeatures.DARK_MODE)
```

## Value Types

Konditional supports multiple value types through the `EncodableValue` abstraction.

### Primitive Types

Boolean, String, Int, and Double are supported natively:

```kotlin
val booleanFlag: Feature<EncodableValue.BooleanEncodeable, Boolean, Context> = Feature("bool")
val stringFlag: Feature<EncodableValue.StringEncodeable, String, Context> = Feature("string")
val intFlag: Feature<EncodableValue.IntEncodeable, Int, Context> = Feature("int")
val doubleFlag: Feature<EncodableValue.DecimalEncodeable, Double, Context> = Feature("double")
```

### JSON Object Types

Complex data classes can be used via the `OfJsonObject` feature type:

```kotlin
data class ThemeConfig(val primaryColor: String, val fontSize: Int)

val themeFlag: Feature.OfJsonObject<ThemeConfig, Context> =
    Feature.jsonObject("theme")

config {
    themeFlag with {
        default(ThemeConfig(primaryColor = "#FFFFFF", fontSize = 14))
    }
}
```

### Custom Wrapper Types

Extend primitives with domain-specific wrappers:

```kotlin
data class ApiUrl(val value: String)

val urlFlag: Feature.OfCustom<ApiUrl, String, Context> =
    Feature.custom("api_url")

config {
    urlFlag with {
        default(
            ApiUrl("https://prod.example.com").asCustomString()
                .encoder { it.value }
                .decoder { ApiUrl(it) }
        )
    }
}
```

## Registry Management

### Singleton Registry

By default, Konditional uses a thread-safe singleton registry:

```kotlin
// Load configuration into singleton
config {
    // ... flag definitions
}

// Evaluate using singleton (implicit)
context.evaluateSafe(AppFeatures.DARK_MODE)
```

### Custom Registries

Create isolated registries for testing or multi-tenant scenarios:

```kotlin
// Create empty registry
val customRegistry = FlagRegistry.create()

// Create with initial configuration from another registry
val snapshot = Taxonomy.Global.konfig()
val customRegistry = ModuleRegistry.create(snapshot)

// Evaluate using custom registry
context.evaluateSafe(AppFeatures.DARK_MODE, registry = customRegistry)
```

### Dynamic Updates

Update configurations at runtime without restarting:

```kotlin
// Update individual flag
val newDefinition = FlagDefinition(
    feature = AppFeatures.DARK_MODE,
    defaultValue = true,
    bounds = emptyList()
)
registry.update(newDefinition)

// Load complete new configuration
registry.load(newConfig)

// Apply incremental patch
registry.update(patch)
```

## Error Handling

Konditional follows the "parse, don't validate" principle with explicit result types.

### EvaluationResult

The primary result type for flag evaluation:

```kotlin
sealed interface EvaluationResult<out S> {
    data class Success<S>(val value: S) : EvaluationResult<S>
    data class FlagNotFound(val key: String) : EvaluationResult<Nothing>
    data class EvaluationError(val key: String, val error: Throwable) : EvaluationResult<Nothing>
}
```

### Folding Results

Transform evaluation results into your preferred error handling type:

```kotlin
val outcome = context.evaluateSafe(AppFeatures.DARK_MODE).fold(
    onSuccess = { Result.success(it) },
    onFlagNotFound = { Result.failure(FlagMissingException(it)) },
    onEvaluationError = { key, err -> Result.failure(FlagFailedException(key, err)) }
)
```

### ParseResult

Used for deserialization operations:

```kotlin
sealed interface ParseResult<out T> {
    data class Success<T>(val value: T) : ParseResult<T>
    data class Failure(val error: ParseError) : ParseResult<Nothing>
}
```

## Thread Safety

Konditional is designed for safe concurrent access:

- **Lock-free reads**: Flag evaluation requires no locks
- **Atomic updates**: Configuration changes are atomic via `AtomicReference`
- **Independent evaluations**: Each flag evaluation is independent and thread-safe
- **Deterministic bucketing**: Rollout assignments are stable across threads

## Next Steps

- **[Context](Context.md)**: Learn about context types and custom extensions
- **[Rules](Rules.md)**: Deep dive into rule evaluation and specificity
- **[Flags](Flags.md)**: Understand feature registration patterns
- **[Builders](Builders.md)**: Master the configuration DSL
- **[Serialization](Serialization.md)**: Work with JSON configurations
- **[Architecture](Architecture.md)**: Understand the internal design
