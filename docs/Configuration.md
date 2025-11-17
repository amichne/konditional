# Configuration

Konditional provides a type-safe DSL for configuring feature flags using the `FeatureContainer` delegation pattern. This guide covers the complete configuration API from a user perspective.

## Overview

Configuration in Konditional happens through property delegation within a `FeatureContainer`. Features are automatically registered and configured when you access them for the first time.

**Two main approaches:**

1. **FeatureContainer delegation** (recommended): Inline configuration with automatic registration
2. **Manual configuration**: Explicit configuration using the `update()` method (advanced)

This guide focuses on the FeatureContainer delegation approach, which provides the most ergonomic API.

## FeatureContainer: Organization and Auto-Registration

`FeatureContainer` is an abstract base class for organizing related feature flags with automatic registration.

```kotlin
import io.amichne.konditional.core.features.FeatureContainer
import io.amichne.konditional.core.Taxonomy

object AppFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    val DARK_MODE by boolean(default = false)
    val API_ENDPOINT by string(default = "https://api.example.com")
    val MAX_RETRIES by int(default = 3)
    val TIMEOUT_SECONDS by double(default = 30.0)
}
```

**Benefits:**

- **Automatic registration**: Features register themselves on first access
- **Type inference**: Default value determines the feature type
- **Single taxonomy declaration**: No need to repeat taxonomy on every feature
- **Mixed types**: Combine Boolean, String, Int, and Double features in one container
- **Complete enumeration**: `allFeatures()` provides runtime access to all features

## Delegation Methods

FeatureContainer provides four delegation methods for different value types:

### boolean()

Creates a Boolean feature with optional configuration:

```kotlin
object MyFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    val DARK_MODE by boolean(default = false) {
        rule {
            platforms(Platform.IOS)
            rollout { 50.0 }
        } implies true
    }
}
```

**Signature:**
```kotlin
protected fun <C : Context> boolean(
    default: Boolean,
    flagScope: FlagScope<BooleanEncodeable, Boolean, C, M>.() -> Unit = {}
): ReadOnlyProperty<FeatureContainer<M>, BooleanFeature<C, M>>
```

**Parameters:**
- `default`: Default value (required)
- `flagScope`: DSL configuration block (optional)

### string()

Creates a String feature with optional configuration:

```kotlin
object MyFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    val API_ENDPOINT by string(default = "https://api.prod.example.com") {
        rule {
            platforms(Platform.ANDROID)
        } implies "https://api-android.example.com"
    }
}
```

**Signature:**
```kotlin
protected fun <C : Context> string(
    default: String,
    stringScope: FlagScope<StringEncodeable, String, C, M>.() -> Unit = {}
): ReadOnlyProperty<FeatureContainer<M>, StringFeature<C, M>>
```

### int()

Creates an Int feature with optional configuration:

```kotlin
object MyFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    val MAX_RETRY_COUNT by int(default = 3) {
        rule {
            platforms(Platform.IOS)
        } implies 5
    }
}
```

**Signature:**
```kotlin
protected fun <C : Context> int(
    default: Int,
    integerScope: FlagScope<IntEncodeable, Int, C, M>.() -> Unit = {}
): ReadOnlyProperty<FeatureContainer<M>, IntFeature<C, M>>
```

### double()

Creates a Double feature with optional configuration:

```kotlin
object MyFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    val TRANSACTION_FEE by double(default = 0.029) {
        rule {
            platforms(Platform.WEB)
        } implies 0.019
    }
}
```

**Signature:**
```kotlin
protected fun <C : Context> double(
    default: Double,
    decimalScope: FlagScope<DecimalEncodeable, Double, C, M>.() -> Unit = {}
): ReadOnlyProperty<FeatureContainer<M>, DoubleFeature<C, M>>
```

## FlagScope DSL

The `FlagScope` interface defines the configuration API for individual feature flags.

### default()

Sets the default value returned when no rules match:

```kotlin
val MY_FLAG by boolean(default = false) {
    default(false)  // Explicit (redundant in this case)
}
```

**Note:** When using delegation, the `default` parameter is automatically applied. You rarely need to call `default()` explicitly within the configuration block.

