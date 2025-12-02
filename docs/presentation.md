# Konditional
## Feature Flags That Can't Fail

---

## The Promise

**If your code compiles, your flags work.**

Not "probably work."
Not "should work."
Not "work if configured correctly."

**They work. Period.**

---

## Configuration Should Be Code

Every feature flag system makes you choose:

- String keys that can't be validated
- Type casting that fails at runtime
- Null checks scattered everywhere
- Configuration errors discovered by users

**What if we refused to accept this?**

---

## The Core Idea

```kotlin
object AppFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
    val DARK_MODE by boolean(default = false) {
        rule {
            platforms(Platform.IOS)
            rollout { 50.0 }
        } returns true
    }
}

val context = Context(
    platform = Platform.IOS,
    locale = AppLocale.UNITED_STATES,
    appVersion = Version.parse("2.1.0"),
    stableId = StableId.of("user-123")
)

val enabled: Boolean = context.evaluate(AppFeatures.DARK_MODE)
```

**Everything the compiler can verify, it does verify.**

---

## Clarity

---

## No String Keys, Ever

```kotlin
// Traditional approach
val darkMode = config.getBoolean("dark_mode")  // Typo? Won't know until runtime
val darkMood = config.getBoolean("dark_mood")  // Different typo, different behavior

// Konditional
val darkMode = context.evaluate(AppFeatures.DARK_MODE)
//                                          ^^^^^^^^^
//                                Property reference - IDE verified
```

Your IDE autocompletes every flag.
Refactoring renames everything correctly.
Typos are caught before compilation.

**Clarity through elimination of ambiguity.**

---

## Configuration as Code

```kotlin
val API_ENDPOINT by string(default = "https://api.prod.com") {
    rule {
        platforms(Platform.WEB)
        versions { min(2, 0, 0) }
    } returns "https://api-v2.com"
}
```

- No external DSL to learn
- No YAML/JSON schemas to maintain
- No runtime parsing errors
- Version controlled with your code
- Reviewed in pull requests

**The configuration IS the documentation.**

---

## Evaluation Flow Is Transparent

```text
Context → Registry Lookup → Flag Active? → Rules (by specificity)
                                              ↓
                                         Match criteria:
                                         1. Platform ✓
                                         2. Locale ✓
                                         3. Version ✓
                                         4. Custom logic ✓
                                         5. Rollout bucket ✓
                                              ↓
                                         Return value
```

Every step is predictable.
Every rule has a specificity score.
Every evaluation follows the same path.

**Clarity through consistency.**

---

## Safety

---

## Errors That Can't Happen

Traditional systems allow these runtime failures:

```kotlin
config.getInt("timeout")           // Returns null? Throws exception? Returns default?
config.getString("retries")        // Type mismatch - string key, int expected
config.getBoolean("undefined_key") // Key doesn't exist
```

**Konditional makes these impossible:**

```kotlin
val TIMEOUT by int(default = 30)
val timeout: Int = context.evaluate(AppFeatures.TIMEOUT)
//          ^^^                                ^^^^^^^
//    Always Int                        Always exists
//    Never null                        Verified at compile time
```

---

## Type Safety From First Principles

```kotlin
// Define with types
val DARK_MODE by boolean(default = false)
val API_URL by string(default = "https://api.prod.com")
val MAX_RETRIES by int(default = 3)
val TIMEOUT by double(default = 30.0)

// Evaluate with types - enforced by generics
val darkMode: Boolean = context.evaluate(AppFeatures.DARK_MODE)
val apiUrl: String = context.evaluate(AppFeatures.API_URL)
val retries: Int = context.evaluate(AppFeatures.MAX_RETRIES)
val timeout: Double = context.evaluate(AppFeatures.TIMEOUT)

// This won't compile
val wrong: String = context.evaluate(AppFeatures.DARK_MODE)
//                                   ^^^^^^^^^^^^^^^^^^^^^^
//                                   Type error: Expected Boolean, found String
```

