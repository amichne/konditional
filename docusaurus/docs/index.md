---
slug: /
---

# What is Konditional?

Konditional is a compile-time safe feature flag library for Kotlin that treats flags as typed properties instead of
runtime strings, and makes configuration a first-class, verifiable contract.

## The Problem

Configuration and feature flags are not “just another component.” They are your production control plane, your
experimentation engine, your insight driver, and your blast-radius minimizer. When everything else goes wrong, this is
the system you rely on to react safely. That means *certainty* is not a nice-to-have. It is a hard requirement. There is
no other system where you can afford silent failure less.

:::danger Key argument
If you cannot trust your configuration and experimentation engine with absolute certainty, you do not have real control
in production. That is not a tolerable risk.
:::

Feature flags and configuration systems are often deceptively simple -- until they bite you in production:

### String-keyed systems fail silently

Somewhere in onboarding code
```kotlin
val newFlow = flagClient.getBool("new_onboaring_flow", false)  // typo
```


Somewhere in config JSON
```json
{ "new_onboarding_flow": true }  // correct spelling
```

The typo ships. The flag never activates. Your A/B test runs with 0% treatment. You find out in a post-mortem.

**String keys fail silently.** The compiler can't help you. Your IDE can't help you. And the worst part is you often
don’t even know you are wrong. If you observe one failure, you can’t know the full blast radius or the unseen drift.
That is an intolerable risk for a control plane.

### Boolean-only systems turn into boolean matrices

```kotlin
enum class Capability {
    NEW_CHECKOUT,
    NEW_CHECKOUT_V2,
    NEW_CHECKOUT_V3,
    CHECKOUT_FAST_PATH
}

// Your code becomes:
if (isEnabled(NEW_CHECKOUT) && !isEnabled(NEW_CHECKOUT_V2)) {
    // original new checkout
} else if (isEnabled(NEW_CHECKOUT_V2) && !isEnabled(CHECKOUT_FAST_PATH)) {
    // v2 without fast path
}
```

**Boolean-only forces you to encode variants as control flow.** Testing becomes exponential. Bugs hide in interactions.
Even moderately complex mappings explode into a fragile web of conditionals. That’s not a scaling path; it’s a failure
mode.

### Type safety disappears at the boundary

You define this
```kotlin
val maxRetries: Int = flagClient.getInt("max_retries", 3)
```

Someone deploys this
```json5
{ "max_retries": "5" }
```

Production gets this
```kotlin
maxRetries = 0  // or throws, or returns default (SDK-dependent)
```

**Runtime configuration breaks compile-time contracts.** The gap causes incidents.

---

## What Konditional Does

Konditional makes three structural commitments:

1. **Flags are properties, not strings** — keys bound at compile-time
2. **Types flow from definitions to callsites** — no runtime coercion
3. **One evaluation semantics** — centralized, deterministic, testable

```kotlin
enum class CheckoutVariant { CLASSIC, OPTIMIZED, EXPERIMENTAL }

object AppFlags : Namespace("app") {
  val checkoutVariant by enum<CheckoutVariant, Context>(default = CheckoutVariant.CLASSIC) {
    rule(CheckoutVariant.OPTIMIZED) { ios() }
    rule(CheckoutVariant.EXPERIMENTAL) { rampUp { 50.0 } }
  }

  val maxRetries by integer<Context>(default = 3) {
    rule(5) { android() }
  }
}

// Usage
val variant: CheckoutVariant = AppFlags.checkoutVariant.evaluate(ctx)  // typed
val retries: Int = AppFlags.maxRetries.evaluate(ctx)                   // typed
```

### What You Get

**Typos become compile errors:**

```kotlin
AppFlags.NEW_ONBOARING_FLOW  // doesn't compile
```

**Type mismatches become compile errors:**

```kotlin
val retries: String = AppFlags.maxRetries.evaluate(ctx)  // doesn't compile
```

**Variants are values, not boolean matrices:**

```kotlin
when (AppFlags.checkoutVariant.evaluate(ctx)) {
  CheckoutVariant.CLASSIC -> classicCheckout()
  CheckoutVariant.OPTIMIZED -> optimizedCheckout()
  CheckoutVariant.EXPERIMENTAL -> experimentalCheckout()
}
```

**Ramp-ups are deterministic:**

```kotlin
// Same user, same flag → same bucket
// SHA-256("$salt:$flagKey:${stableId.hexId}") determines bucket
// Reproducible in logs, no random numbers
```

**Configuration boundaries are explicit:**

