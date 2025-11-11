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

Use any type that makes sense for your domain:

```kotlin
// String configuration
enum class ApiConfig(override val key: String) : Conditional<String, Context> {
    ENDPOINT("api_endpoint"),
}

config {
    ApiConfig.ENDPOINT with {
        default("https://api.prod.example.com")
        rule {
            platforms(Platform.WEB)
        } implies "https://api.staging.example.com"
    }
}

// Data class configuration
data class ThemeConfig(
    val primaryColor: String,
    val darkModeEnabled: Boolean,
    val fontSize: Int
)

enum class AppTheme(override val key: String) : Conditional<ThemeConfig, Context> {
    THEME("app_theme"),
}

config {
    AppTheme.THEME with {
        default(ThemeConfig("#FFFFFF", false, 14))
        rule {
            locales(AppLocale.EN_US)
        } implies ThemeConfig("#1E1E1E", true, 16)
    }
}
```

## Custom Contexts for Your Domain

Extend the base `Context` interface to add your own business logic:

```kotlin
data class EnterpriseContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    // Your custom fields
    val organizationId: String,
    val subscriptionTier: SubscriptionTier,
    val userRole: UserRole,
) : Context

// Use with enterprise-specific flags
enum class EnterpriseFeatures(
    override val key: String
) : Conditional<Boolean, EnterpriseContext> {
    ADVANCED_ANALYTICS("advanced_analytics"),
    BULK_EXPORT("bulk_export"),
}
```

## Use Cases

Konditional is perfect for:

- **Feature Rollouts**: Gradually roll out new features to a percentage of users
- **A/B Testing**: Test different variants with deterministic user assignment
- **Configuration Management**: Type-safe configuration that varies by environment, platform, or user
- **Canary Deployments**: Test risky changes with a small subset of users first
- **Kill Switches**: Quickly disable features in production without redeploying
- **Multi-tenancy**: Different feature sets for different organizations or subscription tiers
- **Regional Customization**: Different experiences for different locales or regions
- **Platform-Specific Features**: Enable features only on specific platforms (iOS, Android, Web)

## Key Benefits

### For Developers
- **Compile-time errors** instead of runtime surprises
- **IDE auto-completion** for all flag names and configuration
- **Refactoring support**: Rename flags safely with IDE refactoring tools
- **Type-safe DSL**: Configuration errors caught at compile time

### For Teams
- **Decouple deployment from release**: Ship code dark, enable features later
- **Reduce risk**: Roll out features gradually and monitor impact
- **Fast rollback**: Disable problematic features instantly without redeploying
- **Better testing**: Test multiple configurations without code changes

### For Operations
- **Deterministic behavior**: Same user always gets same experience
- **Thread-safe**: No locks on read path, atomic updates
- **Low overhead**: Pure computation, no network calls or database queries
- **Observable**: Easily log which flags are evaluated and their values

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

## Getting Started

1. **Add Konditional to your project** (coming soon: Maven Central)
2. **Define your flags** using enums or data classes
3. **Configure rules** with the type-safe DSL
4. **Evaluate in context** wherever you need the values

See [Examples](docs/examples.md) for complete working examples.

## Design Principles

- **Type safety first**: No stringly-typed APIs or runtime type errors
- **Deterministic by default**: Same inputs always produce same outputs
- **Context agnostic**: You define what information matters for your rules
- **Minimal dependencies**: (nearly) Pure Kotlin, easy to integrate anywhere
- **Thread-safe**: Safe to use from multiple threads concurrently
- **Extensible**: Add your own context types, value types, and rule logic