**The compiler is your safety net.**

---

## Non-Null Guarantees

Every feature requires a default value:

```kotlin
val FEATURE by boolean(default = false)  // ✓ Compiles
val FEATURE by boolean()                 // ✗ Won't compile - default required
```

Every evaluation method guarantees a value:

```kotlin
evaluateOrDefault(feature, default)  → T  (never null)
evaluateSafe(feature)               → EvaluationResult<T>  (wraps T, never null)
evaluateOrNull(feature)             → T?  (explicitly nullable)
```

**Null pointer exceptions don't exist here.**

---

## Safety in Where Failures Occur

```kotlin
// Compilation fails for:
- Wrong types
- Missing defaults
- Invalid context types
- Namespace mismatches

// Runtime may return:
- Default value (if flag inactive)
- Evaluated value (if rule matches)
- Error result (if using evaluateSafe)

// Runtime will NEVER:
- Throw NullPointerException
- Return wrong type
- Return undefined
```

**Know at compile time what can't fail at runtime.**

---

## Ergonomics

---

## Natural Declaration

```kotlin
object PaymentFeatures : FeatureContainer<Namespace.Payments>(
    Namespace.Payments
) {
    val APPLE_PAY by boolean(default = false) {
        rule {
            platforms(Platform.IOS)
            versions { min(2, 0, 0) }
            rollout { 25.0 }
        } returns true
    }

    val GOOGLE_PAY by boolean(default = false) {
        rule {
            platforms(Platform.ANDROID)
        } returns true
    }

    val MAX_TRANSACTION by int(default = 10000) {
        rule {
            locales(AppLocale.UNITED_STATES)
        } returns 50000
    }
}
```

**Everything in one place. Everything readable.**

---

## Flexible Evaluation

Choose the error handling that fits your context:

```kotlin
// Simple - for straightforward cases
val enabled = context.evaluateOrDefault(AppFeatures.DARK_MODE, default = false)

// Explicit - for production systems
when (val result = context.evaluateSafe(AppFeatures.DARK_MODE)) {
    is EvaluationResult.Success -> applyDarkMode(result.value)
    is EvaluationResult.FlagNotFound -> logWarning("Flag not found")
    is EvaluationResult.EvaluationError -> logError("Evaluation failed")
}

// Nullable - for optional features
val darkMode: Boolean? = context.evaluateOrNull(AppFeatures.DARK_MODE)
darkMode?.let { applyDarkMode(it) }
```

**The API adapts to your needs, not the reverse.**

---

## Rules That Compose

```kotlin
val PREMIUM_FEATURE by boolean(default = false) {
    // Specificity = 3 (platform + locale + version)
    rule {
        platforms(Platform.IOS)
        locales(AppLocale.UNITED_STATES)
        versions { min(2, 0, 0) }
    } returns true

    // Specificity = 2 (platform + custom logic)
    rule {
        platforms(Platform.WEB)
        extension {
            Evaluable.factory { ctx: EnterpriseContext ->
                ctx.subscriptionTier == SubscriptionTier.ENTERPRISE
            }
        }
    } returns true
}
```

**Most specific rule wins, automatically. No manual priority management.**

---

## Portability

---

## Platform-Stable Bucketing

The same user gets the same experience everywhere:

```kotlin
val user = StableId.of("user-abc123")

// JVM evaluation
bucket("feature", user, "v1")  → 4234  → In 50% rollout ✓

// Android evaluation
bucket("feature", user, "v1")  → 4234  → In 50% rollout ✓

// iOS (Kotlin/Native) evaluation
bucket("feature", user, "v1")  → 4234  → In 50% rollout ✓

// Web (Kotlin/JS) evaluation
bucket("feature", user, "v1")  → 4234  → In 50% rollout ✓
```

**SHA-256 guarantees identical bucketing across all platforms.**

---

## Zero Dependencies

```kotlin
// Core library dependencies
dependencies {
    implementation("io.amichne:konditional:0.0.1")

    // Optional: Only if using JSON serialization
    implementation("com.squareup.moshi:moshi:1.15.0")
}
```

