# Why Konditional Exists

## The Problem

Feature flags and configuration systems seem simple until they bite you in production. Here's what actually happens:

### String-keyed systems create invisible failure modes

```kotlin
// Somewhere in onboarding code
val newFlow = flagClient.getBool("new_onboaring_flow", false)  // typo

// Somewhere in analytics
track("new_onboarding_flow_completed")  // correct spelling

// Somewhere in config JSON
{ "new_onboarding_flow": true }  // correct spelling
```

The typo ships. The flag never activates. Your A/B test runs for weeks with 0% treatment. Nobody notices until you check
the results.

**String keys fail silently.** The compiler can't help you. Your IDE can't help you. You find out in production or in
post-mortems.

### Boolean-only systems turn into boolean matrices

You start clean:

```kotlin
enum class Capability {
    NEW_CHECKOUT
}
```

Then you need variants:

```kotlin
enum class Capability {
    NEW_CHECKOUT,
    NEW_CHECKOUT_V2,
    NEW_CHECKOUT_V3,
    CHECKOUT_FAST_PATH,
    CHECKOUT_STRIPE_INTEGRATION
}

// And now your code looks like:
if (isEnabled(NEW_CHECKOUT) && !isEnabled(NEW_CHECKOUT_V2)) {
    // original new checkout
} else if (isEnabled(NEW_CHECKOUT_V2) && !isEnabled(CHECKOUT_FAST_PATH)) {
    // v2 without fast path
} else if (isEnabled(NEW_CHECKOUT_V3) || isEnabled(CHECKOUT_FAST_PATH)) {
    // ... which logic wins?
}
```

**Boolean-only forces you to encode variants as control flow.** Every combination requires explicit handling. Testing
becomes exponential. Bugs hide in interactions.

### Each team reimplements ramp-up logic differently

The account team implements ramp-ups with modulo arithmetic. The payments team uses random number generators. The growth
team copies some Stack Overflow answer. Each has different edge cases:

- Do they bucket consistently for the same user?
- Can you replay decisions deterministically in logs?
- Does the same user get the same experience on web and mobile?
- What happens when you change the percentage?

**Inconsistent ramp-up semantics create A/B testing landmines.** Results become untrustworthy. Debugging requires
reading five different implementations.

### Type safety disappears at the boundary

```kotlin
// You define this
val maxRetries: Int = flagClient.getInt("max_retries", 3)

// Someone deploys this
{ "max_retries": "five" }

// Production gets this
maxRetries = 0  // or throws, or returns default, depends on the SDK
```

**Runtime configuration breaks compile-time contracts.** JSON is stringly-typed. Your code expects ints. The gap causes
incidents.

---

## What Konditional Does Differently

Konditional makes three structural commitments:

1. **Flags are properties, not strings** — keys are bound at compile-time
2. **Types flow from definitions to callsites** — no type casting or runtime coercion
3. **One evaluation semantics for the entire codebase** — centralized, deterministic, testable

```kotlin
enum class CheckoutVariant { CLASSIC, OPTIMIZED, EXPERIMENTAL }

object AppFlags : Namespace("app") {
    val checkoutVariant by enum<CheckoutVariant, Context>(default = CheckoutVariant.CLASSIC) {
        rule(CheckoutVariant.OPTIMIZED) { platforms(Platform.IOS, Platform.ANDROID) }
        rule(CheckoutVariant.EXPERIMENTAL) { rampUp { 50.0 } }
    }

    val maxRetries by integer<Context>(default = 3) {
        rule(5) { platforms(Platform.WEB) }
    }
}

// Usage
val checkoutVariant: CheckoutVariant = AppFlags.checkoutVariant(ctx)  // typed, cannot be wrong
val retries: Int = AppFlags.maxRetries(ctx)                           // typed, cannot be wrong
```

### What you get

**Typos become compile errors:**

```kotlin
// This will error
AppFlags.NEW_ONBOARING_FLOW  // doesn't compile
```

**Type mismatches become compile errors:**

```kotlin
// This will error
val retries: String = AppFlags.maxRetries(ctx)  // doesn't compile
```

**Variants are values, not boolean matrices:**

