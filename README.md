# Konditional

**Type-safe, deterministic feature flags for Kotlin**

Konditional is a lightweight feature flag framework that puts type safety and developer experience first. Unlike traditional feature flag systems that force you into boolean flags and string-based configuration, Konditional works with your domain types and provides compile-time guarantees.

**For teams using string-based configuration:** If you're experiencing runtime errors from typos, type mismatches, or null handling in your current system, Konditional's type safety eliminates these entire classes of errors at compile time. See our [Migration Guide](docs/Migration.md) to learn how to transition safely.

## Why Konditional?

### Type Safety at Every Level
- **Any value type**: Return `Boolean`, `String`, `Int`, enums, data classes, or any custom type
- **Compile-time safety**: No runtime type errors or string-based lookups
- **Generic contexts**: Define custom evaluation contexts with your own business logic
- **Zero unchecked casts**: Type safety is maintained throughout the entire evaluation chain

### Deterministic & Reliable
- **Stable bucketing**: Users get consistent experiences across sessions using SHA-256 based hashing
- **Independent flags**: Each flag has its own bucketing space - no cross-contamination
- **Predictable rollouts**: Gradual rollouts (0-100%) with deterministic user assignment
- **Thread-safe**: Lock-free reads with atomic konfig updates

### Flexible & Extensible
- **Custom contexts**: Define your own context with organization IDs, user roles, experiments, or any domain data
- **Rule-based targeting**: Target by platform, locale, version, or extend with custom rules
- **Specificity ordering**: More specific rules automatically take precedence
- **DSL configuration**: Clean, type-safe Kotlin DSL for defining flags

### Zero Dependencies
- Pure Kotlin with no external dependencies
- No reflection or runtime code generation
- No dependency injection framework required
- Easy to integrate into any project

## Quick Example

```kotlin
// Define your flags with an enum
enum class Features(override val key: String) : Conditional<Boolean, Context> {
    NEW_CHECKOUT("new_checkout"),
    DARK_MODE("dark_mode"),
}

// Configure with a type-safe DSL
config {
    Features.NEW_CHECKOUT with {
        default(false)

        // 25% rollout for iOS users on version 2.0+
        rule {
            platforms(Platform.IOS)
            versions {
                min(2, 0)
            }
            rollout = Rollout.of(25.0)
        } implies true
    }

    Features.DARK_MODE with {
        default(false)

        // Full rollout for all platforms
        rule {
            rollout = Rollout.MAX
        } implies true
    }
}

// Evaluate with context
val context = Context(
    locale = AppLocale.EN_US,
    platform = Platform.IOS,
    appVersion = Version(2, 5, 0),
    stableId = StableId.of("user-unique-id")
)

val showNewCheckout = context.evaluate(Features.NEW_CHECKOUT) // Boolean
```

## Beyond Boolean Flags

Konditional supports any type—not just booleans:
- **Strings**: API endpoints, feature variants, configuration values
- **Numbers**: Thresholds, timeouts, limits
- **Data classes**: Complex configuration (themes, rate limits, feature bundles)
- **Custom types**: Enums, sealed classes, domain objects

## Custom Contexts for Your Domain

Extend `Context` to add business-specific fields like organization IDs, user roles, subscription tiers, or experiment groups. Your custom context flows through the entire evaluation chain with full type safety.

## Use Cases

- **Gradual Rollouts & A/B Testing**: Deterministic percentage-based rollouts with stable user bucketing
- **Configuration Management**: Type-safe config that varies by platform, locale, version, or custom context
- **Kill Switches & Canary Deployments**: Disable features instantly or test risky changes with small user segments
- **Multi-tenancy**: Different feature sets per organization, subscription tier, or user role

## Key Benefits

- **Compile-time safety**: Catch configuration errors before runtime with IDE support and type checking
- **Deterministic behavior**: Same user always gets same experience via stable SHA-256 bucketing
- **Decouple deploy from release**: Ship dark, enable gradually, rollback instantly
- **Zero overhead**: Thread-safe with lock-free reads, no reflection or external dependencies

## Documentation

### Getting Started
- **[Why Type Safety?](docs/WhyTypeSafety.md)**: See how compile-time guarantees eliminate entire classes of runtime errors
- **[Quick Start](docs/QuickStart.md)**: Get your first type-safe flag running in 5 minutes
- **[Migration Guide](docs/Migration.md)**: Step-by-step migration from string-based configuration systems
- **[Error Prevention Reference](docs/ErrorPrevention.md)**: Complete catalog of errors eliminated by type safety

### Core Concepts
- **[Core Concepts](docs/CoreConcepts.md)**: The type-safe building blocks (Feature, Context, Rule, EncodableValue)
- **[Evaluation](docs/Evaluation.md)**: How flags are evaluated, specificity ordering, and rollout bucketing
- **[Registry and Concurrency](docs/RegistryAndConcurrency.md)**: Thread-safe flag management without locks

### Guides
- **[Context Guide](docs/Context.md)**: Creating custom contexts for your business domain
- **[Builders Guide](docs/Builders.md)**: Master the type-safe DSL
- **[Rules Guide](docs/Rules.md)**: Advanced targeting and rollout strategies
- **[Serialization Guide](docs/Serialization.md)**: Remote configuration and JSON handling

---

**Get started**: `1)` Add dependency → `2)` Define flags → `3)` Configure rules → `4)` Evaluate

See the [Quick Start](docs/QuickStart.md) for a complete walkthrough.