- Pure Kotlin
- No reflection
- No code generation
- No DI framework required
- No platform-specific APIs

**Drop it into any Kotlin project. It just works.**

---

## Write Once, Evaluate Anywhere

```kotlin
// Define features in shared code
object SharedFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
    val DARK_MODE by boolean(default = false) {
        rule {
            platforms(Platform.IOS, Platform.ANDROID, Platform.WEB)
            rollout { 50.0 }
        } returns true
    }
}

// iOS app evaluates it
val enabled = iosContext.evaluate(SharedFeatures.DARK_MODE)

// Android app evaluates it
val enabled = androidContext.evaluate(SharedFeatures.DARK_MODE)

// Web app evaluates it
val enabled = webContext.evaluate(SharedFeatures.DARK_MODE)
```

**One definition. Multiple platforms. Guaranteed consistency.**

---

## Stability

---

## Deterministic Evaluation

```kotlin
val context = Context(
    locale = AppLocale.UNITED_STATES,
    platform = Platform.IOS,
    appVersion = Version.parse("2.1.0"),
    stableId = StableId.of("user-123")
)

// Evaluate 1000 times
repeat(1000) {
    val result = context.evaluate(AppFeatures.DARK_MODE)
    // Result is IDENTICAL every time
}
```

**Same inputs → Same outputs. Always. No randomness. No surprises.**

---

## Same User, Same Experience

```kotlin
val NEW_CHECKOUT by boolean(default = false) {
    rule {
        rollout { 50.0 }
    } returns true
}

// User evaluated on Monday
context.evaluate(AppFeatures.NEW_CHECKOUT)  → true

// Same user evaluated on Friday
context.evaluate(AppFeatures.NEW_CHECKOUT)  → true

// Same user evaluated after app update
context.evaluate(AppFeatures.NEW_CHECKOUT)  → true
```

**SHA-256 bucketing ensures users stay in their buckets.**

---

## Thread-Safe by Design

```kotlin
// Thread 1: Evaluating
val value1 = context.evaluateOrDefault(AppFeatures.DARK_MODE, false)

// Thread 2: Evaluating
val value2 = context.evaluateOrDefault(AppFeatures.API_ENDPOINT, "default")

// Thread 3: Updating configuration
Namespace.Global.load(newConfiguration)  // Atomic swap

// Thread 4: Still evaluating
val value3 = context.evaluateOrDefault(AppFeatures.MAX_RETRIES, 3)
```

- Immutable data structures
- Lock-free reads
- Atomic configuration updates
- Zero contention

**Scales linearly with CPU cores.**

---

## Verifiability

---

## Testing Is Natural

```kotlin
@Test
fun `iOS users in UNITED_STATES locale see dark mode`() {
    val context = Context(
        locale = AppLocale.UNITED_STATES,
        platform = Platform.IOS,
        appVersion = Version.parse("2.1.0"),
        stableId = StableId.of("test-user")
    )

    val result = context.evaluateSafe(AppFeatures.DARK_MODE)

    assertTrue(result is EvaluationResult.Success && result.value == true)
}
```

**Determinism makes testing trivial.**

---

## Rollout Distribution Is Provable

```kotlin
@Test
fun `50% rollout distributes correctly`() {
    val sampleSize = 10_000
    val enabledCount = (0 until sampleSize).count { i →
        val context = Context(
            locale = AppLocale.UNITED_STATES,
            platform = Platform.IOS,
            appVersion = Version.parse("1.0.0"),
            stableId = StableId.of("user-$i")
        )
        context.evaluateOrDefault(AppFeatures.ROLLOUT_FLAG, default = false)
    }

    val percentage = (enabledCount.toDouble() / sampleSize) * 100
    assertTrue(percentage in 48.0..52.0)  // ~50% ± 2%
}
```

**SHA-256 produces mathematically verifiable distributions.**

---

## Specificity Is Predictable

