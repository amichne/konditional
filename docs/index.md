# Konditional

Type-safe, deterministic feature flags for Kotlin.

## Overview

Konditional is a type-safe feature flag library that eliminates runtime errors through compile-time guarantees. Define flags with strong typing, evaluate them deterministically, and organize them by domain using the Taxonomy system.

**Core Principles:**

- **Type Safety First**: Generic type parameters eliminate runtime type errors
- **Deterministic**: Same inputs always produce same outputs
- **Zero Dependencies**: Pure Kotlin with Moshi for JSON serialization only
- **Thread-Safe**: Lock-free reads with atomic updates

## Quick Example

```kotlin
import io.amichne.konditional.core.features.FeatureContainer
import io.amichne.konditional.core.Taxonomy
import io.amichne.konditional.context.*

// Define features
object AppFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    val DARK_MODE by boolean(default = false) {
        rule {
            platforms(Platform.IOS)
            rollout { 50.0 }
        } implies true
    }
}

// Evaluate
val context = Context(
    locale = AppLocale.EN_US,
    platform = Platform.IOS,
    appVersion = Version.parse("2.1.0"),
    stableId = StableId.of("user-123")
)

val enabled: Boolean = context.evaluateOrDefault(AppFeatures.DARK_MODE, default = false)
```

## Core Concepts

### Features

Features are type-safe flag definitions. Use `FeatureContainer` delegation for the most ergonomic API:

```kotlin
object MyFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    val BOOLEAN_FLAG by boolean(default = false)
    val STRING_FLAG by string(default = "production")
    val INT_FLAG by int(default = 42)
    val DOUBLE_FLAG by double(default = 3.14)
}
```

The delegation pattern provides:

- Property access instead of method calls
- Type inference from default values
- Automatic registration with taxonomy
- Inline rule configuration

See **[Features](Features.md)** for enum-based patterns and custom contexts.

### Context

Context provides the evaluation environment. All evaluations require four standard fields:

```kotlin
data class Context(
    val locale: AppLocale,       // User's locale (EN_US, FR_FR, etc.)
    val platform: Platform,      // Platform (IOS, ANDROID, WEB)
    val appVersion: Version,     // Semantic version (2.1.0)
    val stableId: StableId       // Stable user ID for bucketing
)
```

Extend Context with custom fields for business logic:

```kotlin
data class EnterpriseContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    val subscriptionTier: SubscriptionTier,  // Custom field
    val organizationId: String                // Custom field
) : Context
```

See **[Context](Context.md)** for custom contexts and polymorphism.

### Rules

Rules define targeting criteria. All criteria must match for a rule to apply:

```kotlin
rule {
    platforms(Platform.IOS, Platform.ANDROID)  // Must be mobile
    locales(AppLocale.EN_US)                   // AND English US
    versions {
        min(2, 0, 0)                           // AND version >= 2.0.0
    }
    rollout { 50.0 }                           // AND in 50% bucket
} implies true
```

Rules are automatically sorted by specificity (most specific first):

```kotlin
// Specificity = 2 (platform + locale) - evaluated first
rule {
    platforms(Platform.IOS)
    locales(AppLocale.EN_US)
} implies "specific-value"

// Specificity = 1 (platform only) - evaluated second
rule {
    platforms(Platform.IOS)
} implies "general-value"
```

See **[Rules](Rules.md)** for advanced targeting and custom evaluables.

### Taxonomy

Taxonomy provides isolation between feature domains:

```kotlin
// Global taxonomy for shared features
object GlobalFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    val MAINTENANCE_MODE by boolean(default = false)
}

// Domain-specific taxonomies
object AuthFeatures : FeatureContainer<Taxonomy.Domain.Authentication>(
    Taxonomy.Domain.Authentication
) {
    val SOCIAL_LOGIN by boolean(default = false)
}

object PaymentFeatures : FeatureContainer<Taxonomy.Domain.Payments>(
    Taxonomy.Domain.Payments
) {
    val APPLE_PAY by boolean(default = false)
}
```

Benefits:

- **Compile-time isolation**: Features type-bound to taxonomy
- **Runtime isolation**: Each taxonomy has separate registry
- **Organization**: Clear ownership boundaries

See **[Registry](Registry.md)** for taxonomy management and registry operations.

