# Feature Registration

Features are the entry point for defining and evaluating feature flags in Konditional. This document covers feature creation patterns, registration strategies, and organizational best practices.

## Feature Interface

The `Feature` interface represents a configurable flag with a specific value type and evaluation context:

```kotlin
interface Feature<S : EncodableValue<T>, T : Any, C : Context> {
    val registry: FlagRegistry
    val key: String

    fun update(definition: FlagDefinition<S, T, C>)
}
```

### Type Parameters

- **S**: The `EncodableValue` type wrapping the actual value
- **T**: The actual value type (Boolean, String, custom types, etc.)
- **C**: The context type used for evaluation

## Creating Features

### Direct Creation

Create features directly using the factory function:

```kotlin
val DARK_MODE: Feature<EncodableValue.BooleanEncodeable, Boolean, Context> =
    Feature("dark_mode")

val API_URL: Feature<EncodableValue.StringEncodeable, String, Context> =
    Feature("api_url")
```

### Specialized Factory Methods

Use factory methods for specific value types:

```kotlin
// JSON Object features (complex data classes)
val API_CONFIG: Feature.OfJsonObject<ApiConfig, Context> =
    Feature.jsonObject("api_config")

// Custom wrapper features (domain types)
val CREATED_AT: Feature.OfCustom<DateTime, String, Context> =
    Feature.custom("created_at")
```

## Organizational Patterns

### Enum Pattern (Recommended)

Organize features as enum members for type safety and discoverability:

```kotlin
enum class AppFeatures(
    override val key: String
) : Feature<EncodableValue.BooleanEncodeable, Boolean, Context> {
    DARK_MODE("dark_mode"),
    NEW_CHECKOUT("new_checkout"),
    ADVANCED_SEARCH("advanced_search"),
    ANALYTICS_ENABLED("analytics_enabled");

    override val registry: FlagRegistry = FlagRegistry
}
```

Benefits:
- IDE auto-completion
- Compile-time existence checking
- Easy to find all features
- Refactoring support

Usage:

```kotlin
// Configure
config {
    AppFeatures.DARK_MODE with {
        default(false)
    }
}

// Evaluate
val enabled = context.evaluateSafe(AppFeatures.DARK_MODE)
```

### Object Pattern

For features with different value types, use object declarations:

```kotlin
object Features {
    val DARK_MODE: Feature<EncodableValue.BooleanEncodeable, Boolean, Context> =
        Feature("dark_mode")

    val API_ENDPOINT: Feature<EncodableValue.StringEncodeable, String, Context> =
        Feature("api_endpoint")

    val MAX_RETRIES: Feature<EncodableValue.IntEncodeable, Int, Context> =
        Feature("max_retries")

    val THEME_CONFIG: Feature.OfJsonObject<ThemeConfig, Context> =
        Feature.jsonObject("theme_config")
}
```

### Grouped by Domain

Organize features by functional area:

```kotlin
// UI Features
enum class UIFeatures(
    override val key: String
) : Feature<EncodableValue.BooleanEncodeable, Boolean, Context> {
    DARK_MODE("ui_dark_mode"),
    ANIMATIONS_ENABLED("ui_animations"),
    COMPACT_VIEW("ui_compact_view");

    override val registry: FlagRegistry = FlagRegistry
}

// API Features
enum class ApiFeatures(
    override val key: String
) : Feature<EncodableValue.StringEncodeable, String, Context> {
    ENDPOINT("api_endpoint"),
    VERSION("api_version");

    override val registry: FlagRegistry = FlagRegistry
}

// Experimental Features
enum class ExperimentalFeatures(
    override val key: String
) : Feature<EncodableValue.BooleanEncodeable, Boolean, Context> {
    NEW_ALGORITHM("exp_new_algorithm"),
    ML_PREDICTIONS("exp_ml_predictions");

    override val registry: FlagRegistry = FlagRegistry
}
```

## Value Type Patterns

### Boolean Features

The most common feature type for on/off toggles:

```kotlin
enum class FeatureToggles(
    override val key: String
) : Feature<EncodableValue.BooleanEncodeable, Boolean, Context> {
    NEW_UI("new_ui"),
    BETA_FEATURES("beta_features");

    override val registry: FlagRegistry = FlagRegistry
}

config {
    FeatureToggles.NEW_UI with {
        default(false)
        rule { platforms(Platform.WEB) }.implies(true)
    }
}
```

### String Features

For configuration values and endpoints:

```kotlin
enum class StringConfig(
    override val key: String
) : Feature<EncodableValue.StringEncodeable, String, Context> {
    API_ENDPOINT("api_endpoint"),
    LOG_LEVEL("log_level"),
    THEME_NAME("theme_name");

    override val registry: FlagRegistry = FlagRegistry
}

config {
    StringConfig.API_ENDPOINT with {
        default("https://api.prod.example.com")
        rule { platforms(Platform.WEB) }.implies("https://api.staging.example.com")
    }
}
```