```kotlin
when (AppFlags.checkoutVariant(ctx)) {
    CheckoutVariant.CLASSIC -> classicCheckout()
    CheckoutVariant.OPTIMIZED -> optimizedCheckout()
    CheckoutVariant.EXPERIMENTAL -> experimentalCheckout()
}
```

**Ramp-ups are deterministic and consistent:**

```kotlin
// Same user, same flag, same percentage → same bucket
// SHA-256("$salt:$flagKey:${stableId.hexId}") determines bucket
// No random numbers, no modulo edge cases, reproducible in logs
```

**Configuration boundaries are explicit:**

```kotlin
when (val result = AppFlags.fromJson(remoteConfig)) {
    is ParseResult.Success -> Unit // loaded into AppFlags
    is ParseResult.Failure -> {
        // Invalid JSON rejected, last-known-good remains active
        // This will error
        logError("Config parse failed: ${result.error}")
    }
}
```

If the payload references unknown or not-yet-registered feature keys, parsing fails (or can be configured to skip
unknown keys during migrations).

---

## Comparison to Alternatives

| Aspect                   | String-keyed SDKs                            | Enum + boolean capabilities                          | Konditional                                                      |
|--------------------------|----------------------------------------------|------------------------------------------------------|------------------------------------------------------------------|
| **Typo safety**          | Runtime failure (silent or crash)            | Compile-time (enum typos caught)                     | Compile-time (property references)                               |
| **Type safety**          | Runtime coercion (often unsafe)              | Boolean only                                         | Compile-time types (Boolean/String/Int/Double/Enum/custom)       |
| **Variants**             | Supported but runtime-typed                  | Requires multiple booleans + control flow            | First-class typed values                                         |
| **Ramp-up logic**        | SDK-dependent, varies                        | Reimplemented per domain/team                        | Centralized, deterministic (SHA-256 bucketing)                   |
| **Evaluation semantics** | SDK-defined, often opaque                    | Ad-hoc per evaluator (account, card, merchant, etc.) | Single DSL with specificity ordering                             |
| **Configuration drift**  | Implicit boundary, often fails silently      | Ad-hoc validation per evaluator                      | Explicit `ParseResult` boundary, rejects invalid JSON            |
| **Null/missing values**  | Depends on SDK (null, exception, or default) | Depends on implementation                            | Total evaluation (defaults required, no null returns)            |
| **Testing**              | Mock SDK or replay config snapshots          | Mock evaluators or stub booleans                     | Evaluate against typed contexts (deterministic, no mocks needed) |
| **Consistency**          | Depends on SDK discipline and process        | Depends on how many evaluators you maintain          | One rule DSL, one evaluation engine, one set of semantics        |

---

## Why This Matters

### For engineers writing features

- **Autocomplete works:** Your IDE shows available flags. You can't reference flags that don't exist.
- **Types flow:** Return types are known at compile-time. No casting, no runtime surprises.
- **Refactoring is safe:** Rename a flag property and all callsites update. No grep-and-hope.

### For teams running experiments

- **Ramp-ups are reproducible:** Same user always gets same bucket. You can replay decisions from logs.
- **Percentages are stable:** Increasing 10% → 20% only adds users for the same `(stableId, flagKey, salt)`.
- **Targeting is consistent:** Platform/locale/version targeting works the same across all flags.

### For systems that grow

- **No boolean explosion:** Variants are values, not combinatorial boolean checks.
- **No evaluation logic duplication:** One DSL, one evaluation engine, shared across all domains.
- **Clear boundaries:** Compile-time correctness for definitions. Runtime validation for remote config. No blurred
  lines.

---

## When Konditional Fits

Choose Konditional when:

- **You want compile-time correctness** for flag definitions and callsites
- **You need typed values** beyond on/off booleans (variants, thresholds, configuration)
- **You value consistency** over bespoke per-domain solutions
- **You run experiments** and need deterministic, reproducible ramp-ups
- **You have remote configuration** and want explicit validation boundaries

Konditional might not fit if:

- **You need vendor-hosted dashboards** more than you need compile-time safety
- **Your flags are fully dynamic** with zero static definitions (though you can still use Konditional for the static
  subset)
- **You're okay with process and tooling** to prevent string key drift (code review, linters, integration tests)

---