## Evaluation

### Evaluation Methods

Choose based on your error handling needs:

```kotlin
// Safe: Returns EvaluationResult<T>
val result: EvaluationResult<Boolean> = context.evaluateSafe(MyFeatures.FLAG)
when (result) {
    is EvaluationResult.Success -> use(result.value)
    is EvaluationResult.FlagNotFound -> logWarning("Flag not found")
    is EvaluationResult.EvaluationError -> logError("Evaluation failed", result.error)
}

// Convenient: Returns null on failure
val value: Boolean? = context.evaluateOrNull(MyFeatures.FLAG)

// Default: Returns default value on failure
val value: Boolean = context.evaluateOrDefault(MyFeatures.FLAG, default = false)

// Unsafe: Throws exception on failure (use sparingly)
val value: Boolean = context.evaluateOrThrow(MyFeatures.FLAG)
```

See **[Evaluation](Evaluation.md)** for evaluation flow and specificity ordering.

See **[Results](Results.md)** for EvaluationResult and ParseResult error handling patterns.

### Deterministic Bucketing

Rollout bucketing is deterministic and independent per flag:

```kotlin
// SHA-256 based bucketing
fun bucket(flagKey: String, stableId: StableId, salt: String): Int {
    val hash = SHA256("$salt:$flagKey:${stableId.id}")
    return hash.take(4).toInt() % 10_000  // 0-9999 range (0.01% granularity)
}
```

Properties:

- **Deterministic**: Same user always gets same bucket
- **Independent**: Each flag has separate bucketing space
- **Platform-stable**: Consistent across JVM/Android/iOS/Web
- **Redistributable**: Change salt to reassign buckets

## Configuration

### Inline Configuration

Configure rules directly in the delegation:

```kotlin
object MyFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    val EXPERIMENT by boolean(default = false) {
        // Salt affects bucketing (change to redistribute)
        salt("v2")

        // Rule with multiple criteria
        rule {
            platforms(Platform.IOS)
            locales(AppLocale.EN_US, AppLocale.EN_CA)
            versions {
                min(2, 0, 0)
                max(3, 0, 0)
            }
            rollout { 25.0 }
            note("iOS English speakers, v2.x, 25% rollout")
        } implies true

        // Fallback rule for all iOS users
        rule {
            platforms(Platform.IOS)
        } implies false
    }
}
```

See **[Configuration](Configuration.md)** for complete DSL reference.

## Serialization

Export and import configurations as JSON:

```kotlin
// Serialize current configuration
val json = SnapshotSerializer.serialize(Taxonomy.Global.konfig())
File("flags.json").writeText(json)

// Deserialize and load
val json = File("flags.json").readText()
when (val result = SnapshotSerializer.fromJson(json)) {
    is ParseResult.Success -> Taxonomy.Global.load(result.value)
    is ParseResult.Failure -> logError("Parse failed: ${result.error}")
}

// Apply incremental patch
when (val result = SnapshotSerializer.applyPatchJson(currentKonfig, patchJson)) {
    is ParseResult.Success -> Taxonomy.Global.load(result.value)
    is ParseResult.Failure -> logError("Patch failed: ${result.error}")
}
```

See **[Serialization](Serialization.md)** for JSON format, remote configuration, and database persistence.

## Value Types

Konditional supports four primitive types and data classes:

```kotlin
object MyFlags : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    // Primitives
    val BOOLEAN_FLAG by boolean(default = false)
    val STRING_FLAG by string(default = "value")
    val INT_FLAG by int(default = 42)
    val DOUBLE_FLAG by double(default = 3.14)
}
```

For complex types, use data classes (automatically serialized as JSON):

```kotlin
data class ThemeConfig(
    val primaryColor: String,
    val fontSize: Int,
    val darkMode: Boolean
)

object MyThemes : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    // Data classes work automatically
    val APP_THEME by jsonObject(
        default = ThemeConfig(
            primaryColor = "#FFFFFF",
            fontSize = 14,
            darkMode = false
        )
    )
}
```

## Thread Safety

Konditional is designed for concurrent access without locks:

**Lock-Free Reads**: Flag evaluation requires no synchronization. Multiple threads can evaluate flags concurrently without contention.

