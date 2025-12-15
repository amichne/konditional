# Konditional

**Type-safe feature flags for Kotlin that won't break in production.**

Konditional prevents the entire class of runtime errors that come from stringly-typed feature flag systems: typos that ship to production, type coercion failures, inconsistent rollout logic, and configuration drift.

```kotlin
object AppFlags : FeatureContainer() {
    val checkoutVersion by string(default = "classic") {
        rule { platforms(Platform.MOBILE) } returns "optimized"
        rule { rollout { 50.0 } } returns "experimental"
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
val enabled = AppFlags.newOnboardingFlow.evaluate(ctx)  // typos are compile errors
```

Beyond typo safety, Konditional gives you:

- **Typed values** — not just booleans, but strings, ints, doubles, enums, and custom types
- **Deterministic rollouts** — SHA-256 bucketing ensures same user → same bucket, always
- **Unified evaluation** — one rule DSL across your entire codebase, not per-domain evaluators
- **Explicit boundaries** — parse JSON configuration with validation; reject invalid updates before they affect production

Read the full argument: [Why Konditional Exists](docs/09-why-konditional.md)

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
object AppFeatures : FeatureContainer() {
    val darkMode by boolean(default = false) {
        rule { platforms(Platform.IOS) } returns true
        rule { rollout { 50.0 } } returns true
    }

    val apiEndpoint by string(default = "https://api.example.com") {
        rule { platforms(Platform.WEB) } returns "https://api-web.example.com"
    }

    val maxRetries by int(default = 3) {
        rule { versions { min(2, 0, 0) } } returns 5
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
val checkoutVersion by string(default = "v1") {
    rule { rollout { 33.0 } } returns "v2"
    rule { rollout { 66.0 } } returns "v3"
}

when (AppFlags.checkoutVersion.evaluate(ctx)) {
    "v1" -> v1Checkout()
    "v2" -> v2Checkout()
    "v3" -> v3Checkout()
}
```

### Enums

```kotlin
enum class Theme { LIGHT, DARK, AUTO }

val theme by enum(default = Theme.LIGHT) {
    rule { platforms(Platform.IOS) } returns Theme.DARK
}
```

### Custom structured values

```kotlin
@ConfigDataClass
data class RetryPolicy(
    val maxAttempts: Int = 3,
    val backoffMs: Double = 1000.0,
    val enabled: Boolean = true
) : KotlinEncodeable<ObjectSchema> {
    override val schema = schemaRoot {
        ::maxAttempts of { minimum = 1.0 }
        ::backoffMs of { minimum = 0.0 }
        ::enabled of {}
    }
}

val retryPolicy by custom(default = RetryPolicy()) {
    rule { platforms(Platform.WEB) } returns RetryPolicy(maxAttempts = 5, backoffMs = 2000.0)
}
```

---

## Deterministic Rollouts

Rollouts use SHA-256 bucketing for consistent, reproducible results:

```kotlin
val newFeature by boolean(default = false) {
    rule { rollout { 25.0 } } returns true
}
```

**Guarantees:**
- Same user + same flag + same percentage → same bucket
- Changing 10% → 20% doesn't reshuffle existing users
- Rollout decisions are reproducible from logs (`stableId` + flag key → deterministic bucket)

No random number generators. No modulo edge cases. No per-team rollout implementations with subtle differences.

---

## Remote Configuration

### Load configuration from JSON

```kotlin
val json = fetchRemoteConfig()

when (val result = SnapshotSerializer.fromJson(json)) {
    is ParseResult.Success -> Namespace.Global.load(result.value)
    is ParseResult.Failure -> {
        // Invalid JSON rejected, last-known-good remains active
        logError("Config parse failed: ${result.error.message}")
    }
}
```

**The boundary is explicit:** Parse failures don't crash your app or silently corrupt evaluation. Bad config is rejected; the previous working config stays active.

### Serialize current configuration

```kotlin
val snapshot = SnapshotSerializer.serialize(Namespace.Global.configuration)
persistToStorage(snapshot)
```

### Incremental updates (patches)

```kotlin
val patchJson = fetchPatch()

when (val result = SnapshotSerializer.applyPatchJson(currentConfig, patchJson)) {
    is ParseResult.Success -> Namespace.Global.load(result.value)
    is ParseResult.Failure -> logError("Patch failed: ${result.error.message}")
}
```

See [docs/06-remote-config.md](docs/06-remote-config.md) and [docs/08-persistence-format.md](docs/08-persistence-format.md) for details.

---

## Thread Safety

- **Atomic updates:** `Namespace.load()` swaps configuration atomically
- **Lock-free reads:** Evaluation reads a snapshot without blocking writers
- **No races:** Multiple threads can evaluate flags concurrently while configuration updates happen in the background

---

## Namespaces (Optional Isolation)

By default, all flags live in `Namespace.Global`. If you need isolated registries (e.g., per-team, per-domain), define your own:

```kotlin
sealed class AppDomain(id: String) : Namespace(id) {
    data object Account : AppDomain("account")
    data object Payments : AppDomain("payments")
}

object AccountFlags : FeatureContainer<AppDomain.Account>(AppDomain.Account) {
    val creditCheck by boolean(default = false)
}

object PaymentFlags : FeatureContainer<AppDomain.Payments>(AppDomain.Payments) {
    val stripeEnabled by boolean(default = true)
}
```

Each namespace has independent configuration lifecycle, registry, and serialization.

---

## Core Concepts

- **Namespace:** Isolation boundary with its own registry and configuration lifecycle
- **FeatureContainer:** Declares flags as delegated properties bound to a namespace
- **Context:** Runtime inputs for evaluation (`locale`, `platform`, `appVersion`, `stableId`)
- **Rules:** Typed criteria-to-value mappings (`platforms/locales/versions/rollout/extension`)
- **Snapshot/Patch:** JSON formats for persistence and incremental updates
- **Total evaluation:** No nulls—every flag returns its default if no rule matches

---

## Documentation

**Getting started:**
- [Quick Start Guide](docs/01-getting-started.md)
- [Core Concepts](docs/03-core-concepts.md)

**Features:**
- [Targeting & Rollouts](docs/04-targeting-rollouts.md)
- [Evaluation Semantics](docs/05-evaluation.md)
- [Remote Configuration](docs/06-remote-config.md)
- [Persistence Format](docs/08-persistence-format.md)

**Why Konditional:**
- [Why Konditional Exists](docs/09-why-konditional.md) — The compelling argument
- [Engineer Pitch Deck](docs/onboarding-flow.md)
- [vs. Enum/Boolean Capabilities](docs/onboarding-flow-legacy-enums.md)

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

Read more: [Real Problems Konditional Prevents](docs/09-why-konditional.md#real-problems-konditional-prevents)

---

## When to Use Konditional

**Choose Konditional when:**
- You want compile-time correctness for flag definitions and callsites
- You need typed values beyond booleans (variants, thresholds, structured config)
- You run experiments and need deterministic, reproducible rollouts
- You value consistency over bespoke per-domain solutions
- You have remote configuration and want explicit validation boundaries

**Konditional might not fit if:**
- You need vendor-hosted dashboards more than compile-time safety
- Your flags are fully dynamic with zero static definitions
- You're okay with process/tooling to prevent string key drift

---

## Examples

See the [ktor-demo](ktor-demo/) directory for a working example of Konditional in a Ktor application with:
- Custom context types
- Remote configuration loading
- Namespace isolation
- API integration

---

## License

MIT. See [LICENSE](LICENSE).
