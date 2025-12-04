# Getting Started

Type-safe flags in 5 minutes. If it compiles, it works—no runtime errors, no null checks, no string typos.

## Why Konditional?

Most feature flag systems use runtime strings. Konditional uses compile-time properties instead.

| Feature          | String-Based (Custom)                                                       | Konditional                                                                                   |
|------------------|-----------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------|
| **Type Safety**  | Exclusively booleans, drives branching explosion in reality                 | Compile-time safety allows runtime usage without risk `feature { Features.FLAG }`             |
| **Evaluation**   | Hardcoded via boolean flows                                                 | Dynamic and generic, maintaining rigor of type-checking                                       |
| **Context**      | Enum class with string keys                                                 | Typed data classes with IDE autocomplete                                                      |
| **Performance**  | Shared module forces full-rebuild at compile-time, unable to leverage cache | Module changes to flags are not invalidating of task-graph for parent                         |
| **Organization** | Prefixing, shared single source by all                                      | Namespaces (compile-time isolated), with type-enforced boundaries, infitinitely divisible     |
| **Errors**       | Silent failures, null checks, type casting                                  | Guarnteed valid startup config,<br/> Update failures emerge **before** update, during parsing |

**Core benefits:** 
* No more invalid configurations, instead, compile errors.
* First-class Gradle caching support
* Modules own the feature flags they are concerned with
* Unified, single-source, for all flagging
* Your IDE knows everything.
* Built to scale to multi-tenancy, seamlessly

---

## Installation

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.amichne:konditional:0.0.1")
}
```

---

## Your First Flag

Define flags as properties. The compiler enforces types:

```kotlin
import io.amichne.konditional.core.features.FeatureContainer
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.context.*

object AppFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
    val DARK_MODE by boolean(default = false) {
        rule {
            platforms(Platform.IOS)
            rollout { 50.0 }
        } returns true
    }
}

// Create context (required for evaluation)
val context = Context(
    locale = AppLocale.UNITED_STATES,
    platform = Platform.IOS,
    appVersion = Version.parse("2.1.0"),
    stableId = StableId.of("a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6")
)

// Evaluate - returns Boolean, never null
val enabled: Boolean = feature { AppFeatures.DARK_MODE }
```

**vs String-Based Systems:**
```kotlin
// String-based - runtime lookup, can typo, type unknown
val enabled = featureFlags.getBoolean("dark-mode", false)  // No IDE help, typos fail silently

// Konditional - compile-time property, autocomplete works
val enabled = feature { AppFeatures.DARK_MODE }  // IDE knows it's Boolean
```

---

## Multiple Types

All primitives work out of the box:

```kotlin
object AppConfig : FeatureContainer<Namespace.Global>(Namespace.Global) {
    val DARK_MODE by boolean(default = false)
    val API_ENDPOINT by string(default = "https://api.example.com")
    val MAX_RETRIES by int(default = 3)
    val TIMEOUT by double(default = 30.0)
}
```

Each flag is typed—you can't accidentally treat a string as a boolean.

---

## Common Patterns

### Pattern 1: Gradual Rollout

Ship to 10% of users, then expand:

```kotlin
val NEW_CHECKOUT by boolean(default = false) {
    rule {
        platforms(Platform.ANDROID)
        rollout { 10.0 }  // Start small
    } returns true
}

// Later: increase to 50%
// Same users stay enabled (deterministic SHA-256 bucketing)
```

### Pattern 2: Platform-Specific Config

Different values per platform:

```kotlin
val API_ENDPOINT by string(default = "https://api.example.com") {
    rule { platforms(Platform.IOS) } returns "https://api-ios.example.com"
    rule { platforms(Platform.ANDROID) } returns "https://api-android.example.com"
    rule { platforms(Platform.WEB) } returns "https://api-web.example.com"
}
```

### Pattern 3: A/B Testing

Split traffic 50/50:

```kotlin
val RECOMMENDATION_ALGO by string(default = "collaborative") {
    rule { rollout { 50.0 } } returns "content-based"
}
// Same user always gets same variant (deterministic)
```

---

## Evaluation Methods

```kotlin
// Simple evaluation with default
val enabled = feature { AppFeatures.DARK_MODE }

```

---

## Organizing by Team/Domain

Use namespaces to isolate features:

```kotlin
object AuthFeatures : FeatureContainer<Namespace.Authentication>(
    Namespace.Authentication
) {
    val SOCIAL_LOGIN by boolean(default = false)
}

object PaymentFeatures : FeatureContainer<Namespace.Payments>(
    Namespace.Payments
) {
    val APPLE_PAY by boolean(default = false)
}
```

**Benefits:**
- Features can't collide across namespaces
- Each team owns their namespace
- Type system prevents cross-namespace access mistakes

---

## Key Differentiators

### 1. Compile-Time Safety
String-based: `getFlag("flag-name")` — Typos fail at runtime (or silently return defaults)
**Konditional:** `feature { Features.DARK_MODE }` — Typos fail at compile time

### 2. Offline-First Architecture
String-based (LaunchDarkly/Statsig): Network call or cache required for evaluation
**Konditional:** All evaluation happens locally. Zero network dependency.

### 3. Zero-Allocation Evaluation
String-based: HashMap lookups, type casting, object creation per evaluation
**Konditional:** Immutable data structures, lock-free reads, no GC pressure

### 4. Type-Safe Contexts
String-based: `context.put("tier", "enterprise")` — String keys, Any values, no validation
**Konditional:**
```kotlin
data class EnterpriseContext(
    // ... standard fields ...
    val subscriptionTier: SubscriptionTier  // Enum, not string - compile-time validated
) : Context
```

### 5. Deterministic Rollouts
Most systems use hashing, but Konditional's SHA-256 bucketing is:
- Platform-stable (same buckets on JVM, Android, iOS, Web)
- Independent per flag (user in 50% of Flag A ≠ in 50% of Flag B)
- Salt-controllable (change salt to redistribute users)

---

## Next Steps

**Just getting started?** You're done! Start adding flags to your code.

**Need advanced targeting?** See **[Targeting & Rollouts](04-targeting-rollouts.md)** for rules, specificity, and custom logic.

**Want custom contexts?** See **[Core Concepts](03-core-concepts.md)** for extending Context with business data.

**Migrating from another system?** See **[Migration Guide](02-migration.md)** for concept mapping and adoption patterns.

**Loading remote configs?** See **[Remote Configuration](06-remote-config.md)** for JSON serialization.

---

## Quick Reference

```kotlin
// 1. Define features
object Features : FeatureContainer<Namespace.Global>(Namespace.Global) {
    val FLAG by boolean(default = false) {
        rule { platforms(Platform.IOS); rollout { 50.0 } } returns true
    }
}

// 2. Create context
val ctx = Context(locale, platform, version, stableId)

// 3. Evaluate
val value = feature { Features.FLAG }
```

That's it. Type-safe feature flags in 3 steps.
