# Konditional

**Type-safe feature flags for Kotlin that won't break in production.**

Konditional prevents the entire class of runtime errors that come from stringly-typed feature flag systems: typos that
ship to production, type coercion failures, inconsistent rollout logic, and configuration drift.

```kotlin
object AppFlags : Namespace("app") {
    val checkoutVersion by string(default = "classic") {
        rule("optimized") { platforms(Platform.IOS, Platform.ANDROID) }
        rule("experimental") { rollout { 50.0 } }
    }
}

// Typos don't compile. Types are guaranteed. Rollouts are deterministic.
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
- **Deterministic rollouts** — SHA-256 bucketing ensures same user → same bucket, always
- **Unified evaluation** — one rule DSL across your entire codebase, not per-domain evaluators
- **Explicit boundaries** — parse JSON configuration with validation; reject invalid updates before they affect
  production

Read the full argument: [Why Konditional Exists](09-why-konditional.md)

---

## Installation

```kotlin
dependencies {
    implementation("io.github.amichne:konditional:0.0.1")
}
```

---

## Quick Start

### Define flags as properties

```kotlin
object AppFeatures : Namespace("app") {
    val darkMode by boolean(default = false) {
        rule(true) { platforms(Platform.IOS) }
        rule(true) { rollout { 50.0 } }
    }

    val apiEndpoint by string(default = "https://api.example.com") {
        rule("https://api-web.example.com") { platforms(Platform.WEB) }
    }

    val maxRetries by integer(default = 3) {
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
    val checkoutVersion by string(default = "v1") {
        rule("v2") { rollout { 33.0 } }
        rule("v3") { rollout { 66.0 } }
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
    val theme by enum(default = Theme.LIGHT) {
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
) : KotlinEncodeable<ObjectSchema> {
    override val schema = schemaRoot {
        ::maxAttempts of { minimum = 1 }
        ::backoffMs of { minimum = 0.0 }
        ::enabled of { default = true }
    }
}

object PolicyFlags : Namespace("policy") {
    val retryPolicy by custom(default = RetryPolicy()) {
        rule(RetryPolicy(maxAttempts = 5, backoffMs = 2000.0)) { platforms(Platform.WEB) }
    }
}
```

Note: custom structured values are decoded via reflection at the JSON boundary; keep constructor parameter names stable
and provide defaults for optional fields.

---

## Deterministic Rollouts

Rollouts use SHA-256 bucketing for consistent, reproducible results:

```kotlin
object RolloutFlags : Namespace("rollout") {
    val newFeature by boolean(default = false) {
        rule(true) { rollout { 25.0 } }
    }
}
```

**Guarantees:**

- Same user + same flag + same percentage → same bucket
- Increasing 10% → 20% only adds users (no reshuffle) for the same `(stableId, flagKey, salt)`
- Rollout decisions are reproducible (`stableId` + flag key + salt → deterministic bucket)
- Changing `salt(...)` intentionally redistributes buckets (useful when re-running experiments)

No random number generators. No modulo edge cases. No per-team rollout implementations with subtle differences.

---

## Remote Configuration

### Load configuration from JSON

```kotlin
val json = fetchRemoteConfig()

// Important: deserialization requires that your Namespace objects have been initialized
// (so features are registered) before calling SnapshotSerializer.fromJson(...).
// See: 06-remote-config.md

when (val result = SnapshotSerializer.fromJson(json)) {
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
val snapshot = SnapshotSerializer.serialize(AppFlags.configuration)
persistToStorage(snapshot)
```

### Incremental updates (patches)

```kotlin
val patchJson = fetchPatch()
val currentConfig = AppFlags.configuration

when (val result = SnapshotSerializer.applyPatchJson(currentConfig, patchJson)) {
    is ParseResult.Success -> AppFlags.load(result.value)
    is ParseResult.Failure -> logError("Patch failed: ${result.error.message}")
}
```

See [06-remote-config.md](06-remote-config.md) and [08-persistence-format.md](08-persistence-format.md) for details.

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
        val creditCheck by boolean(default = false)
    }

    data object Payments : AppDomain("payments") {
        val stripeEnabled by boolean(default = true)
    }
}
```

Each namespace has independent configuration lifecycle, registry, and serialization.

---

## Core Concepts

- **Namespace:** Isolation boundary with its own registry and configuration lifecycle
- **Flags:** Delegated properties defined on a namespace
- **Context:** Runtime inputs for evaluation (`locale`, `platform`, `appVersion`, `stableId`)
- **Rules:** Typed criteria-to-value mappings (`platforms/locales/versions/rollout/extension`)
- **Snapshot/Patch:** JSON formats for persistence and incremental updates
- **Total evaluation:** No nulls—every flag returns its default if no rule matches

---

## Documentation

**Getting started:**

- [Quick Start Guide](01-getting-started.md)
- [Core Concepts](03-core-concepts.md)

**Features:**

- [Targeting & Rollouts](04-targeting-rollouts.md)
- [Evaluation Semantics](05-evaluation.md)
- [Remote Configuration](06-remote-config.md)
- [Persistence Format](08-persistence-format.md)

**Why Konditional:**

- [Why Konditional Exists](09-why-konditional.md) — The compelling argument

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

**Inconsistent rollout logic:**

```kotlin
// Before: Account team uses modulo, Payments team uses random(), different bucketing
// After: All flags use SHA-256 bucketing, same user → same bucket across all flags
```

Read more: [Real Problems Konditional Prevents](09-why-konditional.md#real-problems-konditional-prevents)

---

## When to Use Konditional

**Choose Konditional when:**

* You want compile-time correctness for flag definitions and callsites
* You need typed values beyond booleans (variants, thresholds, structured config)
* You run experiments and need deterministic, reproducible rollouts
* You value consistency over bespoke per-domain solutions
* You have remote configuration and want explicit validation boundaries

**Konditional might not fit if:**

* You need vendor-hosted dashboards more than compile-time safety

* Your flags are fully dynamic with zero static definitions

* You're okay with process/tooling to prevent string key drift

---

## License

MIT. See [LICENSE](../LICENSE).
