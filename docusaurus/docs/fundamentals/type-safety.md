# Type Safety in Konditional

Konditional provides compile-time type safety for statically-defined flags and runtime validation for configuration loaded from JSON.

---

## What Type Safety Gives You

### 1. Features are Properties, Not Strings

```kotlin
object AppFeatures : Namespace("app") {
    val darkMode by boolean<Context>(default = false)
    val maxRetries by integer<Context>(default = 3)
}

// Usage
val enabled: Boolean = AppFeatures.darkMode.evaluate(context)  // ✓ Typed
val retries: Int = AppFeatures.maxRetries.evaluate(context)     // ✓ Typed
```

**Benefits:**

- **Typos caught at compile-time:** `AppFeatures.darkMood` won't compile
- **IDE autocomplete works:** Your editor shows available flags
- **Refactoring is safe:** Rename a property and all usages update

### 2. Return Types are Known

```kotlin
val timeout by double<Context>(default = 30.0) {
    rule(45.0) { platforms(Platform.ANDROID) }
}

// Compiler knows this returns Double
val timeoutValue: Double = timeout.evaluate(context)  // ✓ Correct
val timeoutValue: String = timeout.evaluate(context)  // X Compile error
```

**You never cast or coerce types.** The type flows from definition to evaluation.

### 3. Rule Types Must Match

```kotlin
val maxRetries by integer<Context>(default = 3) {
    rule(5) { android() }        // ✓ Int matches
    rule("five") { ios() }       // X Compile error: String != Int
}
```

**Invalid rules don't compile.** You can't accidentally return the wrong type.

### 4. Defaults are Required

```kotlin
val feature by boolean<Context>(default = false)  // ✓ Has default
val feature by boolean<Context>()                 // X Compile error: no default
```

**Evaluation is total.** Every feature always returns a value. No nulls, no exceptions (from evaluation).

### 5. Context Types are Enforced

```kotlin
interface PremiumContext : Context {
    val subscriptionTier: SubscriptionTier
}

val premiumFeature by boolean<PremiumContext>(default = false) {
    rule(true) { extension { subscriptionTier == SubscriptionTier.ENTERPRISE } }
}

// Usage
val premiumCtx: PremiumContext = buildPremiumContext()
premiumFeature.evaluate(premiumCtx)  // ✓ Correct context type

val basicCtx: Context = Context(...)
premiumFeature.evaluate(basicCtx)    // X Compile error: wrong context type
```

**Context extensions are type-safe.** You can't evaluate a feature with the wrong context.

---

## The Boundary: Compile-Time vs Runtime

### Compile-Time Safety (Statically-Defined Flags)

When you define flags in Kotlin code, the compiler guarantees:

- Property names map to feature keys
- Return types match between definition and usage
- Rules return the correct types
- Defaults exist for all features
- Context types are correct

### Runtime Validation (JSON Configuration)

When you load configuration from JSON, **the compiler cannot help**:

```kotlin
// Statically defined
object AppFeatures : Namespace("app") {
    val maxRetries by integer<Context>(default = 3)
}

// Loaded from JSON
val json = """{ "maxRetries": "five" }"""  // Wrong type!

when (val result = NamespaceSnapshotLoader(AppFeatures).load(json)) {
    is ParseResult.Success -> Unit // Valid JSON
    is ParseResult.Failure -> {
        // Invalid JSON rejected at boundary
        logError("Parse failed: ${result.error}")
        // Last-known-good configuration remains active
    }
}
```

**Key insight:** Konditional doesn't pretend JSON is type-safe. Instead, it:

1. **Validates at the boundary** with explicit `ParseResult`
2. **Rejects invalid JSON** before affecting production
3. **Keeps last-known-good** when validation fails

---

## What Can Go Wrong (and How Konditional Handles It)

### Typo in Flag Name

**String-keyed systems:**

```kotlin
val enabled = flagClient.getBool("new_onboaring_flow", false)  // Typo ships
```

→ Silent failure. Flag never activates.

**Konditional:**

```kotlin
AppFeatures.newOnboaringFlow  // Doesn't compile
```

→ Caught at compile-time.

### Type Mismatch

**String-keyed systems:**

```kotlin
val retries: Int = flagClient.getInt("max_retries", 3)
// JSON: { "max_retries": "five" }
// Runtime: retries = 0 (or exception, SDK-dependent)
```

→ Production incident.

**Konditional:**

```kotlin
val json = """{ "maxRetries": "five" }"""
when (val result = NamespaceSnapshotLoader(AppFeatures).load(json)) {
    is ParseResult.Failure -> // Invalid type caught, last-known-good remains
}
```

→ Invalid JSON rejected, no incident.

### Missing Default

**String-keyed systems:**

```kotlin
val timeout = flagClient.getDouble("timeout", null)  // Nullable!
timeout?.let { setTimeoutMs(it) }  // Null checks everywhere
```

→ Null propagation through codebase.

**Konditional:**

```kotlin
val timeout by double<Context>(default = 30.0)  // Default required
val value: Double = timeout.evaluate(ctx)        // Never null
```

→ Evaluation is total. No nulls.

---

## Practical Implications

### For Day-to-Day Development

1. **Use IDE autocomplete** to discover flags
2. **Rely on the compiler** for refactoring
3. **Don't cast or coerce types** — they flow automatically
4. **Handle `ParseResult` explicitly** when loading JSON

### For Code Reviews

1. **Check that defaults make sense** (required, non-optional)
2. **Verify rule types match** (compiler enforces this anyway)
3. **Ensure `ParseResult` failures are handled** (log, alert, fallback)

### For Testing

1. **Test evaluation with typed contexts** — no mocks needed
2. **Test `ParseResult.Failure` cases** for invalid JSON
3. **Test that rules return correct types** (compiler helps, but test edge cases)

---

## Summary

Konditional's type safety has two parts:

1. **Compile-time guarantees** for statically-defined flags:
  - Property names = feature keys
  - Return types flow from definition to usage
  - Rules match feature types
  - Defaults required, evaluation total
  - Context types enforced

2. **Runtime validation** for JSON configuration:
  - Explicit `ParseResult` boundary
  - Invalid JSON rejected before affecting production
  - Last-known-good configuration preserved

**You get compile-time safety where possible, explicit validation where necessary.**

---

## Next Steps

- [Configuration Lifecycle](/fundamentals/configuration-lifecycle) — How configuration flows from JSON to evaluation
- [Type Safety Boundaries (Theory)](/theory/type-safety-boundaries) — Deep dive into how type safety is implemented
- [Production Operations](/production-operations/failure-modes) — Handling invalid configuration in production
