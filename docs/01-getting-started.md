# Getting Started

Type-safe flags in 5 minutes. If it compiles, it works—no runtime errors, no null checks, no string typos.

## Why Konditional?

Unlike LaunchDarkly/Split's runtime string-based flags, Konditional gives you compile-time guarantees.

| Feature          | LaunchDarkly/Split                                    | Konditional                                               |
|------------------|-------------------------------------------------------|-----------------------------------------------------------|
| **Type Safety**  | Runtime strings `client.boolVariation("flag", false)` | Compile-time properties `context.evaluate(Features.FLAG)` |
| **Evaluation**   | Server-side (network required)                        | Offline-first (local, zero-allocation)                    |
| **Context**      | `HashMap<String, Any>`                                | Typed data classes with IDE autocomplete                  |
| **Performance**  | Network latency + serialization                       | O(n) local evaluation (n < 10 rules typically)            |
| **Organization** | Tags/projects (runtime only)                          | Namespaces (compile-time isolated)                        |
| **Errors**       | Silent failures or runtime exceptions                 | Parse-don't-validate Result types                         |

**Core benefit:** Typos become compile errors. Type mismatches are impossible. Your IDE knows everything.

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
val enabled: Boolean = context.evaluateOrDefault(AppFeatures.DARK_MODE, default = false)
```

**vs LaunchDarkly:**
```kotlin
// LaunchDarkly - runtime string, can typo
val enabled = client.boolVariation("dark-mode", false)  // No IDE help

// Konditional - compile-time property, autocomplete works
val enabled = context.evaluateOrDefault(AppFeatures.DARK_MODE, false)  // IDE knows it's Boolean
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

Choose based on your error handling:

```kotlin
// Recommended: Explicit error handling
when (val result = context.evaluateSafe(AppFeatures.DARK_MODE)) {
    is EvaluationResult.Success -> use(result.value)
    is EvaluationResult.FlagNotFound -> logWarning()
    is EvaluationResult.EvaluationError -> logError(result.error)
}

// Quick: Returns default on any error
val enabled = context.evaluateOrDefault(AppFeatures.DARK_MODE, false)

// Nullable: Returns null on error
val enabled = context.evaluateOrNull(AppFeatures.DARK_MODE)

// Throws: Use only for development
val enabled = context.evaluateOrThrow(AppFeatures.DARK_MODE)
```

Most production code should use `evaluateOrDefault` for simplicity or `evaluateSafe` for robust error handling.

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
LaunchDarkly/Split: `client.boolVariation("flag-name", false)` — Typos fail at runtime
**Konditional:** `context.evaluate(Features.DARK_MODE)` — Typos fail at compile time

### 2. Offline-First Architecture
LaunchDarkly/Split: Network call required for evaluation (with caching)
**Konditional:** All evaluation happens locally. Zero network dependency.

### 3. Zero-Allocation Evaluation
LaunchDarkly/Split: Creates objects for each evaluation
**Konditional:** Immutable data structures, lock-free reads, no GC pressure

### 4. Type-Safe Contexts
LaunchDarkly/Split: `context.set("tier", "enterprise")` — String-based attributes
**Konditional:**
```kotlin
data class EnterpriseContext(
    // ... standard fields ...
    val subscriptionTier: SubscriptionTier  // Compile-time validated
) : Context
```

### 5. Deterministic Rollouts
Both use hashing, but Konditional's SHA-256 bucketing is:
- Platform-stable (same buckets on JVM, Android, iOS, Web)
- Independent per flag (user in 50% of Flag A ≠ in 50% of Flag B)
- Salt-controllable (change salt to redistribute users)

---

## Next Steps

**Just getting started?** You're done! Start adding flags to your code.

**Need advanced targeting?** See **[Targeting & Rollouts](04-targeting-rollouts.md)** for rules, specificity, and custom logic.

**Want custom contexts?** See **[Core Concepts](03-core-concepts.md)** for extending Context with business data.

**Migrating from LaunchDarkly/Split?** See **[Migration Guide](02-migration.md)** for concept mapping and adoption patterns.

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
val value = ctx.evaluateOrDefault(Features.FLAG, false)
```

That's it. Type-safe feature flags in 3 steps.