### salt()

Sets the hash salt for rollout bucketing:

```kotlin
val EXPERIMENT by boolean(default = false) {
    salt("v1")  // Change to "v2" to redistribute users

    rule {
        rollout { 50.0 }
    } implies true
}
```

**Use cases:**
- **Initial experiments**: Start with "v1"
- **Re-randomization**: Change to "v2", "v3", etc. to redistribute users
- **Independent bucketing**: Different salts create independent rollout buckets

**Important:** Changing the salt redistributes all users across rollout buckets for that flag.

### rule()

Defines a targeting rule with a DSL configuration block:

```kotlin
val MY_FLAG by boolean(default = false) {
    rule {
        platforms(Platform.IOS, Platform.ANDROID)
        locales(AppLocale.EN_US)
        rollout { 50.0 }
    } implies true
}
```

**Returns:** A `Rule<C>` object that must be associated with a value using `implies`

**Signature:**
```kotlin
fun rule(build: RuleScope<C>.() -> Unit): Rule<C>
```

### implies (infix)

Associates a rule with its return value:

```kotlin
rule {
    platforms(Platform.IOS)
} implies true
```

**Type safety:** The value type must match the feature's declared type.

```kotlin
val BOOLEAN_FLAG by boolean(default = false) {
    rule { platforms(Platform.IOS) } implies true    // ✓ Valid
    rule { platforms(Platform.WEB) } implies "true"  // ✗ Compile error
}
```

**Signature:**
```kotlin
infix fun Rule<C>.implies(value: T)
```

## RuleScope DSL

The `RuleScope` interface defines targeting criteria for rules.

### platforms()

Specify which platforms the rule applies to:

```kotlin
rule {
    platforms(Platform.IOS, Platform.ANDROID)
} implies mobileValue
```

**Available platforms:**
- `Platform.IOS`
- `Platform.ANDROID`
- `Platform.WEB`
- `Platform.DESKTOP`
- `Platform.SERVER`

**Empty platforms:** Matches all platforms

### locales()

Specify which locales the rule applies to:

```kotlin
rule {
    locales(AppLocale.EN_US, AppLocale.EN_CA, AppLocale.EN_GB)
} implies englishValue
```

**Common locales:**
- `AppLocale.EN_US` (English - US)
- `AppLocale.EN_GB` (English - UK)
- `AppLocale.FR_FR` (French - France)
- `AppLocale.DE_DE` (German - Germany)
- `AppLocale.ES_ES` (Spanish - Spain)
- `AppLocale.JA_JP` (Japanese - Japan)
- `AppLocale.ZH_CN` (Chinese - China)

**Empty locales:** Matches all locales

### versions()

Specify version range using `VersionRangeScope`:

```kotlin
rule {
    versions {
        min(2, 0, 0)  // >= 2.0.0
        max(3, 0, 0)  // < 3.0.0
    }
} implies value
```

**Version range patterns:**

```kotlin
// Minimum only
versions { min(2, 0, 0) }  // 2.0.0 or higher

// Maximum only
versions { max(1, 9, 9) }  // 1.9.9 or lower

// Both (range)
versions {
    min(1, 5, 0)
    max(2, 0, 0)
}

// Exact version
versions {
    min(2, 1, 3)
    max(2, 1, 3)
}
```

### rollout()

Set gradual rollout percentage (0-100):

```kotlin
rule {
    platforms(Platform.IOS)
    rollout { 50.0 }  // 50% of iOS users
} implies true
```

**Common values:**

```kotlin
rollout { 0.0 }    // 0% - effectively disabled
rollout { 10.0 }   // 10% - canary/pilot
rollout { 25.0 }   // 25% - limited rollout
rollout { 50.0 }   // 50% - A/B test
rollout { 100.0 }  // 100% - full rollout
```

**Rollout characteristics:**
- **Deterministic**: Same user (by `stableId`) always gets same assignment
- **Independent**: Each flag has its own bucketing space
- **Stable**: Assignments don't change unless salt changes

### extension()

Add custom evaluation logic using `Evaluable`:

```kotlin
rule {
    extension {
        object : Evaluable<EnterpriseContext>() {
            override fun matches(context: EnterpriseContext): Boolean =
                context.subscriptionTier == SubscriptionTier.ENTERPRISE

            override fun specificity(): Int = 1
        }
    }
} implies true
```

**Use cases:**
- Business logic targeting (subscription tier, organization ID, etc.)
- Complex conditional logic
- Custom user segmentation

### note()

Add human-readable documentation to rules:

```kotlin
rule {
    platforms(Platform.IOS)
    rollout { 10.0 }
    note("iOS canary deployment - Phase 1")
} implies true
```

**Best practices:**
- Document purpose and intent
- Include tracking IDs for experiments
- Note ownership and duration
- Explain complex targeting logic

## VersionRangeScope DSL

The `VersionRangeScope` interface defines version constraint configuration.

### min()

Set minimum version (inclusive):

```kotlin
versions {
    min(2, 0, 0)  // Version 2.0.0 or higher
}
```

**Signature:**
```kotlin
fun min(major: Int, minor: Int = 0, patch: Int = 0)
```

**Examples:**
```kotlin
min(2)           // >= 2.0.0
min(2, 5)        // >= 2.5.0
min(2, 5, 3)     // >= 2.5.3
```

### max()

Set maximum version (exclusive):

```kotlin
versions {
    max(3, 0, 0)  // Below version 3.0.0
}
```

**Signature:**
```kotlin
fun max(major: Int, minor: Int = 0, patch: Int = 0)
```

**Examples:**
```kotlin
max(3)           // < 3.0.0
max(2, 9)        // < 2.9.0
max(2, 9, 99)    // < 2.9.99
```

## Complete Configuration Examples

### Simple Boolean Flag

```kotlin
object AppFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    val DARK_MODE by boolean(default = false) {
        rule {
            platforms(Platform.IOS, Platform.ANDROID)
            rollout { 50.0 }
            note("Mobile dark mode, 50% rollout")
        } implies true
    }
}
```

### Multi-Platform with Different Values

```kotlin
object AppFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    val API_ENDPOINT by string(default = "https://api.example.com") {
        rule {
            platforms(Platform.IOS)
        } implies "https://api-ios.example.com"

        rule {
            platforms(Platform.ANDROID)
        } implies "https://api-android.example.com"

        rule {
            platforms(Platform.WEB)
        } implies "https://api-web.example.com"
    }
}
```

### Version-Based Feature Rollout

```kotlin
object AppFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    val NEW_UI by boolean(default = false) {
        // Full rollout for version 3.0.0+
        rule {
            versions { min(3, 0, 0) }
        } implies true

        // 50% rollout for version 2.5.0 - 2.9.9
        rule {
            versions {
                min(2, 5, 0)
                max(3, 0, 0)
            }
            rollout { 50.0 }
        } implies true

        // 10% canary for version 2.0.0 - 2.4.9
        rule {
            versions {
                min(2, 0, 0)
                max(2, 5, 0)
            }
            rollout { 10.0 }
        } implies true
    }
}
```

### Custom Context with Business Logic

```kotlin
data class EnterpriseContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    val subscriptionTier: SubscriptionTier,
    val organizationId: String
) : Context

enum class SubscriptionTier { FREE, PROFESSIONAL, ENTERPRISE }

object PremiumFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    val ADVANCED_ANALYTICS by boolean<EnterpriseContext>(default = false) {
        // Enterprise customers: 100% rollout
        rule {
            extension {
                object : Evaluable<EnterpriseContext>() {
                    override fun matches(context: EnterpriseContext): Boolean =
                        context.subscriptionTier == SubscriptionTier.ENTERPRISE

                    override fun specificity(): Int = 1
                }
            }
            note("Full rollout for enterprise customers")
        } implies true

        // Professional customers: 50% rollout
        rule {
            extension {
                object : Evaluable<EnterpriseContext>() {
                    override fun matches(context: EnterpriseContext): Boolean =
                        context.subscriptionTier == SubscriptionTier.PROFESSIONAL

                    override fun specificity(): Int = 1
                }
            }
            rollout { 50.0 }
            note("50% rollout for professional tier")
        } implies true
    }
}
```

