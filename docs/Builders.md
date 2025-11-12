# Builder DSL

Konditional provides a type-safe, expressive DSL for configuring feature flags. The DSL uses Kotlin's receiver types and
inline functions to create a fluent configuration interface while maintaining compile-time safety.

## Overview

The configuration DSL consists of three scope levels:

1. **ConfigScope**: Top-level configuration block
2. **FlagScope**: Individual flag configuration
3. **RuleScope**: Rule definition within a flag

```kotlin
config {  // ConfigScope
    MyFeature.FLAG with {  // FlagScope
        default(value)
        rule {  // RuleScope
            platforms(Platform.IOS)
        }.implies(value)
    }
}
```

## ConfigScope

The outermost scope for defining flag configurations.

### config Function

Entry point for flag configuration:

```kotlin
config(registry: FlagRegistry = FlagRegistry) {
    // Flag configurations
}
```

**Parameters:**

- `registry`: Target registry (defaults to singleton)

**Usage:**

```kotlin
// Configure singleton registry
config {
    MyFeature.FLAG_A with { /* ... */ }
    MyFeature.FLAG_B with { /* ... */ }
}

// Configure custom registry
val testRegistry = FlagRegistry.create()
config(testRegistry) {
    MyFeature.FLAG_A with { /* ... */ }
}
```

### Feature Configuration

Use the `with` infix function to configure features:

```kotlin
config {
    MyFeature.DARK_MODE with {
        default(false)
        // ... rules
    }

    MyFeature.API_ENDPOINT with {
        default("https://api.prod.example.com")
        // ... rules
    }
}
```

## FlagScope

Scope for configuring individual flags.

### default()

Sets the default value returned when no rules match:

```kotlin
MyFeature.BOOLEAN_FLAG with {
    default(false)  // Boolean
}

MyFeature.STRING_FLAG with {
    default("production")  // String
}

MyFeature.INT_FLAG with {
    default(42)  // Int
}

MyFeature.CONFIG_FLAG with {
    default(Config(url = "https://prod.example.com", timeout = 30))  // Complex type
}
```

**Note:** The default value type must match the feature's declared type.

### salt()

Sets the hash salt for rollout bucketing:

```kotlin
MyFeature.EXPERIMENT with {
    default(false)
    salt("v1")  // Initial experiment

    rule {
        rollout = Rollout.of(50.0)
    }.implies(true)
}
```

Changing the salt redistributes users across rollout buckets.

### rule()

Defines a targeting rule:

```kotlin
MyFeature.FLAG with {
    default(false)

    rule {
        // ... targeting criteria
    }.implies(true)
}
```

Returns a `Rule` object that must be associated with a value using `implies`.

### implies

Associates a rule with its value:

```kotlin
rule {
    platforms(Platform.IOS)
}.implies(true)  // When rule matches, return true
```

## RuleScope

Scope for defining rule targeting criteria.

### platforms()

Specify target platforms:

```kotlin
rule {
    platforms(Platform.IOS, Platform.ANDROID)
}.implies(value)

rule {
    platforms(Platform.WEB)
}.implies(webValue)
```

Empty platforms = match all.

### locales()

Specify target locales:

```kotlin
rule {
    locales(AppLocale.EN_US, AppLocale.EN_CA)
}.implies(value)

rule {
    locales(AppLocale.FR_FR)
}.implies(frenchValue)
```

Empty locales = match all.

### versions()

Specify version range using VersionRangeScope:

```kotlin
rule {
    versions {
        min(2, 0, 0)  // Minimum version
        max(3, 0, 0)  // Maximum version
    }
}.implies(value)
```

### rollout

Set rollout percentage:

```kotlin
rule {
    platforms(Platform.IOS)
    rollout = Rollout.of(25.0)  // 25% rollout
}.implies(value)
```

Values: 0.0 to 100.0 (use `Rollout.MAX` for 100.0)

### extension()

Add custom evaluation logic:

```kotlin
rule {
    extension {
        object : Evaluable<EnterpriseContext>() {
            override fun matches(context: EnterpriseContext): Boolean =
                context.subscriptionTier == SubscriptionTier.ENTERPRISE

            override fun specificity(): Int = 1
        }
    }
}.implies(value)
```

