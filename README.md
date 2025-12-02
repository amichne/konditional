# Konditional

✨ **Feature flags that can't fail at runtime.** ✨

Konditional makes configuration errors impossible. Not unlikely—impossible. If your code compiles, your flags work. Period.

## The Core Idea

Configuration should be code, not strings. When you define a feature flag, the compiler should know its type, validate its rules, and ensure every evaluation is safe. No exceptions. No null checks. No runtime surprises.

```kotlin
// Define features as properties, and register keys using the property names via delegation
object AppFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
    val darkMode by boolean(default = false) {
        rule {
            platforms(Platform.IOS)
            rollout { 50.0 }
            versions {
                max(4, 1, 3)
                min(1, 9, 4)
            }
        } returns true
    }

    val timeout by double(default = 30.0) {
        rule {
            platforms(Platform.ANDROID)
            versions { max(4) }
        } returns 45.0
    }

    val retries by int(default = 3) {
        rule {
            versions { min(2, 0, 0) }
        } returns 5
    }
}

// Evaluate with context
val context = Context(
    locale = AppLocale.UNITED_STATES,
    platform = Platform.IOS,
    appVersion = Version.parse("2.1.0"),
    stableId = StableId.of("user-123")
)

// Type-safe, null-safe, guaranteed to work
val isDarkMode: Boolean = context.evaluate(AppFeatures.darkMode)
val timeout: Double = context.evaluate(AppFeatures.timeout, 30.0)
val retries: Int = context.evaluate(AppFeatures.retries, 3)
```

**What just happened?**

- No string keys means no typos can sneak by
- No type casting means no runtime type failure
- No null checks means certainty on data present
- No runtime configuration errors
- IDE autocomplete on every flag
- Refactoring renames everything correctly
- If it compiles, it works

## Why This Matters

Every flag evaluation in Konditional is:

**Type-safe**: The compiler enforces that `DARK_MODE` returns `Boolean`, `API_TIMEOUT` returns `Double`, and `MAX_RETRIES` returns `Int`. Wrong type? Won't compile.

**Deterministic**: Same user, same flag, same result. Every time. SHA-256 bucketing ensures identical inputs always produce identical outputs.

**Non-null**: Default values are required. Every evaluation has a fallback. Null pointer exceptions don't exist here.

**Thread-safe**: Read flags from any thread, any time. Lock-free reads mean zero contention. Update configurations atomically without blocking readers.

**Isolated**: Organize flags by team or domain using Namespaces. `Namespace.Global`, `Namespace.Payments`, `Namespace.Authentication`—each with its own registry, zero collision risk.

## Beyond Booleans

Flags can return any type you need:

```kotlin
// Strings: API endpoints, feature variants
val API_ENDPOINT by string(default = "https://api.prod.example.com") {
    rule {
        platforms(Platform.WEB)
    } returns "https://api-staging.example.com"
}

// Numbers: Thresholds, timeouts, limits
val RATE_LIMIT by int(default = 100) {
    rule {
        rollout { 10.0 }
    } returns 500
}

// Complex types: Configuration objects
data class ThemeConfig(
    val primaryColor: String,
    val fontSize: Int,
    val darkMode: Boolean
)

val APP_THEME by jsonObject(
    default = ThemeConfig("#FFFFFF", 14, false)
) {
    rule {
        platforms(Platform.IOS)
    } returns ThemeConfig("#000000", 16, true)
}
```

## Your Domain, Your Context

Extend `Context` with business-specific fields. The type system ensures your custom context flows through every evaluation:

```kotlin
data class EnterpriseContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    val organizationId: String,
    val subscriptionTier: SubscriptionTier
) : Context

object EnterpriseFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
    val ADVANCED_ANALYTICS by boolean(default = false)
}

// Compile-time guarantee: this context works with these features
val ctx = EnterpriseContext(/* ... */)
val enabled = ctx.evaluateOrDefault(EnterpriseFeatures.ADVANCED_ANALYTICS, false)
```

## Targeting Made Simple

Rules compose naturally. All criteria must match for a rule to apply:

```kotlin
val PREMIUM_FEATURE by boolean(default = false) {
    rule {
        platforms(Platform.IOS, Platform.ANDROID)  // Mobile only
        locales(AppLocale.UNITED_STATES)                   // English US
        versions {
            min(2, 0, 0)                           // Version >= 2.0.0
        }
        rollout { 50.0 }                           // 50% of matching users
    } returns true
}
```

Rules are automatically sorted by specificity—more specific rules win:

```kotlin
// Specificity = 2 (platform + locale) → evaluated first
rule {
    platforms(Platform.IOS)
    locales(AppLocale.UNITED_STATES)
} returns "specific"

// Specificity = 1 (platform only) → evaluated second
rule {
    platforms(Platform.IOS)
} returns "general"
```

## Deterministic Rollouts

Gradual rollouts use SHA-256 bucketing. Same user always gets the same result:

```kotlin
val NEW_CHECKOUT by boolean(default = false) {
    rule {
        rollout { 25.0 }  // 25% of users
    } returns true
}
```

