---
slug: /
---

# Konditional

**Type-safe feature flags for Kotlin that won't break in production.**

Konditional prevents the entire class of runtime errors that come from stringly-typed feature flag systems: typos that
ship to production, type coercion failures, inconsistent ramp-up logic, and configuration drift.

```kotlin
object AppFlags : Namespace("app") {
    val checkoutVersion by string<Context>(default = "classic") {
        rule("optimized") { platforms(Platform.IOS, Platform.ANDROID) }
        rule("experimental") { rampUp { 50.0 } }
    }
}

// Typos don't compile. Types are guaranteed. Ramp-ups are deterministic.
val version: String = AppFlags.checkoutVersion.evaluate(ctx)
```

---

## Why Konditional?

Traditional feature flag SDKs use string keys:

```kotlin
val enabled = flagClient.getBool("new_onboaring_flow", false)  // typo ships to production
```

This compiles. It deploys. Your experiment silently fails. You discover it weeks later.

**Konditional makes flags compile-time correct:**

```kotlin
val version = AppFlags.checkoutVersion.evaluate(ctx)  // property access is compile-time checked
// AppFlags.checkotuVersion.evaluate(ctx)              // doesn't compile (typo)
```

Beyond typo safety, Konditional gives you:

- **Typed values** — not just booleans, but strings, ints, doubles, enums, and custom types
- **Deterministic ramp-ups** — SHA-256 bucketing ensures same user → same bucket, always
- **Unified evaluation** — one rule DSL across your entire codebase, not per-domain evaluators
- **Explicit boundaries** — parse JSON configuration with validation; reject invalid updates before they affect
  production

Read the full argument: [Why Konditional Exists](why-konditional)

---

## Installation

```kotlin
dependencies {
    implementation("io.amichne:konditional:0.0.1")
}
```

---

## Quick Start

### Define flags as properties

```kotlin
object AppFeatures : Namespace("app") {
    val darkMode by boolean<Context>(default = false) {
        rule(true) { platforms(Platform.IOS) }
        rule(true) { rampUp { 50.0 } }
    }

    val apiEndpoint by string<Context>(default = "https://api.example.com") {
        rule("https://api-web.example.com") { platforms(Platform.WEB) }
    }

    val maxRetries by integer<Context>(default = 3) {
        rule(5) { versions { min(2, 0, 0) } }
    }
}
```

### Evaluate with context

```kotlin
val ctx = Context(
    locale = AppLocale.UNITED_STATES,
    platform = Platform.IOS,
    appVersion = Version.of(2, 1, 0),
    stableId = StableId.of("user-123")
)

val enabled: Boolean = AppFeatures.darkMode.evaluate(ctx)
val endpoint: String = AppFeatures.apiEndpoint.evaluate(ctx)
val retries: Int = AppFeatures.maxRetries.evaluate(ctx)
```

**Evaluation is total:** if no rule matches, the default is returned. No nulls, no exceptions.

---

## Beyond Booleans: Typed Values

### Stop encoding variants as multiple booleans

**Before (boolean explosion):**

```kotlin
if (isEnabled(CHECKOUT_V1) && !isEnabled(CHECKOUT_V2)) {
    // v1 logic
} else if (isEnabled(CHECKOUT_V2) && !isEnabled(CHECKOUT_FAST_PATH)) {
    // v2 without fast path
} else if (isEnabled(CHECKOUT_V3) || isEnabled(CHECKOUT_FAST_PATH)) {
    // which logic wins?
}
```

**After (typed values):**

```kotlin
object CheckoutFlags : Namespace("checkout") {
    val checkoutVersion by string<Context>(default = "v1") {
        rule("v2") { rampUp { 33.0 } }
        rule("v3") { rampUp { 66.0 } }
    }
}

when (CheckoutFlags.checkoutVersion.evaluate(ctx)) {
    "v1" -> v1Checkout()
    "v2" -> v2Checkout()
    "v3" -> v3Checkout()
}
```

### Enums

```kotlin
enum class Theme { LIGHT, DARK, AUTO }

object ThemeFlags : Namespace("theme") {
    val theme by enum<Theme, Context>(default = Theme.LIGHT) {
        rule(Theme.DARK) { platforms(Platform.IOS) }
    }
}
```

### Custom structured values

```kotlin
data class RetryPolicy(
    val maxAttempts: Int = 3,
    val backoffMs: Double = 1000.0,
    val enabled: Boolean = true
) : Konstrained<ObjectSchema> {
    override val schema = schemaRoot {
        ::maxAttempts of { minimum = 1 }
        ::backoffMs of { minimum = 0.0 }
        ::enabled of { default = true }
    }
}

object PolicyFlags : Namespace("policy") {
    val retryPolicy by custom<RetryPolicy, Context>(default = RetryPolicy()) {
        rule(RetryPolicy(maxAttempts = 5, backoffMs = 2000.0)) { platforms(Platform.WEB) }
    }
}
```

Note: custom structured values are decoded via reflection at the JSON boundary; keep constructor parameter names stable
and provide defaults for optional fields. If you use code shrinking/obfuscation, keep constructor parameter names and
ensure Kotlin reflection is available at runtime.

---

## Deterministic Ramp-ups

Ramp-ups use SHA-256 bucketing for consistent, reproducible results:

```kotlin
object RampUpFlags : Namespace("ramp-up") {
    val newFeature by boolean<Context>(default = false) {
        rule(true) { rampUp { 25.0 } }
    }
}
```

**Guarantees:**

- Same user + same flag + same percentage → same bucket
- Increasing 10% → 20% only adds users (no reshuffle) for the same `(stableId, flagKey, salt)`
- Ramp-up decisions are reproducible (`stableId` + flag key + salt → deterministic bucket)
- Changing `salt(...)` intentionally redistributes buckets (useful when re-running experiments)

No random number generators. No modulo edge cases. No per-team ramp-up implementations with subtle differences.

---

## Remote Configuration

### Load configuration from JSON

```kotlin
val json = fetchRemoteConfig()

// Important: deserialization requires that your Namespace objects have been initialized
// (so features are registered) before calling ConfigurationSnapshotCodec.decode(...).
// See: /fundamentals/definition-vs-initialization

when (val result = ConfigurationSnapshotCodec.decode(json)) {
    is ParseResult.Success -> AppFlags.load(result.value)
    is ParseResult.Failure -> {
        // Invalid JSON rejected, last-known-good remains active
        logError("Config parse failed: ${result.error.message}")
    }
}
```

**The boundary is explicit:** Parse failures don't crash your app or silently corrupt evaluation. Bad config is
rejected; the previous working config stays active.

### Serialize current configuration

```kotlin
val snapshot = ConfigurationSnapshotCodec.encode(AppFlags.configuration)
persistToStorage(snapshot)
```

### Incremental updates (patches)

```kotlin
val patchJson = fetchPatch()
val currentConfig = AppFlags.configuration

when (val result = ConfigurationSnapshotCodec.applyPatchJson(currentConfig, patchJson)) {
    is ParseResult.Success -> AppFlags.load(result.value)
    is ParseResult.Failure -> logError("Patch failed: ${result.error.message}")
}
```

See [Loading from JSON](/getting-started/loading-from-json) and [Persistence Format](/persistence-format) for details.

---

## Thread Safety

- **Atomic updates:** `Namespace.load()` swaps configuration atomically
- **Lock-free reads:** Evaluation reads a snapshot without blocking writers
- **No races:** Multiple threads can evaluate flags concurrently while configuration updates happen in the background

---

## Namespaces (Optional Isolation)

If you need isolated registries (e.g., per-team, per-domain), define multiple namespaces:

```kotlin
sealed class AppDomain(id: String) : Namespace(id) {
    data object Account : AppDomain("account") {
        val creditCheck by boolean<Context>(default = false)
    }

    data object Payments : AppDomain("payments") {
        val stripeEnabled by boolean<Context>(default = true)
    }
}
```

Each namespace has independent configuration lifecycle, registry, and serialization.

---

## Core Concepts

- **Namespace:** Isolation boundary with its own registry and configuration lifecycle
- **Flags:** Delegated properties defined on a namespace
- **Context:** Runtime inputs for evaluation (`locale`, `platform`, `appVersion`, `stableId`)
- **Rules:** Typed criteria-to-value mappings (`platforms/locales/versions/rampUp/extension`)
- **Snapshot/Patch:** JSON formats for persistence and incremental updates
- **Total evaluation:** No nulls—every flag returns its default if no rule matches

---

## Documentation

**Getting started:**

- [Quick Start Guide](/getting-started/installation)
- [Core Concepts](/fundamentals/core-primitives)

**Features:**

- [Targeting & Ramp-ups](/rules-and-targeting/rollout-strategies)
- [Evaluation Semantics](/fundamentals/evaluation-semantics)
- [Loading from JSON](/getting-started/loading-from-json)
- [Persistence Format](/persistence-format)

**Why Konditional:**

- [Why Konditional Exists](why-konditional) — The compelling argument

---

## What Konditional Prevents

**Typos that ship to production:**

```kotlin
flagClient.getBool("new_onboaring_flow", false)  // compiles, deploys, fails silently
AppFlags.newOnboaringFlow                        // compile error
```

**Type coercion incidents:**

```kotlin
// JSON: {"max_retries": "disabled"}
flagClient.getInt("max_retries", 3)  // returns 0, service fails immediately
AppFlags.maxRetries                  // parse fails at boundary, last-known-good stays active
```

**Boolean explosion:**

```kotlin
// Before: 5 boolean flags → 2^5 = 32 test cases, most undefined
// After: 1 typed enum flag → 5 explicit variants, all defined
```

**Inconsistent ramp-up logic:**

```kotlin
// Before: Account team uses modulo, Payments team uses random(), different bucketing
// After: All flags use SHA-256 bucketing, same user → same bucket across all flags
```

Read more: [Real Problems Konditional Prevents](why-konditional#real-problems-konditional-prevents)

---

## When to Use Konditional

**Choose Konditional when:**

* You want compile-time correctness for flag definitions and callsites
* You need typed values beyond booleans (variants, thresholds, structured config)
* You run experiments and need deterministic, reproducible ramp-ups
* You value consistency over bespoke per-domain solutions
* You have remote configuration and want explicit validation boundaries

**Konditional might not fit if:**

* You need vendor-hosted dashboards more than compile-time safety

* Your flags are fully dynamic with zero static definitions

* You're okay with process/tooling to prevent string key drift

---

## License

MIT. See [LICENSE](https://github.com/amichne/konditional/blob/main/LICENSE).