**Atomic Updates**: Registry updates use `AtomicReference` for atomic snapshots. Readers see either old or new configuration, never partial updates.

**Immutable Data**: `Konfig` and `FlagDefinition` are immutable. Once created, they cannot be modified.

**Independent Evaluations**: Each evaluation is stateless and independent. No shared mutable state during evaluation.

## Type Safety Guarantees

| Guarantee | How Konditional Enforces It |
|-----------|----------------------------|
| **Non-null returns** | Default value required at definition time |
| **Correct type** | Generic type parameters enforce value type |
| **Correct context** | Context type parameter enforced by compiler |
| **Valid rollout** | Rollout.of() validates 0.0-100.0 range |
| **Valid version** | Version.parse() validates semantic versioning |
| **Valid stable ID** | StableId.of() validates hexadecimal format |
| **Supported types** | Only Boolean, String, Int, Double, and data classes allowed |

**Core Principle**: If it compiles, the types are correct. No runtime type errors.

## Documentation Guide

**Getting Started:**

1. **[Quick Start](QuickStart.md)** - Get running in 5 minutes

**Core Concepts:**

2. **[Features](Features.md)** - Feature definition patterns
3. **[Context](Context.md)** - Evaluation contexts
4. **[Evaluation](Evaluation.md)** - Flag evaluation mechanics

**Configuration:**

5. **[Configuration](Configuration.md)** - DSL reference
6. **[Rules](Rules.md)** - Targeting and rollouts

**Advanced:**

7. **[Serialization](Serialization.md)** - JSON import/export
8. **[Registry](Registry.md)** - Taxonomy and registry management
9. **[Results](Results.md)** - Error handling patterns

## Common Use Cases

### Feature Flags

Gradual rollout of new features:

```kotlin
val NEW_CHECKOUT by boolean(default = false) {
    rule {
        platforms(Platform.ANDROID)
        rollout { 10.0 }  // Start with 10%
    } implies true
}
```

### Configuration Management

Environment-specific configuration:

```kotlin
val API_ENDPOINT by string(default = "https://api.prod.example.com") {
    rule {
        platforms(Platform.WEB)
    } implies "https://api-staging.example.com"
}
```

### A/B Testing

Split traffic for experiments:

```kotlin
val RECOMMENDATION_ALGORITHM by string(default = "collaborative") {
    rule {
        rollout { 50.0 }  // 50/50 split
    } implies "content-based"
}
```

### Kill Switches

Emergency feature disable:

```kotlin
val PAYMENT_PROCESSING by boolean(default = true) {
    // Can be updated remotely via JSON to disable instantly
}
```

## Performance

**Evaluation Complexity**: O(n) where n = number of rules per flag (typically < 10)

**Memory**: Zero allocations during evaluation. All data structures are pre-allocated and immutable.

**Concurrency**: Lock-free reads. No thread contention during evaluation.

**Bucketing**: O(1) SHA-256 hash computation per rollout evaluation.

## Best Practices

**Use FeatureContainer delegation**: Simplest and most ergonomic API for most use cases.

**Organize by domain**: Use Taxonomy to separate features by team or business domain.

**Start with small rollouts**: Begin with 10% rollout, increase gradually after monitoring.

**Document with note()**: Add context to rules explaining why they exist.

**Use evaluateOrDefault**: Provides failsafe behavior without exception handling.

**Version your salts**: Track salt changes to understand bucketing redistribution.

**Test with custom contexts**: Create test contexts with specific values to verify rule logic.

**Validate after deserialization**: Check rollout ranges and required flags after loading JSON.

## Migration Path

Migrating from string-based configuration systems:

1. **Inventory existing flags**: Document all current flags and their types
2. **Define features**: Create FeatureContainer with current defaults
3. **Run in parallel**: Evaluate both systems, compare results
4. **Migrate incrementally**: Move flags one by one
5. **Deprecate old system**: Remove string-based system after full migration

## Next Steps

**New to Konditional?** Start with **[Quick Start](QuickStart.md)**

**Need to define features?** See **[Features](Features.md)**

**Building targeting rules?** See **[Rules](Rules.md)**

**Loading remote config?** See **[Serialization](Serialization.md)**

**Organizing by team?** See **[Registry](Registry.md)**
