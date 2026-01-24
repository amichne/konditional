---
title: Golden Path Example
---

```kotlin id="greeting_name"
val name = "Kotlin"
val greeting = "Hi, " + name /*hover:id=greeting_name*/
println(greeting)
// ^ hover:id=call_println
```


# Golden Path: End-to-End Feature Flag

Complete, runnable example showing Konditional's core workflow from definition to evaluation.

## What This Shows

- Namespace and feature definition
- Multiple rule types (targeting, ramp-up)
- Context construction
- Evaluation with type safety
- Explainable results for debugging

## Complete Code

```kotlin id="golden-path"
// File: src/main/kotlin/com/example/GoldenPath.kt
package com.example

import io.amichne.konditional.context.*
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.boolean
import io.amichne.konditional.core.dsl.enum
import io.amichne.konditional.api.evaluate
import io.amichne.konditional.api.explain

// Step 1: Define types for typed features
enum class CheckoutVariant { CLASSIC, FAST_PATH, EXPERIMENTAL }

// Step 2: Create namespace (feature registry)
object AppFeatures : Namespace("app") {

    // Boolean feature with platform targeting
    val darkMode by boolean<Context>(default = false) {
        rule(true) {
            platforms(Platform.IOS)
            note("iOS users get dark mode")
        }
        rule(true) {
            rampUp { 10.0 }
            note("10% gradual rollout to everyone")
        }
    }

    // Enum feature with multiple variants
    val checkoutVariant by enum<CheckoutVariant, Context>(
        default = CheckoutVariant.CLASSIC
    ) {
        rule(CheckoutVariant.FAST_PATH) {
            platforms(Platform.IOS)
            versions { min(2, 0, 0) }
            rampUp { 25.0 }
            note("iOS 2.0+ users, 25% rollout")
        }
        rule(CheckoutVariant.EXPERIMENTAL) {
            platforms(Platform.ANDROID)
            rampUp { 5.0 }
            note("Android users, 5% rollout")
        }
    }
}

// Step 3: Build contexts from runtime data
fun buildContext(userId: String, platform: Platform, version: Version): Context {
    return Context(
        locale = AppLocale.UNITED_STATES,
        platform = platform,
        appVersion = version,
        stableId = StableId.of(userId)
    )
}

// Step 4: Evaluate features
fun main() {
    // iOS user on v2.1.0
    val iosUserCtx = buildContext(
        userId = "user-123",
        platform = Platform.IOS,
        version = Version.of(2, 1, 0)
    )

    // Evaluation returns typed values (never null)
    val darkModeEnabled: Boolean = AppFeatures.darkMode.evaluate(iosUserCtx)
    val checkoutVariant: CheckoutVariant = AppFeatures.checkoutVariant.evaluate(iosUserCtx)

    println("iOS User:")
    println("  Dark mode: $darkModeEnabled")           // true (platform match)
    println("  Checkout: $checkoutVariant")            // FAST_PATH or CLASSIC (25% ramp)

    // Android user on v1.5.0
    val androidUserCtx = buildContext(
        userId = "user-456",
        platform = Platform.ANDROID,
        version = Version.of(1, 5, 0)
    )

    val darkModeAndroid = AppFeatures.darkMode.evaluate(androidUserCtx)
    val checkoutAndroid = AppFeatures.checkoutVariant.evaluate(androidUserCtx)

    println("\nAndroid User:")
    println("  Dark mode: $darkModeAndroid")           // false or true (10% ramp)
    println("  Checkout: $checkoutAndroid")            // EXPERIMENTAL or CLASSIC (5% ramp)

    // Step 5: Debugging with explain()
    val result = AppFeatures.darkMode.explain(iosUserCtx)
    println("\nDebug iOS dark mode:")
    println("  Value: ${result.value}")
    println("  Decision: ${result.decision}")
    result.bucketInfo?.let {
        println("  Bucket: ${it.bucket} (threshold: ${it.percentage}%)")
    }
}
```

## Run This Example

1. **Create project**:
```bash
mkdir konditional-example && cd konditional-example
gradle init --type kotlin-application
```

2. **Add dependency** to `build.gradle.kts`:
```kotlin
dependencies {
    implementation("io.amichne:konditional-core:0.1.0")
}
```

3. **Copy code** above to `src/main/kotlin/com/example/GoldenPath.kt`

4. **Run**:
```bash
./gradlew run
```

**Expected output**:
```
iOS User:
  Dark mode: true
  Checkout: FAST_PATH

Android User:
  Dark mode: false
  Checkout: CLASSIC

Debug iOS dark mode:
  Value: true
  Decision: Rule(matched=...)
  Bucket: 4523 (threshold: 10.0%)
```

## Key Concepts Demonstrated

| Concept | Location | What It Shows |
|---------|----------|---------------|
| **Namespace** | `object AppFeatures : Namespace("app")` | Isolation boundary for features |
| **Feature definition** | `val darkMode by boolean<Context>(...)` | Typed property with default and rules |
| **Rule targeting** | `platforms(Platform.IOS)` | Criteria-based matching |
| **Ramp-up** | `rampUp { 10.0 }` | Percentage rollout with deterministic bucketing |
| **Specificity** | Rule order | Higher specificity rules evaluated first |
| **Type safety** | `val enabled: Boolean = ...` | Compile-time type propagation |
| **Total evaluation** | `default = false` | Always returns value, never null |
| **Explainability** | `explain(ctx)` | Debugging decision path |

## Guarantees Demonstrated

**Type Safety**:
- **Guarantee**: Feature access and return types are compile-time safe
- **Mechanism**: Property delegation with generic types
- **Evidence**: `val darkModeEnabled: Boolean` — type mismatch would fail compilation

**Deterministic Ramp-Ups**:
- **Guarantee**: Same (stableId, featureKey, salt) → same bucket
- **Mechanism**: SHA-256 bucketing in `RampUpBucketing`
- **Evidence**: Run example multiple times with same userId — same result

**Total Evaluation**:
- **Guarantee**: Every evaluation returns a value
- **Mechanism**: Required `default` value
- **Evidence**: No null checks needed, exhaustive `when` on enums

## Next Steps

- [Guide: Install and Set Up](/guides/install-and-setup) — Set up your own project
- [Learn: Core Primitives](/learn/core-primitives) — Understand building blocks
- [Learn: Evaluation Model](/learn/evaluation-model) — How rules are ordered
- [Guide: Test Features](/guides/test-features) — Write tests for this example