### Multi-Criteria Targeting

```kotlin
object AppFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    val PREMIUM_FEATURE by boolean(default = false) {
        rule {
            platforms(Platform.IOS, Platform.ANDROID)
            locales(AppLocale.EN_US, AppLocale.EN_CA)
            versions {
                min(2, 0, 0)
                max(3, 0, 0)
            }
            rollout { 25.0 }
            note("Mobile English NA users, v2.x, 25% rollout")
        } implies true
    }
}
```

### Gradual Rollout Strategy

```kotlin
object ExperimentFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    val NEW_CHECKOUT by boolean(default = false) {
        salt("v1")  // Change to re-randomize

        // Phase 1: Internal testing (use custom context)
        rule {
            extension {
                object : Evaluable<EnterpriseContext>() {
                    override fun matches(context: EnterpriseContext): Boolean =
                        context.organizationId == "internal"
                    override fun specificity(): Int = 2
                }
            }
            note("Phase 1: Internal testing")
        } implies true

        // Phase 2: 10% canary
        rule {
            rollout { 10.0 }
            note("Phase 2: 10% canary")
        } implies true

        // Later: Increase to 50%, then 100%
        // Update configuration dynamically or through config management
    }
}
```

## Type Safety in Configuration

The DSL enforces type safety at compile time:

### Value Type Checking

```kotlin
val BOOLEAN_FLAG by boolean(default = false) {
    rule { platforms(Platform.IOS) } implies true    // ✓ Valid
    rule { platforms(Platform.WEB) } implies "true"  // ✗ Type mismatch
}

val STRING_FLAG by string(default = "default") {
    rule { platforms(Platform.IOS) } implies "ios-value"  // ✓ Valid
    rule { platforms(Platform.WEB) } implies true         // ✗ Type mismatch
}
```

### Context Type Checking

```kotlin
data class CustomContext(/* ... */) : Context

val CUSTOM_FLAG by boolean<CustomContext>(default = false) {
    rule {
        extension {
            // Must use CustomContext, not base Context
            object : Evaluable<CustomContext>() {  // ✓ Correct type
                override fun matches(context: CustomContext) = true
                override fun specificity() = 1
            }
        }
    } implies true
}
```

## Configuration Patterns

### Organizing Features by Domain

Use separate containers for different domains:

```kotlin
object AuthFeatures : FeatureContainer<Taxonomy.Domain.Authentication>(
    Taxonomy.Domain.Authentication
) {
    val SOCIAL_LOGIN by boolean(default = false)
    val TWO_FACTOR_AUTH by boolean(default = true)
}

object PaymentFeatures : FeatureContainer<Taxonomy.Domain.Payments>(
    Taxonomy.Domain.Payments
) {
    val APPLE_PAY by boolean(default = false)
    val GOOGLE_PAY by boolean(default = false)
    val TRANSACTION_LIMIT by double(default = 10000.0)
}

object MessagingFeatures : FeatureContainer<Taxonomy.Domain.Messaging>(
    Taxonomy.Domain.Messaging
) {
    val PUSH_NOTIFICATIONS by boolean(default = true)
    val EMAIL_DIGEST by boolean(default = false)
}
```

**Benefits:**
- Clear ownership boundaries
- Isolation between teams
- Independent configuration management
- Type-safe taxonomy enforcement

### Extracting Complex Rules

Create reusable `Evaluable` classes:

```kotlin
class EnterpriseCustomerRule : Evaluable<EnterpriseContext>() {
    override fun matches(context: EnterpriseContext): Boolean =
        context.subscriptionTier == SubscriptionTier.ENTERPRISE

    override fun specificity(): Int = 1
}

class ProfessionalCustomerRule : Evaluable<EnterpriseContext>() {
    override fun matches(context: EnterpriseContext): Boolean =
        context.subscriptionTier == SubscriptionTier.PROFESSIONAL

    override fun specificity(): Int = 1
}

object PremiumFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    val FEATURE_A by boolean<EnterpriseContext>(default = false) {
        rule {
            extension { EnterpriseCustomerRule() }
        } implies true
    }

    val FEATURE_B by boolean<EnterpriseContext>(default = false) {
        rule {
            extension { EnterpriseCustomerRule() }
        } implies true
    }
}
```