```kotlin
when (val result = NamespaceSnapshotLoader(AppFlags).load(remoteConfig)) {
  is ParseResult.Success -> Unit // loaded into AppFlags
  is ParseResult.Failure -> {
    // Invalid JSON rejected, last-known-good remains active
    logError("Config parse failed: ${result.error}")
  }
}
```

---

## Comparison to Alternatives

| Aspect             | String-keyed SDKs                 | Enum + boolean                   | Konditional                     |
|--------------------|-----------------------------------|----------------------------------|---------------------------------|
| **Typo safety**    | Runtime failure (silent or crash) | Compile-time                     | Compile-time                    |
| **Type safety**    | Runtime coercion (often unsafe)   | Boolean only                     | Compile-time types              |
| **Variants**       | Runtime-typed                     | Multiple booleans + control flow | First-class typed values        |
| **Ramp-up logic**  | SDK-dependent                     | Per-team reimplementation        | Centralized, deterministic      |
| **Evaluation**     | SDK-defined, opaque               | Ad-hoc per evaluator             | Single DSL with specificity     |
| **Invalid config** | Fails silently or crashes         | Depends on implementation        | Explicit `ParseResult` boundary |
| **Testing**        | Mock SDK or replay snapshots      | Mock evaluators                  | Evaluate against typed contexts |

---

## When Konditional Fits

**Choose Konditional when:**

- You want compile-time correctness for flag definitions and callsites
- You need typed values beyond on/off booleans (variants, thresholds, configuration)
- You value consistency over bespoke per-domain solutions
- You run experiments and need deterministic, reproducible ramp-ups
- You have remote configuration and want explicit validation boundaries

**Konditional might not fit if:**

- You need vendor-hosted dashboards more than compile-time safety
- Your flags are fully dynamic with zero static definitions
- You're okay with process and tooling to prevent string key drift

---

## Real Problems Konditional Prevents

### Production incident: Type coercion

A string-keyed SDK returns `0` when parsing `"max_retries": "disabled"`. Service retries 0 times. All requests fail
immediately.

**With Konditional:** Parse fails at boundary. `ParseResult.Failure` logged. Last-known-good remains active. No
incident.

### Experiment contamination: Inconsistent bucketing

Two teams implement ramp-ups with different hashing. Same user gets opposite buckets. A/B test results polluted.

**With Konditional:** All ramp-ups use deterministic SHA-256 bucketing. Same user, same bucket. Clean results.

### Maintenance burden: Boolean explosion

Feature has 5 boolean flags for variants. Testing requires 32 combinations. Most undefined. Bugs hide in interactions.

**With Konditional:** One flag, typed value, explicit variants. Testing covers defined cases. Code readable.

---

## Migration Path

Coming from a boolean capability system:

1. **Mirror existing flags** as properties:
   ```kotlin
   object Features : Namespace("app") {
       val featureX by boolean<Context>(default = false)
   }
   ```

2. **Centralize evaluation** into rules:
   ```kotlin
   val featureX by boolean<Context>(default = false) {
       rule(true) { android() }
       rule(true) { rampUp { 25.0 } }
   }
   ```

3. **Replace boolean matrices** with typed values:
   ```kotlin
   // Before: CHECKOUT_V1, CHECKOUT_V2, CHECKOUT_V3 (3 booleans)
   enum class CheckoutVersion { V1, V2, V3 }
   val checkoutVersion by enum<CheckoutVersion, Context>(default = V1) {
       rule(V2) { rampUp { 33.0 } }
       rule(V3) { rampUp { 66.0 } }
   }
   ```

4. **Add remote config** with explicit boundaries:
   ```kotlin
   when (val result = NamespaceSnapshotLoader(Features).load(json)) {
       is ParseResult.Success -> Unit
       is ParseResult.Failure -> keepLastKnownGood()
   }
   ```

See the [Migration Guide](./reference/migration-guide.md) for detailed patterns.

---

## Summary

Feature flags aren't "nice to have" features. They're load-bearing infrastructure. When they fail, they fail at scale,
in production, with user impact.

Konditional exists because **stringly-typed systems cause production incidents**, **boolean-only systems create
maintenance nightmares**, and **inconsistent
evaluation semantics make experiments untrustworthy**.

The solution is structural: bind types at compile-time, centralize evaluation semantics, and draw explicit boundaries
between static definitions and dynamic
configuration.

## Next Steps

- [Installation](./getting-started/installation) — Add Konditional to your project
- [Your First Feature](./getting-started/your-first-flag) — Define and evaluate your first feature flag
- [Core Concepts](./learn/core-primitives) — Understand the foundational types
- [Verified Design Synthesis](./theory/verified-synthesis) — Cross-document, code-verified invariants and trade-offs