### Numeric Features

For thresholds, limits, and numeric configuration:

```kotlin
enum class NumericConfig(
    override val key: String
) : Feature<EncodableValue.IntEncodeable, Int, Context> {
    MAX_RETRIES("max_retries"),
    TIMEOUT_SECONDS("timeout_seconds"),
    BATCH_SIZE("batch_size");

    override val registry: FlagRegistry = FlagRegistry
}

config {
    NumericConfig.MAX_RETRIES with {
        default(3)
        rule { platforms(Platform.ANDROID) }.implies(5)
    }
}
```

### Complex Object Features

For structured configuration:

```kotlin
data class ApiConfig(
    val baseUrl: String,
    val timeout: Int,
    val retryEnabled: Boolean
)

object ComplexFeatures {
    val API_CONFIG: Feature.OfJsonObject<ApiConfig, Context> =
        Feature.jsonObject("api_config")
}

config {
    ComplexFeatures.API_CONFIG with {
        default(ApiConfig(
            baseUrl = "https://api.prod.example.com",
            timeout = 30,
            retryEnabled = true
        ))

        rule {
            platforms(Platform.WEB)
        }.implies(ApiConfig(
            baseUrl = "https://api.staging.example.com",
            timeout = 60,
            retryEnabled = false
        ))
    }
}
```

### Custom Wrapper Types

For domain-specific types that encode to primitives:

```kotlin
data class ApiUrl(val value: String)
data class Timeout(val milliseconds: Long)

object CustomFeatures {
    val API_URL: Feature.OfCustom<ApiUrl, String, Context> =
        Feature.custom("api_url")

    val REQUEST_TIMEOUT: Feature.OfCustom<Timeout, Double, Context> =
        Feature.custom("request_timeout")
}

config {
    CustomFeatures.API_URL with {
        default(
            ApiUrl("https://prod.example.com").asCustomString()
                .encoder { it.value }
                .decoder { ApiUrl(it) }
        )
    }

    CustomFeatures.REQUEST_TIMEOUT with {
        default(
            Timeout(30000).asCustomDouble()
                .encoder { it.milliseconds.toDouble() }
                .decoder { Timeout(it.toLong()) }
        )
    }
}
```

## Custom Context Features

Features can require specific context types:

```kotlin
enum class EnterpriseFeatures(
    override val key: String
) : Feature<EncodableValue.BooleanEncodeable, Boolean, EnterpriseContext> {
    ADVANCED_ANALYTICS("enterprise_advanced_analytics"),
    BULK_EXPORT("enterprise_bulk_export"),
    CUSTOM_BRANDING("enterprise_custom_branding");

    override val registry: FlagRegistry = FlagRegistry
}

// These features can only be evaluated with EnterpriseContext
val enterpriseContext: EnterpriseContext = // ...
enterpriseContext.evaluateSafe(EnterpriseFeatures.ADVANCED_ANALYTICS)  // OK

val basicContext: Context = // ...
// basicContext.evaluateSafe(EnterpriseFeatures.ADVANCED_ANALYTICS)  // Compile error!
```

## Registry Management

### Singleton Registry (Default)

By default, features use the singleton registry:

```kotlin
enum class MyFeatures(
    override val key: String
) : Feature<EncodableValue.BooleanEncodeable, Boolean, Context> {
    FEATURE_A("feature_a");

    override val registry: FlagRegistry = FlagRegistry  // Singleton
}
```

### Custom Registry

Use custom registries for isolation (testing, multi-tenancy):

```kotlin
enum class TestFeatures(
    override val key: String,
    override val registry: FlagRegistry
) : Feature<EncodableValue.BooleanEncodeable, Boolean, Context> {
    ;  // Empty enum body

    companion object {
        private val testRegistry = FlagRegistry.create()

        val FEATURE_A = TestFeature("feature_a", testRegistry)
    }

    private data class TestFeature(
        override val key: String,
        override val registry: FlagRegistry
    ) : Feature<EncodableValue.BooleanEncodeable, Boolean, Context>
}

// Configure with custom registry
config(registry = testRegistry) {
    // ... configurations
}
```

## Feature Naming Conventions

### Key Naming

Use consistent, descriptive keys:

```kotlin
// Good: Clear, namespaced keys
enum class Features(override val key: String) : Feature<...> {
    NEW_CHECKOUT("checkout_v2"),
    DARK_MODE("ui_dark_mode"),
    ML_RECOMMENDATIONS("ml_recommendations_enabled");

    override val registry: FlagRegistry = FlagRegistry
}

// Avoid: Ambiguous or inconsistent keys
enum class BadFeatures(override val key: String) : Feature<...> {
    F1("f1"),  // Not descriptive
    newCheckout("newCheckout"),  // Inconsistent casing
    DARK_MODE_FEATURE_FLAG("DARK-MODE-FEATURE-FLAG");  // Mixed conventions

    override val registry: FlagRegistry = FlagRegistry
}
```