```kotlin
val API_ENDPOINT by string(default = "https://api.prod.com") {
    // Specificity = 3: evaluated FIRST
    rule {
        platforms(Platform.IOS)
        locales(AppLocale.UNITED_STATES)
        versions { min(2, 0, 0) }
    } returns "https://api-v2-ios-us.com"

    // Specificity = 2: evaluated SECOND
    rule {
        platforms(Platform.IOS)
        locales(AppLocale.UNITED_STATES)
    } returns "https://api-ios-us.com"

    // Specificity = 1: evaluated THIRD
    rule {
        platforms(Platform.IOS)
    } returns "https://api-ios.com"
}
```

**No rule priority configuration. No ordering bugs. Automatic and correct.**

---

## Putting It All Together

---

## Real-World Example: Gradual Rollout

```kotlin
object NewFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
    val NEW_CHECKOUT by boolean(default = false) {
        salt("v1")

        // Enterprise customers: 100%
        rule {
            extension {
                Evaluable.factory { ctx: EnterpriseContext ->
                    ctx.subscriptionTier == SubscriptionTier.ENTERPRISE
                }
            }
        } returns true

        // iOS users: 50% rollout
        rule {
            platforms(Platform.IOS)
            versions { min(2, 0, 0) }
            rollout { 50.0 }
        } returns true

        // Everyone else: 10% rollout
        rule {
            rollout { 10.0 }
        } returns true
    }
}
```

**Clarity. Safety. Ergonomics. Verifiability. All in one declaration.**

---

## What We've Gained

| Principle | How Konditional Delivers |
|-----------|-------------------------|
| **Clarity** | No strings, IDE support, configuration as code |
| **Safety** | Compile-time types, non-null, failures at compilation |
| **Ergonomics** | Natural DSL, flexible evaluation, automatic specificity |
| **Portability** | Platform-stable, zero dependencies, works everywhere |
| **Stability** | Deterministic, thread-safe, immutable |
| **Verifiability** | Testable, provable, predictable |

**Not built by comparing to alternatives.**
**Built by asking: "What should be impossible?"**

---

## The Core Insight

Most systems try to make configuration errors **unlikely**.

Konditional makes them **impossible**.

- Can't have a type error → Enforced by generics
- Can't have a null → Enforced by defaults
- Can't have a typo → Enforced by property references
- Can't have inconsistent bucketing → Enforced by SHA-256
- Can't have race conditions → Enforced by immutability

**The compiler proves your flags work before your users run them.**

---

## Why This Matters

```kotlin
// With Konditional, this is ALL THE VALIDATION YOU NEED:
val enabled: Boolean = context.evaluate(AppFeatures.DARK_MODE)

// No checks for:
// - Does the key exist? ✓ Compiler verified
// - Is it the right type? ✓ Compiler verified
// - Could it be null? ✓ Compiler verified
// - Will it evaluate? ✓ Determinism guaranteed
// - Is it thread-safe? ✓ Immutability guaranteed
// - Will bucketing work? ✓ SHA-256 guaranteed
```

**You write less code. The code you write is correct.**

---

## An Invitation

Konditional asks you to consider:

What if configuration was just code?
What if types were always correct?
What if evaluation was always deterministic?
What if concurrency was never an issue?
What if testing was always straightforward?

**What if feature flags just worked?**

---

## Get Started

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.amichne:konditional:0.0.1")
}
```

```kotlin
// Define
object MyFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
    val MY_FLAG by boolean(default = false)
}

// Evaluate
val context = Context(
    locale = AppLocale.UNITED_STATES,
    platform = Platform.IOS,
    appVersion = Version.parse("1.0.0"),
    stableId = StableId.of("user-id")
)

val enabled = context.evaluate(MyFeatures.MY_FLAG)
```

**If it compiles, it works.**

---

## Thank You

**Konditional: Feature flags that can't fail at runtime.**

Documentation: [github.com/amichne/konditional](https://github.com/amichne/konditional)

---