**Properties**:
- Same user, same bucket, always
- Independent per flag (no cross-contamination)
- Platform-stable (JVM, Android, iOS, Web)
- Reproducible (change salt to redistribute)

```kotlin
val EXPERIMENT by boolean(default = false) {
    salt("v2")  // New salt = new bucketing
    rule {
        rollout { 50.0 }
    } returns true
}
```

## Remote Configuration

Export configurations to JSON, update remotely, reload atomically:

```kotlin
// Export current configuration
val json = SnapshotSerializer.serialize(Namespace.Global.configuration())
File("flags.json").writeText(json)

// Load from JSON
val json = File("flags.json").readText()
when (val result = SnapshotSerializer.fromJson(json)) {
    is ParseResult.Success -> Namespace.Global.load(result.value)
    is ParseResult.Failure -> logError("Parse failed: ${result.error}")
}

// Apply incremental patches
when (val result = SnapshotSerializer.applyPatchJson(currentConfig, patchJson)) {
    is ParseResult.Success -> Namespace.Global.load(result.value)
    is ParseResult.Failure -> logError("Patch failed: ${result.error}")
}
```

## Organize by Domain

Use Namespaces to isolate features by team or business domain:

```kotlin
object AuthFeatures : FeatureContainer<Namespace.Authentication>(
    Namespace.Authentication
) {
    val SOCIAL_LOGIN by boolean(default = false)
    val TWO_FACTOR_AUTH by boolean(default = true)
}

object PaymentFeatures : FeatureContainer<Namespace.Payments>(
    Namespace.Payments
) {
    val APPLE_PAY by boolean(default = false)
    val GOOGLE_PAY by boolean(default = false)
}
```

**Benefits**:
- Compile-time isolation (features type-bound to namespace)
- Runtime isolation (each namespace has separate registry)
- Clear ownership boundaries
- Zero collision risk

## Zero Dependencies

Pure Kotlin. No reflection. No code generation. No DI framework required.

Only external dependency: Moshi for JSON serialization (and only if you use it).

## Installation

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.amichne:konditional:0.0.1")
}
```

## Get Started

**1. Define features**:
```kotlin
object MyFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
    val MY_FLAG by boolean(default = false)
}
```

**2. Configure rules (optional)**:
```kotlin
val MY_FLAG by boolean(default = false) {
    rule {
        platforms(Platform.IOS)
        rollout { 50.0 }
    } returns true
}
```

**3. Evaluate**:
```kotlin
val context = Context(
    locale = AppLocale.UNITED_STATES,
    platform = Platform.IOS,
    appVersion = Version.parse("1.0.0"),
    stableId = StableId.of("user-id")
)

val enabled = context.evaluateOrDefault(MyFeatures.MY_FLAG, false)
```

## Use Cases

**Gradual Rollouts**: Deploy features to 10%, monitor, increase to 50%, then 100%. Deterministic bucketing ensures stability.

**A/B Testing**: Split traffic 50/50 for experiments. Each flag has independent bucketing—no cross-contamination.

**Configuration Management**: Environment-specific config (API endpoints, timeouts, limits) that varies by platform, locale, or version.

**Kill Switches**: Disable features instantly by updating remote configuration.

**Multi-tenancy**: Different feature sets per organization, subscription tier, or user role using custom contexts.

**Canary Deployments**: Test risky changes with 5% of users before full rollout.

## Documentation

### Getting Started
- **[Quick Start](docs/QuickStart.md)** - Get your first flag running in 5 minutes
- **[Overview](docs/index.md)** - Complete API overview and core concepts

### Core Concepts
- **[Features](docs/Features.md)** - All feature definition patterns
- **[Context](docs/Context.md)** - Evaluation contexts and custom extensions
- **[Evaluation](docs/Evaluation.md)** - Deep dive into flag evaluation
- **[Rules](docs/Rules.md)** - Advanced targeting and rollouts

### Advanced
- **[Configuration](docs/Configuration.md)** - Complete DSL reference
- **[Serialization](docs/Serialization.md)** - Export/import configurations as JSON
- **[Registry](docs/Registry.md)** - Namespace and registry management
- **[Results](docs/Results.md)** - Error handling with EvaluationResult

## Key Principles

**Type Safety First**: Generic type parameters eliminate runtime type errors. If it compiles, the types are correct.

**Deterministic**: Same inputs always produce same outputs. No random behavior, no surprises.

**Thread-Safe**: Lock-free reads with atomic updates. Read from any thread without blocking.

**Zero Dependencies**: Pure Kotlin. Easy to integrate anywhere.

**Extensible**: Custom contexts, custom rules, custom value types—extend everything.

## Contributing

Contributions welcome! See [CONTRIBUTING.md](CONTRIBUTING.md) for development guidelines.

## License

MIT License - See [LICENSE](LICENSE) for details.

---

**Start here**: [Quick Start Guide](docs/QuickStart.md)

**Questions?** Open an issue at [github.com/amichne/konditional](https://github.com/amichne/konditional)
