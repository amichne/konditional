# Feature Definition

Features are the core building blocks of Konditional. They define the flags you want to evaluate, their value types, and which taxonomy they belong to. This document covers the two patterns for defining features, their type parameters, and best practices for organizing your feature flags.

---

## Overview

A Feature represents a configurable flag with a specific value type. Konditional provides two patterns for defining features:

1. **FeatureContainer Pattern** (Recommended): Property delegation with automatic registration
2. **Enum Pattern** (Alternative): Manual enum-based implementation

Both patterns provide the same type safety guarantees and evaluation behavior. Choose based on your organizational needs.

---

## FeatureContainer Pattern (Recommended)

The `FeatureContainer` pattern uses Kotlin's property delegation to provide a clean, declarative syntax for defining features with minimal boilerplate.

### Basic Usage

```kotlin
object PaymentFeatures : FeatureContainer<Taxonomy.Domain.Payments>(
    Taxonomy.Domain.Payments
) {
    val APPLE_PAY by boolean<Context>(default = false)
    val GOOGLE_PAY by boolean<Context>(default = false)
    val MAX_CARDS by int<Context>(default = 5)
    val API_ENDPOINT by string<Context>(default = "https://api.example.com")
    val TRANSACTION_FEE by double<Context>(default = 0.029)
}
```

### Delegation Methods

FeatureContainer provides four delegation methods, one for each supported primitive type:

#### `boolean(default, flagScope)`

Creates a Boolean feature flag.

```kotlin
val DARK_MODE by boolean<Context>(default = false) {
    rule {
        platforms(Platform.IOS, Platform.ANDROID)
    } implies true
}
```

**Parameters:**
- `default: Boolean` - The default value for this feature (required)
- `flagScope` - DSL block for configuring rules, rollouts, and targeting (optional)

**Returns:** `BooleanFeature<C, M>`

#### `string(default, stringScope)`

Creates a String feature flag.

```kotlin
val API_ENDPOINT by string<Context>(default = "https://api.prod.com") {
    rule {
        platforms(Platform.WEB)
    } implies "https://api.staging.com"
}
```

**Parameters:**
- `default: String` - The default value for this feature (required)
- `stringScope` - DSL block for configuring rules, rollouts, and targeting (optional)

**Returns:** `StringFeature<C, M>`

#### `int(default, integerScope)`

Creates an Int feature flag.

```kotlin
val MAX_RETRIES by int<Context>(default = 3) {
    rule {
        platforms(Platform.ANDROID)
        rollout = Rollout.of(50.0)
    } implies 5
}
```

**Parameters:**
- `default: Int` - The default value for this feature (required)
- `integerScope` - DSL block for configuring rules, rollouts, and targeting (optional)

**Returns:** `IntFeature<C, M>`

#### `double(default, decimalScope)`

Creates a Double feature flag.

```kotlin
val TRANSACTION_FEE by double<Context>(default = 0.029) {
    rule {
        platforms(Platform.WEB)
    } implies 0.019
}
```

**Parameters:**
- `default: Double` - The default value for this feature (required)
- `decimalScope` - DSL block for configuring rules, rollouts, and targeting (optional)

**Returns:** `DoubleFeature<C, M>`

### Property Delegation Pattern

FeatureContainer uses Kotlin's property delegation (`by`) to automatically:

1. **Capture the property name** as the feature key
2. **Create the feature** on first access (lazy initialization)
3. **Register the feature** in the container's feature list
4. **Apply configuration** from the DSL block

```kotlin
object MyFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    // Property name "NEW_CHECKOUT" becomes the feature key
    val NEW_CHECKOUT by boolean<Context>(default = false) {
        // Configuration is automatically applied to Taxonomy.Global
        rule {
            platforms(Platform.WEB)
        } implies true
    }
}

// Feature key is "NEW_CHECKOUT" (derived from property name)
println(MyFeatures.NEW_CHECKOUT.key) // "NEW_CHECKOUT"
```

**Key behaviors:**
- Features are created lazily when first accessed
- Configuration is applied atomically when the feature is created
- Property name must be a valid Kotlin identifier (no spaces, special characters)
- Use uppercase snake_case convention for consistency

### `allFeatures()` Method

FeatureContainer provides complete enumeration of all features in the container:

```kotlin
val all: List<Feature<*, *, *, Taxonomy.Domain.Payments>> =
    PaymentFeatures.allFeatures()

all.forEach { feature ->
    println("Feature: ${feature.key}")
}
// Output:
// Feature: APPLE_PAY
// Feature: GOOGLE_PAY
// Feature: MAX_CARDS
// Feature: API_ENDPOINT
// Feature: TRANSACTION_FEE
```

**Use cases:**
- **Validation**: Ensure all features are configured
- **Testing**: Iterate over all features for comprehensive tests
- **Auditing**: Generate inventory of all features
- **Documentation**: Auto-generate feature lists

**Important notes:**
- Features are only added to the list when their property is accessed
- The list contains features that have been accessed at least once
- For guaranteed completeness, access all properties before calling `allFeatures()`

### Benefits Over Enum Pattern

| Feature | FeatureContainer | Enum Pattern |
|---------|-----------------|--------------|
| **Mixed types** | Multiple types in one container | Single type per enum |
| **Boilerplate** | Minimal (taxonomy declared once) | High (module override per entry) |
| **Enumeration** | Automatic via `allFeatures()` | Manual tracking required |
| **Configuration** | Inline with declaration | Separate config block |
| **IDE support** | Full autocomplete and refactoring | Full autocomplete and refactoring |

### Example: Mixed Type Container

```kotlin
object AppConfig : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    // Boolean toggles
    val DARK_MODE by boolean<Context>(default = false)
    val ANALYTICS_ENABLED by boolean<Context>(default = true)

    // String configuration
    val API_ENDPOINT by string<Context>(default = "https://api.prod.com")
    val LOG_LEVEL by string<Context>(default = "INFO")

    // Numeric configuration
    val MAX_RETRIES by int<Context>(default = 3)
    val TIMEOUT_SECONDS by int<Context>(default = 30)
    val TRANSACTION_FEE by double<Context>(default = 0.029)
}

// All features in one place, different types
val allConfig = AppConfig.allFeatures()
println("Total config entries: ${allConfig.size}") // 7
```

---

## Enum Pattern (Alternative)

The enum pattern uses manual enum definitions implementing specific feature interfaces. This pattern is useful when:

- All features in a group have the same value type
- You want exhaustive when-expressions
- You need backwards compatibility with existing enum-based code

### Feature Interfaces

Konditional provides four type-specific feature interfaces:

#### `BooleanFeature<C, M>`

For Boolean feature flags (on/off toggles):

```kotlin
enum class FeatureToggles(override val key: String) :
    BooleanFeature<Context, Taxonomy.Global> {

    DARK_MODE("dark_mode"),
    NEW_UI("new_ui"),
    BETA_FEATURES("beta_features");

    override val module = Taxonomy.Global
}
```

#### `StringFeature<C, M>`

For String configuration values:

```kotlin
enum class StringConfig(override val key: String) :
    StringFeature<Context, Taxonomy.Global> {

    API_ENDPOINT("api_endpoint"),
    LOG_LEVEL("log_level"),
    THEME_NAME("theme_name");

    override val module = Taxonomy.Global
}
```

#### `IntFeature<C, M>`

For Integer numeric values:

```kotlin
enum class NumericConfig(override val key: String) :
    IntFeature<Context, Taxonomy.Global> {

    MAX_RETRIES("max_retries"),
    TIMEOUT_SECONDS("timeout_seconds"),
    BATCH_SIZE("batch_size");

    override val module = Taxonomy.Global
}
```

#### `DoubleFeature<C, M>`

For Double precision numeric values:

```kotlin
enum class DecimalConfig(override val key: String) :
    DoubleFeature<Context, Taxonomy.Global> {

    TRANSACTION_FEE("transaction_fee"),
    DISCOUNT_RATE("discount_rate"),
    TAX_RATE("tax_rate");

    override val module = Taxonomy.Global
}
```

### Enum Pattern Implementation

Each enum must:

1. Implement one of the four feature interfaces
2. Override `key` with the feature identifier
3. Override `module` with the taxonomy

```kotlin
enum class PaymentFeatures(override val key: String) :
    BooleanFeature<Context, Taxonomy.Domain.Payments> {

    APPLE_PAY("apple_pay"),
    GOOGLE_PAY("google_pay"),
    CARD_ON_FILE("card_on_file");

    override val module = Taxonomy.Domain.Payments
}
```