## The Three Models at a Glance

| Approach     | Key/Definition    | Evaluation       | Result          | Main Trade-off                               |
|--------------|-------------------|------------------|-----------------|----------------------------------------------|
| String-based | `"flag-name"`     | `getVal/...`     | Value (runtime) | Flexibility vs. runtime safety               |
| Enum-bool    | `enum Capability` | `isEnabled(ctx)` | Boolean         | Compile-time keys vs. boolean-only values    |
| Konditional  | Compiled property | Rules + default  | Typed value     | Static definitions vs. dynamic configuration |

---

## Real Problems Konditional Prevents

### Production incident: Type coercion

A string-keyed SDK returns `0` when parsing `"max_retries": "disabled"` from JSON. The service retries 0 times. All
requests fail immediately. Incident lasts 45 minutes.

**With Konditional:** The JSON parse fails at the boundary. `ParseResult.Failure` is logged. Last-known-good
configuration remains active. No incident.

### Experiment contamination: Inconsistent bucketing

Two teams implement ramp-ups with different hashing. Same user gets opposite buckets for related features. A/B test
results are polluted. Experiment analysis is invalid.

**With Konditional:** All ramp-ups use the same deterministic SHA-256 bucketing. Same user, same flag, same bucket.
Results are clean.

### Maintenance burden: Boolean explosion

A feature has 5 boolean flags for variants. Testing requires 2^5 = 32 combinations. Most combinations are undefined.
Bugs hide in interactions. Engineers avoid touching the code.

**With Konditional:** One flag, one typed value, explicit variants. Testing covers defined cases. Code is readable.

### Configuration drift: Silent deployment

Someone changes `"new_onboarding_flow"` to `"new_onboarding_experience"` in the remote config. Half the codebase uses
the old key, half uses the new key. Ramp-up percentage splits across both. Metrics are nonsense.

**With Konditional:** Flag keys are derived from property names. Renaming the property updates all callsites (
compile-time). If remote config still sends the old key, loading fails (or emits a warning if configured to skip unknown
keys) instead of silently splitting traffic.

---

## Migration Path

If you're coming from a boolean capability system:

1. **Mirror existing flags** as properties on a `Namespace`:
   ```kotlin
   object Features : Namespace("app") {
       val featureX by boolean<Context>(default = false)
   }
   ```

2. **Centralize evaluation logic** into rules:
   ```kotlin
   object Features : Namespace("app") {
       val featureX by boolean<Context>(default = false) {
           rule(true) { platforms(Platform.WEB) }
           rule(true) { rampUp { 25.0 } }
       }
   }
   ```

3. **Replace boolean matrices** with typed values where variants exist:
   ```kotlin
   // Before: CHECKOUT_V1, CHECKOUT_V2, CHECKOUT_V3 (3 booleans)
   // After:
   enum class CheckoutVersion { V1, V2, V3 }
   object CheckoutFlags : Namespace("checkout") {
       val checkoutVersion by enum<CheckoutVersion, Context>(default = CheckoutVersion.V1) {
           rule(CheckoutVersion.V2) { rampUp { 33.0 } }
           rule(CheckoutVersion.V3) { rampUp { 66.0 } }
       }
   }
   ```

4. **Introduce namespaces** only if you need independent registries:
   ```kotlin
   sealed class Domain(id: String) : Namespace(id) {
       data object Account : Domain("account")
       data object Payments : Domain("payments")
   }
   ```

5. **Add remote config** with explicit boundaries:
   ```kotlin
   when (val result = Features.fromJson(json)) {
       is ParseResult.Success -> Unit // loaded into Features
       is ParseResult.Failure -> keepLastKnownGood()
   }
   ```

---

## Summary

Feature flags and configuration aren't just "nice to have" features. They're load-bearing infrastructure. When they
fail, they fail at scale, in production, with user impact.

Konditional exists because **stringly-typed systems cause production incidents**, **boolean-only systems create
maintenance nightmares**, and **inconsistent evaluation semantics make experiments untrustworthy**.

The solution isn't more process or better code review. The solution is structural: bind types at compile-time,
centralize evaluation semantics, and draw explicit boundaries between static definitions and dynamic configuration.

That's what Konditional does.