**Benefits:**
- Reusability across multiple flags
- Easier testing
- Cleaner configuration DSL
- Type-safe composition

### Named Configuration Values

Extract complex values into named constants:

```kotlin
object ApiConfig : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    private val PROD_ENDPOINT = "https://api.prod.example.com"
    private val STAGING_ENDPOINT = "https://api.staging.example.com"
    private val DEV_ENDPOINT = "https://api.dev.example.com"

    val API_ENDPOINT by string(default = PROD_ENDPOINT) {
        rule {
            extension {
                object : Evaluable<EnterpriseContext>() {
                    override fun matches(context: EnterpriseContext) =
                        context.organizationId.startsWith("staging-")
                    override fun specificity() = 1
                }
            }
        } implies STAGING_ENDPOINT

        rule {
            extension {
                object : Evaluable<EnterpriseContext>() {
                    override fun matches(context: EnterpriseContext) =
                        context.organizationId == "internal"
                    override fun specificity() = 2
                }
            }
        } implies DEV_ENDPOINT
    }
}
```

## Exporting Configurations

### konfig() Method

Get a snapshot of the current configuration:

```kotlin
object MyFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    val FLAG_A by boolean(default = false)
    val FLAG_B by string(default = "default")
}

// Get configuration snapshot
val snapshot = Taxonomy.Global.konfig()

// Serialize to JSON
val json = SnapshotSerializer.serialize(snapshot)

// Load into another registry (testing)
testRegistry.load(snapshot)
```

**Use cases:**
- Exporting current configuration state
- Testing with isolated registries
- Configuration auditing
- External configuration management
- Snapshot comparisons

### allFeatures() Method

Enumerate all features in a container:

```kotlin
val features = MyFeatures.allFeatures()
features.forEach { feature ->
    println("Feature: ${feature.key}")
}
```

**Use cases:**
- Configuration validation
- Documentation generation
- Feature inventory auditing
- Testing all features

## DSL Reference Table

### Scope Hierarchy

| Scope                | Parent Scope      | Available Methods                                      |
|----------------------|-------------------|--------------------------------------------------------|
| FeatureContainer     | -                 | `boolean()`, `string()`, `int()`, `double()`, `allFeatures()` |
| FlagScope            | Delegation block  | `default()`, `salt()`, `rule()`, `implies`             |
| RuleScope            | `rule { }`        | `platforms()`, `locales()`, `versions()`, `rollout()`, `extension()`, `note()` |
| VersionRangeScope    | `versions { }`    | `min()`, `max()`                                       |

### FlagScope Methods

| Method                                      | Description                               | Required |
|---------------------------------------------|-------------------------------------------|----------|
| `default(value: T)`                         | Set default value                         | No*      |
| `salt(value: String)`                       | Set rollout salt                          | No       |
| `rule(build: RuleScope<C>.() -> Unit)`      | Define targeting rule                     | No       |
| `infix fun Rule<C>.implies(value: T)`       | Associate rule with value                 | Yes**    |

\* Required when not using delegation parameter
\** Required for each `rule()` call

### RuleScope Methods

| Method                                   | Description                    | Default           |
|------------------------------------------|--------------------------------|-------------------|
| `platforms(vararg ps: Platform)`         | Target specific platforms      | All platforms     |
| `locales(vararg appLocales: AppLocale)`  | Target specific locales        | All locales       |
| `versions(build: VersionRangeScope.() -> Unit)` | Target version range    | All versions      |
| `rollout(function: () -> Number)`        | Set rollout percentage         | 100.0             |
| `extension(function: () -> Evaluable<C>)` | Add custom targeting          | No extension      |
| `note(text: String)`                     | Add documentation note         | No note           |

### VersionRangeScope Methods

| Method                                      | Description                  |
|---------------------------------------------|------------------------------|
| `min(major: Int, minor: Int = 0, patch: Int = 0)` | Set minimum version (inclusive) |
| `max(major: Int, minor: Int = 0, patch: Int = 0)` | Set maximum version (exclusive) |