### Configuration (Separate)

With the enum pattern, configuration is done separately from declaration:

```kotlin
// Declaration (above)
enum class PaymentFeatures(...) : BooleanFeature<...> { ... }

// Configuration (separate)
Taxonomy.Domain.Payments.config {
    PaymentFeatures.APPLE_PAY with {
        default(false)
        rule {
            platforms(Platform.IOS)
        } implies true
    }
}
```

### When to Use Enum Pattern

**Choose enum pattern when:**
- All features have the same value type (all Boolean, all String, etc.)
- You need exhaustive when-expressions over features
- You're working with existing enum-based code
- You prefer separation between declaration and configuration

**Choose FeatureContainer when:**
- You have mixed types (Boolean, String, Int in same group)
- You want inline configuration with declaration
- You need automatic enumeration via `allFeatures()`
- You want minimal boilerplate

---

## Type Parameters Explained

All feature definitions use four generic type parameters:

### Type Parameter Reference

```kotlin
Feature<S : EncodableValue<T>, T : Any, C : Context, M : Taxonomy>
```

| Parameter | Meaning | Examples | Purpose |
|-----------|---------|----------|---------|
| **S** | EncodableValue wrapper | `BooleanEncodeable`, `StringEncodeable` | Internal serialization type (automatically inferred) |
| **T** | Actual value type | `Boolean`, `String`, `Int`, `Double` | The type returned by `evaluate()` |
| **C** | Context type | `Context`, `EnterpriseContext` | Evaluation context required |
| **M** | Taxonomy (module) | `Taxonomy.Global`, `Taxonomy.Domain.Payments` | Namespace and registry isolation |

### Understanding S (EncodableValue)

The `S` parameter wraps the actual value type for serialization. You rarely interact with it directly:

```kotlin
// These are equivalent type declarations
BooleanFeature<Context, Taxonomy.Global>
Feature<EncodableValue.BooleanEncodeable, Boolean, Context, Taxonomy.Global>

StringFeature<Context, Taxonomy.Global>
Feature<EncodableValue.StringEncodeable, String, Context, Taxonomy.Global>

IntFeature<Context, Taxonomy.Global>
Feature<EncodableValue.IntEncodeable, Int, Context, Taxonomy.Global>

DoubleFeature<Context, Taxonomy.Global>
Feature<EncodableValue.DecimalEncodeable, Double, Context, Taxonomy.Global>
```

**You typically use the simplified interfaces** (`BooleanFeature`, `StringFeature`, etc.) instead of specifying `S` manually.

### Understanding T (Value Type)

The `T` parameter determines what type `evaluate()` returns:

```kotlin
val DARK_MODE: BooleanFeature<Context, Taxonomy.Global> = // ...
val enabled: Boolean = context.evaluate(DARK_MODE)
//            ^^^^^^^ T = Boolean

val API_URL: StringFeature<Context, Taxonomy.Global> = // ...
val url: String = context.evaluate(API_URL)
//       ^^^^^^ T = String

val MAX_RETRIES: IntFeature<Context, Taxonomy.Global> = // ...
val retries: Int = context.evaluate(MAX_RETRIES)
//           ^^^ T = Int

val FEE_RATE: DoubleFeature<Context, Taxonomy.Global> = // ...
val fee: Double = context.evaluate(FEE_RATE)
//       ^^^^^^ T = Double
```

**Supported primitive types:**
- `Boolean` - true/false toggles
- `String` - text configuration
- `Int` - integer values
- `Double` - decimal values

### Understanding C (Context Type)

The `C` parameter specifies what context information is required for evaluation:

```kotlin
// Basic context (standard fields)
val DARK_MODE by boolean<Context>(default = false)
context.evaluate(DARK_MODE) // Requires Context

// Custom context (additional fields)
val ENTERPRISE_ANALYTICS by boolean<EnterpriseContext>(default = false)
enterpriseContext.evaluate(ENTERPRISE_ANALYTICS) // Requires EnterpriseContext

// Type safety prevents misuse
basicContext.evaluate(ENTERPRISE_ANALYTICS) // ❌ Compile error!
```