### note()

Add documentation to rules:

```kotlin
rule {
    platforms(Platform.IOS)
    rollout = Rollout.of(10.0)
    note("Initial iOS canary deployment")
}.implies(value)
```

## VersionRangeScope

Scope for defining version constraints.

### min()

Set minimum version (inclusive):

```kotlin
versions {
    min(2, 0, 0)  // >= 2.0.0
}

versions {
    min(1, 5, 3)  // >= 1.5.3
}
```

### max()

Set maximum version (inclusive):

```kotlin
versions {
    max(3, 0, 0)  // <= 3.0.0
}

versions {
    max(2, 9, 99)  // <= 2.9.99
}
```

### Combined

Define version range:

```kotlin
versions {
    min(2, 0, 0)  // >= 2.0.0
    max(3, 0, 0)  // <= 3.0.0
}

// Exact version
versions {
    min(2, 1, 5)
    max(2, 1, 5)  // Exactly 2.1.5
}
```

## Complete Examples

### Simple Boolean Flag

```kotlin
config {
    MyFeatures.DARK_MODE with {
        default(false)

        rule {
            platforms(Platform.IOS, Platform.ANDROID)
        }.implies(true)
    }
}
```

### Multi-Rule Configuration

```kotlin
config {
    MyFeatures.THEME with {
        default("light")
        salt("v2")

        // Highest specificity: platform + locale + version
        rule {
            platforms(Platform.IOS)
            locales(AppLocale.EN_US)
            versions {
                min(2, 0, 0)
            }
            rollout = Rollout.of(50.0)
            note("iOS US users on v2+, 50% rollout")
        }.implies("dark")

        // Medium specificity: platform + locale
        rule {
            platforms(Platform.IOS)
            locales(AppLocale.EN_US)
            note("All iOS US users not in above bucket")
        }.implies("auto")

        // Low specificity: platform only
        rule {
            platforms(Platform.IOS)
        }.implies("light-ios")
    }
}
```

### Complex Type Flag

```kotlin
data class ApiConfig(
    val baseUrl: String,
    val timeout: Int,
    val retryEnabled: Boolean
)

config {
    MyFeatures.API_CONFIG with {
        default(ApiConfig(
            baseUrl = "https://api.prod.example.com",
            timeout = 30,
            retryEnabled = true
        ))

        rule {
            platforms(Platform.WEB)
            rollout = Rollout.of(25.0)
            note("Staging API for 25% of web users")
        }.implies(ApiConfig(
            baseUrl = "https://api.staging.example.com",
            timeout = 60,
            retryEnabled = false
        ))

        rule {
            locales(AppLocale.EN_US)
        }.implies(ApiConfig(
            baseUrl = "https://api-us.prod.example.com",
            timeout = 30,
            retryEnabled = true
        ))
    }
}
```

### Enterprise Context Flag

```kotlin
config {
    EnterpriseFeatures.ADVANCED_ANALYTICS with {
        default(false)

        // Enterprise tier customers
        rule {
            extension {
                object : Evaluable<EnterpriseContext>() {
                    override fun matches(context: EnterpriseContext): Boolean =
                        context.subscriptionTier == SubscriptionTier.ENTERPRISE

                    override fun specificity(): Int = 1
                }
            }
            rollout = Rollout.MAX
            note("Full rollout for enterprise customers")
        }.implies(true)

        // Professional tier with admin role
        rule {
            extension {
                object : Evaluable<EnterpriseContext>() {
                    override fun matches(context: EnterpriseContext): Boolean =
                        context.subscriptionTier == SubscriptionTier.PROFESSIONAL &&
                        context.userRole == UserRole.ADMIN

                    override fun specificity(): Int = 2
                }
            }
            rollout = Rollout.of(50.0)
            note("50% rollout for professional admins")
        }.implies(true)
    }
}
```

## buildSnapshot()

Create configurations without loading them into a registry:

```kotlin
val snapshot = buildSnapshot {
    MyFeatures.FLAG_A with {
        default(false)
    }

    MyFeatures.FLAG_B with {
        default(true)
    }
}

// Use snapshot later
registry.load(snapshot)

// Or serialize it
val json = SnapshotSerializer.default.serialize(snapshot)
```

**Use cases:**

- Testing configurations
- Building configurations programmatically
- Serializing configurations
- External configuration management

## Type Safety

The DSL provides compile-time type safety:

```kotlin
config {
    // Type-safe: Boolean feature expects Boolean values
    MyBooleanFeature.FLAG with {
        default(false)
        rule { platforms(Platform.IOS) }.implies(true)  // OK
        // rule { platforms(Platform.IOS) }.implies("true")  // Compile error!
    }

    // Type-safe: String feature expects String values
    MyStringFeature.API_URL with {
        default("https://prod.example.com")
        rule { platforms(Platform.WEB) }.implies("https://staging.example.com")  // OK
        // rule { platforms(Platform.WEB) }.implies(true)  // Compile error!
    }
}
```

## DSL Markers

The DSL uses `@FeatureFlagDsl` annotation to prevent accidental nesting:

```kotlin
@FeatureFlagDsl
interface ConfigScope { /* ... */ }

@FeatureFlagDsl
interface FlagScope<S, T, C, M> { /* ... */ }

@FeatureFlagDsl
interface RuleScope<C> { /* ... */ }
```

This prevents invalid constructions like:

```kotlin
config {
    MyFeature.FLAG with {
        rule {
            rule {  // Compile error: Can't nest rule inside rule
                // ...
            }
        }
    }
}
```

## Best Practices

### Separate Configuration Files

Organize configurations by feature area:

```kotlin
// UIConfig.kt
object UIConfig {
    fun configure() = config {
        UIFeatures.DARK_MODE with { /* ... */ }
        UIFeatures.ANIMATIONS with { /* ... */ }
    }
}

// ApiConfig.kt
object ApiConfig {
    fun configure() = config {
        ApiFeatures.ENDPOINT with { /* ... */ }
        ApiFeatures.TIMEOUT with { /* ... */ }
    }
}

// Initialize all
fun initializeFeatureFlags() {
    UIConfig.configure()
    ApiConfig.configure()
}
```

### Use Named Values for Clarity

```kotlin
config {
    MyFeatures.API_CONFIG with {
        val prodConfig = ApiConfig(
            baseUrl = "https://api.prod.example.com",
            timeout = 30
        )

        val stagingConfig = ApiConfig(
            baseUrl = "https://api.staging.example.com",
            timeout = 60
        )

        default(prodConfig)

        rule {
            platforms(Platform.WEB)
        }.implies(stagingConfig)
    }
}
```

### Extract Complex Extensions

```kotlin
class EnterpriseCustomerEvaluable : Evaluable<EnterpriseContext>() {
    override fun matches(context: EnterpriseContext): Boolean =
        context.subscriptionTier in setOf(
            SubscriptionTier.PROFESSIONAL,
            SubscriptionTier.ENTERPRISE
        )

    override fun specificity(): Int = 1
}

config {
    MyFeatures.PREMIUM_FEATURE with {
        default(false)

        rule {
            extension { EnterpriseCustomerEvaluable() }
        }.implies(true)
    }
}
```

### Document Complex Configurations

```kotlin
config {
    MyFeatures.EXPERIMENT_A with {
        default(false)
        salt("experiment_a_v2")  // Changed salt for fresh distribution

        rule {
            platforms(Platform.IOS)
            versions {
                min(2, 5, 0)  // Requires new API features
            }
            rollout = Rollout.of(20.0)
            note("""
                Phase 1 rollout of Experiment A.
                Target: iOS users on v2.5+
                Tracking: analytics_experiment_a_v2
                Owner: product-team@example.com
                Duration: 2024-Q1
            """.trimIndent())
        }.implies(true)
    }
}
```

## Next Steps

- **[Rules](Rules.md)**: Understand rule evaluation
- **[Flags](Flags.md)**: Learn about feature patterns
- **[Overview](index.md)**: Back to API overview