## Best Practices

### 1. Use FeatureContainer Delegation

Prefer delegation over manual configuration:

```kotlin
// ✓ Recommended
object MyFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    val DARK_MODE by boolean(default = false)
}

// ✗ Avoid (manual configuration)
enum class MyFeatures : BooleanFeature<Context, Taxonomy.Global> {
    DARK_MODE("dark_mode");
    override val module = Taxonomy.Global
}
```

### 2. Organize by Feature Area

Separate configurations into dedicated containers:

```kotlin
// ✓ Clear organization
object UIFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) { }
object ApiFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) { }
object ExperimentFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) { }

// ✗ Everything in one container
object AllFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    val UI_DARK_MODE by boolean(default = false)
    val API_ENDPOINT by string(default = "")
    val EXPERIMENT_A by boolean(default = false)
    // ... 100 more features
}
```

### 3. Use Named Values for Clarity

Extract complex values:

```kotlin
// ✓ Clear and reusable
private val PRODUCTION_ENDPOINT = "https://api.prod.example.com"
val API_URL by string(default = PRODUCTION_ENDPOINT)

// ✗ Magic strings
val API_URL by string(default = "https://api.prod.example.com")
```

### 4. Document Rules with note()

Add context to complex rules:

```kotlin
// ✓ Well documented
rule {
    platforms(Platform.IOS)
    rollout { 10.0 }
    note("EXP-1234: iOS canary for new checkout - Q1 2025 - Owner: payments-team")
} implies true

// ✗ No context
rule {
    platforms(Platform.IOS)
    rollout { 10.0 }
} implies true
```

### 5. Maintain Salt Hygiene

Document salt changes:

```kotlin
// ✓ Documented salt changes
val EXPERIMENT by boolean(default = false) {
    salt("v2")  // v2: Re-randomized 2025-01-15 to fix skewed distribution
    // ...
}

// ✗ Unexplained salt changes
val EXPERIMENT by boolean(default = false) {
    salt("abc123xyz")
    // ...
}
```

### 6. Extract Reusable Evaluables

Create classes for common logic:

```kotlin
// ✓ Reusable and testable
class PremiumTierRule : Evaluable<EnterpriseContext>() {
    override fun matches(context: EnterpriseContext) =
        context.subscriptionTier == SubscriptionTier.PREMIUM ||
        context.subscriptionTier == SubscriptionTier.ENTERPRISE
    override fun specificity() = 1
}

// ✗ Duplicated logic
val FEATURE_A by boolean<EnterpriseContext>(default = false) {
    rule {
        extension {
            object : Evaluable<EnterpriseContext>() {
                override fun matches(context: EnterpriseContext) =
                    context.subscriptionTier == SubscriptionTier.PREMIUM ||
                    context.subscriptionTier == SubscriptionTier.ENTERPRISE
                override fun specificity() = 1
            }
        }
    } implies true
}
```

### 7. Validate Configurations

Use `allFeatures()` for validation:

```kotlin
fun validateAllFeaturesConfigured() {
    val configuredKeys = loadConfigurationKeys()  // From external source
    val declaredKeys = MyFeatures.allFeatures().map { it.key }.toSet()
    val missing = declaredKeys - configuredKeys

    require(missing.isEmpty()) {
        "Missing configuration for features: $missing"
    }
}
```

### 8. Use Specific Rollout Values

Prefer common percentages for consistency:

```kotlin
// ✓ Standard increments
rollout { 10.0 }   // Canary
rollout { 25.0 }   // Limited
rollout { 50.0 }   // A/B test
rollout { 100.0 }  // Full rollout

// ✗ Arbitrary percentages
rollout { 17.3 }
rollout { 42.7 }
```

## Next Steps

- **[Features](Features.md)**: Feature definition patterns and type aliases
- **[Rules](Rules.md)**: Advanced targeting and rule evaluation
- **[Context](Context.md)**: Custom contexts and polymorphism
- **[Evaluation](Evaluation.md)**: Flag evaluation mechanics and result handling
- **[Serialization](Serialization.md)**: JSON export/import for configurations
- **[Registry](Registry.md)**: Taxonomy and registry management