### Naming Patterns

```kotlin
// Pattern: <domain>_<feature>_<variant?>
CHECKOUT_V2("checkout_v2")
PAYMENT_APPLE_PAY("payment_apple_pay")
UI_DARK_MODE("ui_dark_mode")

// Pattern: <feature>_<state>
ANALYTICS_ENABLED("analytics_enabled")
BETA_FEATURES_AVAILABLE("beta_features_available")

// Pattern: <experiment>_<variant>
EXPERIMENT_CHECKOUT_LAYOUT_A("exp_checkout_layout_a")
EXPERIMENT_CHECKOUT_LAYOUT_B("exp_checkout_layout_b")
```

## Dynamic Features

For scenarios requiring runtime feature creation:

```kotlin
class DynamicFeatureManager {
    private val features = mutableMapOf<String, Feature<*, *, *>>()

    fun createFeature(key: String): Feature<EncodableValue.BooleanEncodeable, Boolean, Context> {
        return features.getOrPut(key) {
            Feature(key)
        } as Feature<EncodableValue.BooleanEncodeable, Boolean, Context>
    }

    fun getFeature(key: String): Feature<EncodableValue.BooleanEncodeable, Boolean, Context>? {
        return features[key] as? Feature<EncodableValue.BooleanEncodeable, Boolean, Context>
    }
}
```

However, prefer static feature definitions for type safety and discoverability.

## Feature Documentation

Document features with KDoc:

```kotlin
enum class AppFeatures(
    override val key: String
) : Feature<EncodableValue.BooleanEncodeable, Boolean, Context> {
    /**
     * Enables the new checkout flow with improved UX.
     *
     * Rollout plan:
     * - Phase 1: 10% web users (Week 1)
     * - Phase 2: 50% web users (Week 2)
     * - Phase 3: 100% all platforms (Week 3)
     *
     * @since 2.5.0
     * @see CheckoutService
     */
    NEW_CHECKOUT("new_checkout"),

    /**
     * Enables dark mode UI theme.
     *
     * Platform support:
     * - iOS: Full support
     * - Android: Full support
     * - Web: Partial support (no custom color schemes)
     *
     * @since 2.3.0
     */
    DARK_MODE("dark_mode");

    override val registry: FlagRegistry = FlagRegistry
}
```

## Testing Features

### Test-Specific Features

Create features for testing:

```kotlin
object TestFeatures {
    val testRegistry = FlagRegistry.create()

    val TEST_FEATURE: Feature<EncodableValue.BooleanEncodeable, Boolean, Context> =
        Feature("test_feature", testRegistry)
}

@Test
fun `feature evaluation works correctly`() {
    config(registry = TestFeatures.testRegistry) {
        TestFeatures.TEST_FEATURE with {
            default(false)
            rule { platforms(Platform.IOS) }.implies(true)
        }
    }

    val iosContext = Context(
        platform = Platform.IOS,
        // ...
    )

    val result = iosContext.evaluateSafe(TestFeatures.TEST_FEATURE, TestFeatures.testRegistry)
    assertTrue(result is EvaluationResult.Success && result.value == true)
}
```

### Feature Factories for Tests

Create factory functions for test features:

```kotlin
object FeatureTestFactory {
    fun createBooleanFeature(
        key: String,
        defaultValue: Boolean = false,
        registry: FlagRegistry = FlagRegistry.create()
    ): Feature<EncodableValue.BooleanEncodeable, Boolean, Context> {
        val feature = Feature<EncodableValue.BooleanEncodeable, Boolean, Context>(key, registry)

        config(registry) {
            feature with {
                default(defaultValue)
            }
        }

        return feature
    }
}
```

## Best Practices

### Use Enums for Related Features

Group related features in enums for organization:

```kotlin
// Good: Related features grouped
enum class CheckoutFeatures(override val key: String) : Feature<...> {
    GUEST_CHECKOUT("checkout_guest"),
    SAVE_PAYMENT("checkout_save_payment"),
    ONE_CLICK_BUY("checkout_one_click");

    override val registry: FlagRegistry = FlagRegistry
}

// Avoid: Scattered features
object Features {
    val GUEST_CHECKOUT = Feature("checkout_guest")
    val SAVE_PAYMENT = Feature("payment_save")
    val ANALYTICS = Feature("analytics")
    // ... unrelated features mixed together
}
```

### Make Keys Immutable

Always use `val` for feature keys:

```kotlin
// Good: Immutable key
enum class Features(override val key: String) : Feature<...>

// Bad: Mutable key
class MutableFeature(override var key: String) : Feature<...>  // Don't do this
```

### Modules are coming!!!

## Next Steps

- **[Builders](Builders.md)**: Learn the configuration DSL
- **[Rules](Rules.md)**: Understand rule evaluation
- **[Overview](Overview.md)**: Back to API overview
