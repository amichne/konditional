# Quick Start Guide

Get your first type-safe feature flag running in 5 minutes.

## Installation

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.amichne:konditional:0.0.1")
}
```

## Your First Feature Flag

### Step 1: Define Your Features

Use the `FeatureContainer` delegation pattern for the simplest approach:

```kotlin
import io.amichne.konditional.core.features.FeatureContainer
import io.amichne.konditional.core.Taxonomy

object AppFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    val DARK_MODE by boolean(default = false)
}
```

**What this gives you:**

- Property access: `AppFeatures.DARK_MODE`
- Type-safe: Always returns `Boolean`, never null
- Compile-time validation: Typos become compile errors

### Step 2: Configure Rules (Optional)

Add targeting rules within the delegation:

```kotlin
object AppFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    val DARK_MODE by boolean(default = false) {
        rule {
            platforms(Platform.IOS)
            rollout { 50.0 }
        } implies true
    }
}
```

### Step 3: Evaluate the Flag

Create a context and evaluate:

```kotlin
import io.amichne.konditional.context.*

// Create evaluation context
val context = Context(
    locale = AppLocale.EN_US,
    platform = Platform.IOS,
    appVersion = Version.parse("2.1.0"),
    stableId = StableId.of("user-123")
)

// Evaluate the flag
val isDarkMode = context.evaluateOrDefault(AppFeatures.DARK_MODE, default = false)

// Use it
if (isDarkMode) {
    applyDarkTheme()
}
```

**What you get:**

- Non-null result guaranteed (via `evaluateOrDefault`)
- Type-safe: `isDarkMode` is always `Boolean`
- Deterministic: Same context always returns same value

## Common Patterns

### Multiple Flag Types

Define different value types in the same container:

```kotlin
object AppConfig : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    val DARK_MODE by boolean(default = false)
    val API_ENDPOINT by string(default = "https://api.prod.example.com")
    val MAX_RETRIES by int(default = 3)
    val TIMEOUT_SECONDS by double(default = 30.0)
}
```

### Platform-Specific Values

Configure different values for different platforms:

```kotlin
object AppFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    val API_ENDPOINT by string(default = "https://api.example.com") {
        rule {
            platforms(Platform.IOS)
        } implies "https://api-ios.example.com"

        rule {
            platforms(Platform.ANDROID)
        } implies "https://api-android.example.com"
    }
}
```

### Gradual Rollout

Deploy features gradually using rollout percentages:

```kotlin
object AppFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    val NEW_CHECKOUT by boolean(default = false) {
        rule {
            platforms(Platform.ANDROID)
            rollout { 25.0 }  // 25% of Android users
        } implies true
    }
}
```

**Rollout characteristics:**

- Deterministic: Same user always gets same result
- Independent: Each flag buckets users independently
- Stable: SHA-256 based bucketing

### Combining Criteria

Rules support multiple targeting criteria (all must match):

```kotlin
object PremiumFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    val ADVANCED_ANALYTICS by boolean(default = false) {
        rule {
            platforms(Platform.IOS)
            locales(AppLocale.EN_US)
            versions {
                min(2, 0, 0)  // Version 2.0.0 or higher
            }
            rollout { 50.0 }
        } implies true
    }
}
```

## Evaluation Methods

Konditional provides multiple evaluation methods for different error handling needs:

```kotlin
// Safe evaluation with Result type
val result: EvaluationResult<Boolean> = context.evaluateSafe(AppFeatures.DARK_MODE)
when (result) {
    is EvaluationResult.Success -> println("Value: ${result.value}")
    is EvaluationResult.FlagNotFound -> println("Flag not found")
    is EvaluationResult.EvaluationError -> println("Error: ${result.error}")
}

// Null on failure
val value: Boolean? = context.evaluateOrNull(AppFeatures.DARK_MODE)

// Default on failure
val value: Boolean = context.evaluateOrDefault(AppFeatures.DARK_MODE, default = false)

// Throw exception on failure (use sparingly)
val value: Boolean = context.evaluateOrThrow(AppFeatures.DARK_MODE)
```

## Organizing Features by Domain

Use Taxonomy to organize features by team or domain:

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
}
```

Benefits:

- Isolation: Features don't collide across taxonomies
- Organization: Clear ownership boundaries
- Type safety: Compile-time taxonomy enforcement

## Next Steps

Now that you have basic flags running, explore:

- **[Overview](index.md)**: Complete API overview and core concepts
- **[Features](Features.md)**: All feature definition patterns
- **[Context](Context.md)**: Evaluation contexts and custom extensions
- **[Rules](Rules.md)**: Advanced targeting and rollouts
- **[Evaluation](Evaluation.md)**: Deep dive into flag evaluation
- **[Configuration](Configuration.md)**: Complete DSL reference
- **[Results](Results.md)**: Error handling with EvaluationResult
- **[Serialization](Serialization.md)**: Export/import configurations as JSON
- **[Registry](Registry.md)**: Taxonomy and registry management

## Key Takeaways

- **FeatureContainer delegation**: Simplest way to define features
- **Context required**: All evaluations need locale, platform, version, stableId
- **Multiple evaluation methods**: Choose based on error handling needs
- **Type safety**: If it compiles, the types are correct
- **Deterministic**: Same inputs always produce same outputs
