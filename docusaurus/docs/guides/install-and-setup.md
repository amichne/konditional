---
title: Install and Set Up Konditional
---

# Install and Set Up Konditional

## Summary

Install Konditional Core, define your first feature flag, and evaluate it in code. This guide takes 5-10 minutes and gives you a working feature flag with compile-time type safety.

**When to use:** You're starting a new project or adding Konditional to an existing Kotlin codebase.

## Prerequisites

- **Kotlin**: 1.9.0 or later
- **JVM**: 11 or later
- **Build tool**: Gradle (Kotlin DSL or Groovy)
- **Permissions**: Ability to modify `build.gradle.kts` and create Kotlin files

## Happy Path

### Step 1: Add Dependency

Add Konditional Core to your `build.gradle.kts`:

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.amichne:konditional-core:0.1.0") // Replace with latest version
}
```

**Expected output**: Gradle sync succeeds, dependency resolves.

**Evidence**: `konditional-core/build.gradle.kts` declares publication coordinates.

### Step 2: Define a Namespace and Feature

Create a Kotlin file for your features:

```kotlin
// src/main/kotlin/com/example/AppFeatures.kt
import io.amichne.konditional.context.*
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.boolean

object AppFeatures : Namespace("app") {
    val darkMode by boolean<Context>(default = false) {
        rule(true) { platforms(Platform.IOS) }
    }
}
```

**What this does**:
- `Namespace("app")`: Creates an isolated registry for features
- `darkMode by boolean<Context>(default = false)`: Declares a boolean feature with default `false`
- `rule(true) { platforms(Platform.IOS) }`: iOS users get `true`, others get default

**Evidence**: `konditional-core/src/main/kotlin/io/amichne/konditional/core/Namespace.kt:71`

### Step 3: Evaluate the Feature

```kotlin
// src/main/kotlin/com/example/Main.kt
fun main() {
    val ctx = Context(
        locale = AppLocale.UNITED_STATES,
        platform = Platform.IOS,
        appVersion = Version.of(2, 0, 0),
        stableId = StableId.of("user-123")
    )

    val enabled: Boolean = AppFeatures.darkMode.evaluate(ctx)
    println("Dark mode enabled: $enabled") // Output: Dark mode enabled: true
}
```

**Expected output**: `Dark mode enabled: true` (because platform is IOS)

**Evidence**: `konditional-core/src/main/kotlin/io/amichne/konditional/api/FeatureEvaluation.kt:32`

## Annotated Example

See the complete working example in [Golden Path Example](/examples/golden-path).

```kotlin
// Complete example with imports and usage
import io.amichne.konditional.context.*
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.boolean

// 1. Define namespace (registry for features)
object AppFeatures : Namespace("app") {
    // 2. Declare feature with type, default, and rules
    val darkMode by boolean<Context>(default = false) {
        rule(true) { platforms(Platform.IOS) }          // Rule 1: iOS → true
        rule(true) { rampUp { 10.0 } }                   // Rule 2: 10% rollout → true
    }
}

fun checkDarkMode(userId: String, platform: Platform): Boolean {
    // 3. Build context from runtime inputs
    val ctx = Context(
        locale = AppLocale.UNITED_STATES,
        platform = platform,
        appVersion = Version.of(2, 0, 0),
        stableId = StableId.of(userId)
    )

    // 4. Evaluate (returns Boolean, never null)
    return AppFeatures.darkMode.evaluate(ctx)
}
```

**File path**: `docusaurus/docs/examples/golden-path.md`
**Command to run**: Copy to `src/main/kotlin` and run `./gradlew run`

## Options

| Option | Purpose | Default | When to Change |
|--------|---------|---------|----------------|
| `default` value | Fallback when no rules match | Required | Set to safe production value (usually `false` for booleans) |
| Rule order | Determines evaluation precedence | By specificity | Only matters for rules with same specificity |
| `salt("...")` | Bucketing input for ramp-ups | `"default"` | When restarting experiments with new sample |
| Context type | Defines available targeting | `Context` | When adding custom business logic fields |

## Requirements

- **Default value is required**: Cannot be `null` or omitted.
  - Fix: Always provide `default = <value>`
  - Why: [Total evaluation guarantee](/learn/evaluation-model#total-evaluation)

- **StableId must be persistent**: Don't use session IDs or random values for ramp-ups.
  - Fix: Use database user ID or device identifier
  - Why: Non-persistent IDs cause users to switch buckets on every evaluation

## Performance & Security Notes

**Performance**:
- Evaluation cost: O(R) where R = number of rules (typically < 10)
- No allocations in hot path (value class usage)
- Thread-safe reads (atomic snapshots)

**Security**:
- No reflection or code generation at runtime
- StableId hashing is deterministic (SHA-256), not cryptographically secure
- Remote configuration should be validated before load (see [Load Remote Config](/guides/load-remote-config))

## Troubleshooting

### Symptom: Compile error "Type mismatch: inferred type is String but Boolean was expected"

**Causes**:
- Using wrong feature name or type

**Fix**:
```kotlin
// Correct: type flows from feature definition
val enabled: Boolean = AppFeatures.darkMode.evaluate(ctx)

// Wrong: cannot assign Boolean to String
val enabled: String = AppFeatures.darkMode.evaluate(ctx) // Compile error
```

**Verification**: Code compiles without type errors.

**Related**: [Learn: Type Safety](/learn/type-safety)

### Symptom: All evaluations return default value

**Causes**:
- No rules match the context
- Rules have unreachable criteria
- Registry kill-switch enabled

**Fix**:
- Check context values match rule criteria
- Verify rule specificity and order
- Confirm `AppFeatures.isDisabled == false`

**Verification**: Call `AppFeatures.darkMode.evaluate(ctx)` to see why default was returned.

**Related**: [Reference: Feature Evaluation](/reference/api/feature-evaluation#explain)

## Next Steps

- [Learn: Core Primitives](/learn/core-primitives) — Understand Namespace, Feature, Context, Rule
- [Learn: Evaluation Model](/learn/evaluation-model) — How rules are ordered and evaluated
- [Guide: Roll Out Gradually](/guides/roll-out-gradually) — Add ramp-ups and percentages
- [Guide: Test Features](/guides/test-features) — Write tests for features
- [Examples: Golden Path](/examples/golden-path) — Complete annotated example