See [Custom Context with Features](#custom-context-with-features) for details.

### Understanding M (Taxonomy)

The `M` parameter binds features to their taxonomy, providing isolation:

```kotlin
object CoreFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    val KILL_SWITCH by boolean<Context>(default = false)
}
// KILL_SWITCH.module == Taxonomy.Global

object PaymentFeatures : FeatureContainer<Taxonomy.Domain.Payments>(
    Taxonomy.Domain.Payments
) {
    val APPLE_PAY by boolean<Context>(default = false)
}
// APPLE_PAY.module == Taxonomy.Domain.Payments
```

**Benefits of taxonomy isolation:**
- Different teams can use the same feature keys without collision
- Each taxonomy has its own registry instance
- Compile-time enforcement prevents cross-taxonomy access
- Independent deployment and versioning

---

## Custom Context with Features

Features can require specific context types beyond the standard `Context` interface.

### Defining Custom Context

```kotlin
data class EnterpriseContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    // Custom fields
    val organizationId: String,
    val subscriptionTier: SubscriptionTier,
    val seatCount: Int
) : Context

enum class SubscriptionTier {
    STARTER, PROFESSIONAL, ENTERPRISE
}
```

### Features Requiring Custom Context

#### FeatureContainer Pattern

```kotlin
object EnterpriseFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    // Requires EnterpriseContext
    val ADVANCED_ANALYTICS by boolean<EnterpriseContext>(default = false) {
        rule {
            // Can access EnterpriseContext fields
            custom { ctx ->
                ctx.subscriptionTier == SubscriptionTier.ENTERPRISE
            }
        } implies true
    }

    val BULK_EXPORT by boolean<EnterpriseContext>(default = false) {
        rule {
            custom { ctx -> ctx.seatCount >= 50 }
        } implies true
    }
}
```

#### Enum Pattern

```kotlin
enum class EnterpriseFeatures(override val key: String) :
    BooleanFeature<EnterpriseContext, Taxonomy.Global> {

    ADVANCED_ANALYTICS("advanced_analytics"),
    BULK_EXPORT("bulk_export"),
    CUSTOM_BRANDING("custom_branding");

    override val module = Taxonomy.Global
}
```

### Type Safety with Custom Context

The compiler enforces context type requirements:

```kotlin
val enterpriseCtx: EnterpriseContext = // ...
val basicCtx: Context = // ...

// ✅ Correct: EnterpriseContext provided
enterpriseCtx.evaluate(EnterpriseFeatures.ADVANCED_ANALYTICS)

// ❌ Compile error: Context is not EnterpriseContext
basicCtx.evaluate(EnterpriseFeatures.ADVANCED_ANALYTICS)

// ✅ Standard features work with both
enterpriseCtx.evaluate(CoreFeatures.DARK_MODE)
basicCtx.evaluate(CoreFeatures.DARK_MODE)
```

### Polymorphic Context Usage

Custom contexts are subypes of `Context`, enabling polymorphic usage:

```kotlin
fun evaluateFeature(ctx: Context, feature: BooleanFeature<Context, Taxonomy.Global>): Boolean {
    return ctx.evaluate(feature)
}

// Works with both
val basic = Context(...)
val enterprise = EnterpriseContext(...)

evaluateFeature(basic, CoreFeatures.DARK_MODE)
evaluateFeature(enterprise, CoreFeatures.DARK_MODE)

// But enterprise-specific features require EnterpriseContext
fun evaluateEnterpriseFeature(
    ctx: EnterpriseContext,
    feature: BooleanFeature<EnterpriseContext, Taxonomy.Global>
): Boolean {
    return ctx.evaluate(feature)
}
```

---

## Organizational Patterns

### Single Container per Taxonomy

Organize features by functional area using one container per taxonomy:

```kotlin
object CoreFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    val KILL_SWITCH by boolean<Context>(default = false)
    val MAINTENANCE_MODE by boolean<Context>(default = false)
    val API_VERSION by string<Context>(default = "v1")
}

object PaymentFeatures : FeatureContainer<Taxonomy.Domain.Payments>(
    Taxonomy.Domain.Payments
) {
    val APPLE_PAY by boolean<Context>(default = false)
    val GOOGLE_PAY by boolean<Context>(default = false)
    val MAX_TRANSACTION by int<Context>(default = 10000)
}

object MessagingFeatures : FeatureContainer<Taxonomy.Domain.Messaging>(
    Taxonomy.Domain.Messaging
) {
    val PUSH_NOTIFICATIONS by boolean<Context>(default = true)
    val EMAIL_ENABLED by boolean<Context>(default = true)
    val MAX_MESSAGE_LENGTH by int<Context>(default = 500)
}
```

### Multiple Containers per Taxonomy

For large domains, split features across multiple containers sharing a taxonomy:

```kotlin
// All share Taxonomy.Domain.Payments
object CheckoutFeatures : FeatureContainer<Taxonomy.Domain.Payments>(
    Taxonomy.Domain.Payments
) {
    val GUEST_CHECKOUT by boolean<Context>(default = false)
    val ONE_CLICK_BUY by boolean<Context>(default = false)
}

object PaymentMethodFeatures : FeatureContainer<Taxonomy.Domain.Payments>(
    Taxonomy.Domain.Payments
) {
    val APPLE_PAY by boolean<Context>(default = false)
    val GOOGLE_PAY by boolean<Context>(default = false)
    val CRYPTO_PAYMENTS by boolean<Context>(default = false)
}

object FraudDetectionFeatures : FeatureContainer<Taxonomy.Domain.Payments>(
    Taxonomy.Domain.Payments
) {
    val RISK_SCORING by boolean<Context>(default = true)
    val MANUAL_REVIEW_THRESHOLD by int<Context>(default = 1000)
}
```

**All features share the same registry** since they use the same taxonomy.

### Grouped by Type (Enum Pattern)

When using enums, group by both domain and type:

```kotlin
// Payment domain - Boolean features
enum class PaymentToggles(override val key: String) :
    BooleanFeature<Context, Taxonomy.Domain.Payments> {
    APPLE_PAY("apple_pay"),
    GOOGLE_PAY("google_pay");
    override val module = Taxonomy.Domain.Payments
}

// Payment domain - String config
enum class PaymentConfig(override val key: String) :
    StringFeature<Context, Taxonomy.Domain.Payments> {
    API_ENDPOINT("api_endpoint"),
    PROVIDER_NAME("provider_name");
    override val module = Taxonomy.Domain.Payments
}

// Payment domain - Numeric limits
enum class PaymentLimits(override val key: String) :
    IntFeature<Context, Taxonomy.Domain.Payments> {
    MAX_TRANSACTION("max_transaction"),
    DAILY_LIMIT("daily_limit");
    override val module = Taxonomy.Domain.Payments
}
```

---

## Best Practices

### 1. Use FeatureContainer for New Code

For new projects and features, prefer `FeatureContainer` over enum pattern:

```kotlin
// ✅ Recommended: FeatureContainer
object MyFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    val FEATURE_A by boolean<Context>(default = false)
    val CONFIG_B by string<Context>(default = "default")
    val LIMIT_C by int<Context>(default = 100)
}

// ⚠️  Alternative: Enum pattern (more boilerplate)
enum class MyFeatures(override val key: String) :
    BooleanFeature<Context, Taxonomy.Global> {
    FEATURE_A("feature_a");
    override val module = Taxonomy.Global
}
```

### 2. Choose Meaningful Property Names

Property names become feature keys automatically:

```kotlin
// ✅ Good: Clear, descriptive names
object Features : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    val DARK_MODE by boolean<Context>(default = false)
    val NEW_CHECKOUT_FLOW by boolean<Context>(default = false)
    val API_ENDPOINT by string<Context>(default = "https://api.prod.com")
}

// ❌ Avoid: Ambiguous or inconsistent names
object Features : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    val f1 by boolean<Context>(default = false)  // Not descriptive
    val darkMode by boolean<Context>(default = false)  // Inconsistent casing
}
```

**Conventions:**
- Use SCREAMING_SNAKE_CASE for consistency
- Be descriptive (prefer `NEW_CHECKOUT_FLOW` over `NCF`)
- Avoid abbreviations unless universally understood

### 3. Provide Defaults for All Features

Always specify default values to ensure non-null returns:

```kotlin
// ✅ Good: Default provided
val DARK_MODE by boolean<Context>(default = false)

// ❌ Won't compile: Default is required
val DARK_MODE by boolean<Context>() // Missing default parameter
```

### 4. Group Related Features

Organize features by functional area or team ownership:

```kotlin
// ✅ Good: Features grouped by domain
object AuthFeatures : FeatureContainer<Taxonomy.Domain.Authentication>(
    Taxonomy.Domain.Authentication
) {
    val SSO_ENABLED by boolean<Context>(default = false)
    val MFA_REQUIRED by boolean<Context>(default = false)
    val PASSWORD_MIN_LENGTH by int<Context>(default = 8)
}

object PaymentFeatures : FeatureContainer<Taxonomy.Domain.Payments>(
    Taxonomy.Domain.Payments
) {
    val APPLE_PAY by boolean<Context>(default = false)
    val MAX_TRANSACTION by int<Context>(default = 10000)
}

// ❌ Avoid: Unrelated features mixed together
object Features : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    val SSO_ENABLED by boolean<Context>(default = false)
    val APPLE_PAY by boolean<Context>(default = false)
    val DARK_MODE by boolean<Context>(default = false)
    // ... unrelated features
}
```

### 5. Use Appropriate Taxonomies

Choose taxonomies that match your organizational structure:

```kotlin
// ✅ Global: System-wide features
object CoreFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    val KILL_SWITCH by boolean<Context>(default = false)
    val MAINTENANCE_MODE by boolean<Context>(default = false)
}

// ✅ Domain: Team-specific features
object PaymentFeatures : FeatureContainer<Taxonomy.Domain.Payments>(
    Taxonomy.Domain.Payments
) {
    val APPLE_PAY by boolean<Context>(default = false)
}
```

### 6. Leverage `allFeatures()` for Validation

Use feature enumeration to ensure complete configuration:

```kotlin
fun validateConfiguration() {
    val allFeatures = PaymentFeatures.allFeatures()
    val configuredKeys = loadConfiguredKeysFromRemote()

    val missingKeys = allFeatures.map { it.key }.toSet() - configuredKeys

    if (missingKeys.isNotEmpty()) {
        logger.warn("Features not configured remotely: $missingKeys")
    }
}
```

### 7. Document Complex Features

Add KDoc comments for features with complex behavior:

```kotlin
object Features : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
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
    val NEW_CHECKOUT by boolean<Context>(default = false) {
        rule {
            platforms(Platform.WEB)
            rollout = Rollout.of(10.0)
        } implies true
    }
}
```

### 8. Keep Feature Keys Stable

Feature keys are used for persistence and remote configuration:

```kotlin
// ✅ Good: Stable key even if property name changes
enum class Features(override val key: String) : BooleanFeature<...> {
    DARK_MODE("dark_mode")  // Key stays "dark_mode" even if renamed
}

// ⚠️  FeatureContainer: Property name IS the key
object Features : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    val DARK_MODE by boolean<Context>(default = false)
    // Key is "DARK_MODE" - renaming property breaks persistence!
}
```

If you need stable keys independent of property names, use the enum pattern or maintain a mapping.

### 9. Test All Features

Leverage `allFeatures()` for comprehensive testing:

```kotlin
@Test
fun `all features evaluate successfully`() {
    val testContext = Context(
        locale = AppLocale.EN_US,
        platform = Platform.WEB,
        appVersion = Version(1, 0, 0),
        stableId = StableId.of("12345678901234567890123456789012")
    )

    PaymentFeatures.allFeatures().forEach { feature ->
        val result = testContext.evaluateSafe(feature)
        assertTrue(result is EvaluationResult.Success,
            "Feature ${feature.key} failed evaluation")
    }
}
```

---

## Examples

### Complete FeatureContainer Example

```kotlin
/**
 * Payment processing feature flags.
 *
 * This container manages all payment-related features including
 * payment methods, fraud detection, and transaction limits.
 */
object PaymentFeatures : FeatureContainer<Taxonomy.Domain.Payments>(
    Taxonomy.Domain.Payments
) {
    // Payment method toggles
    val APPLE_PAY by boolean<Context>(default = false) {
        rule {
            platforms(Platform.IOS)
            versions(FullyBound(Version(2, 0, 0), Version(3, 0, 0)))
            rollout = Rollout.of(25.0)
        } implies true
    }

    val GOOGLE_PAY by boolean<Context>(default = false) {
        rule {
            platforms(Platform.ANDROID)
        } implies true
    }

    // Configuration
    val PAYMENT_PROVIDER by string<Context>(default = "stripe") {
        rule {
            platforms(Platform.WEB)
        } implies "braintree"
    }

    // Numeric limits
    val MAX_TRANSACTION by int<Context>(default = 10000) {
        rule {
            locales(AppLocale.EN_US)
        } implies 50000
    }

    val TRANSACTION_FEE by double<Context>(default = 0.029) {
        rule {
            platforms(Platform.WEB)
            rollout = Rollout.of(50.0)
        } implies 0.019
    }
}

// Usage
fun processPayment(context: Context, amount: Int) {
    val maxTransaction = context.evaluate(PaymentFeatures.MAX_TRANSACTION)
    require(amount <= maxTransaction) {
        "Transaction exceeds limit: $maxTransaction"
    }

    val fee = context.evaluate(PaymentFeatures.TRANSACTION_FEE)
    val total = amount + (amount * fee)

    val provider = context.evaluate(PaymentFeatures.PAYMENT_PROVIDER)
    paymentService.charge(provider, total)
}

// Validation
fun validatePaymentConfig() {
    val features = PaymentFeatures.allFeatures()
    println("Payment features configured: ${features.size}")

    features.forEach { feature ->
        println("- ${feature.key}: ${feature::class.simpleName}")
    }
}
```

### Complete Enum Pattern Example

```kotlin
// Declaration
enum class PaymentFeatures(override val key: String) :
    BooleanFeature<Context, Taxonomy.Domain.Payments> {

    APPLE_PAY("apple_pay"),
    GOOGLE_PAY("google_pay"),
    CRYPTO_PAYMENTS("crypto_payments");

    override val module = Taxonomy.Domain.Payments
}

enum class PaymentConfig(override val key: String) :
    StringFeature<Context, Taxonomy.Domain.Payments> {

    PAYMENT_PROVIDER("payment_provider");

    override val module = Taxonomy.Domain.Payments
}

enum class PaymentLimits(override val key: String) :
    IntFeature<Context, Taxonomy.Domain.Payments> {

    MAX_TRANSACTION("max_transaction");

    override val module = Taxonomy.Domain.Payments
}

// Configuration
Taxonomy.Domain.Payments.config {
    PaymentFeatures.APPLE_PAY with {
        default(false)
        rule {
            platforms(Platform.IOS)
        } implies true
    }

    PaymentConfig.PAYMENT_PROVIDER with {
        default("stripe")
        rule {
            platforms(Platform.WEB)
        } implies "braintree"
    }

    PaymentLimits.MAX_TRANSACTION with {
        default(10000)
        rule {
            locales(AppLocale.EN_US)
        } implies 50000
    }
}
```

### Custom Context Example

```kotlin
// Custom context with additional fields
data class EnterpriseContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    val organizationId: String,
    val subscriptionTier: SubscriptionTier,
    val seatCount: Int,
    val customDomain: String?
) : Context

enum class SubscriptionTier {
    STARTER, PROFESSIONAL, ENTERPRISE
}

// Features requiring custom context
object EnterpriseFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    val ADVANCED_ANALYTICS by boolean<EnterpriseContext>(default = false) {
        rule {
            custom { ctx ->
                ctx.subscriptionTier == SubscriptionTier.ENTERPRISE &&
                ctx.seatCount >= 50
            }
        } implies true
    }

    val CUSTOM_BRANDING by boolean<EnterpriseContext>(default = false) {
        rule {
            custom { ctx -> ctx.customDomain != null }
        } implies true
    }

    val API_RATE_LIMIT by int<EnterpriseContext>(default = 100) {
        rule {
            custom { ctx -> ctx.subscriptionTier == SubscriptionTier.ENTERPRISE }
        } implies 10000

        rule {
            custom { ctx -> ctx.subscriptionTier == SubscriptionTier.PROFESSIONAL }
        } implies 1000
    }
}

// Usage
fun generateReport(ctx: EnterpriseContext) {
    if (ctx.evaluate(EnterpriseFeatures.ADVANCED_ANALYTICS)) {
        analyticsService.generateAdvancedReport(ctx.organizationId)
    } else {
        analyticsService.generateBasicReport(ctx.organizationId)
    }
}
```

---

## Next Steps

- **[Configuration DSL](Configuration.md)**: Learn how to configure features with the DSL
- **[Rule Evaluation](Rules.md)**: Understand targeting and rule matching
- **[Context](Context.md)**: Deep dive into evaluation contexts
- **[Overview](index.md)**: Back to API overview
