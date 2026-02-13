---
title: Troubleshooting
---

# Troubleshooting

Symptom-first index for diagnosing and fixing Konditional issues.

---

## Start Here

Use the fastest path based on what broke first:

- Config payload rejected -> go to [Parsing Issues](#parsing-issues)
- Same user flips between rollout states -> go to [Bucketing Issues](#bucketing-issues)
- Feature value differs from expectation -> go to [Evaluation Issues](#evaluation-issues)
- Runtime behavior is stale after load -> go to [Integration Issues](#integration-issues)

## Quick Diagnosis

| Symptom | Likely Cause | Section |
|---------|--------------|---------|
| Feature returns unexpected value | Rule mismatch or precedence | [Evaluation Issues](#evaluation-issues) |
| Same user gets different values | Non-deterministic stableId | [Bucketing Issues](#bucketing-issues) |
| JSON fails to load | Parse error or schema mismatch | [Parsing Issues](#parsing-issues) |
| Slow evaluation | Too many rules or hooks | [Performance Issues](#performance-issues) |
| Feature not found error | Namespace not initialized | [Integration Issues](#integration-issues) |

---

## Evaluation Issues

### Wrong value returned

**Symptom**: Feature evaluates to unexpected value.

**Likely causes**:
1. Context doesn't match rule criteria
2. Rule precedence (specificity) incorrect
3. Registry kill-switch active
4. Feature inactive in configuration

**Fix**:
```kotlin
val result = AppFeatures.darkMode.explain(ctx)
println(result.decision) // Shows why value was returned
```

**Verification**: `explain()` output shows expected decision path.

**Related**: [Learn: Evaluation Model](/learn/evaluation-model), [Reference: Feature Evaluation](/reference/api/feature-evaluation)

---

### All features return defaults

**Symptom**: Every evaluation returns default value, no rules match.

**Likely causes**:
1. Kill-switch enabled: `namespace.disableAll()` called
2. All features inactive in configuration
3. Context values don't match any rule criteria

**Fix**:
```kotlin
// Check kill-switch
if (AppFeatures.isDisabled) {
    AppFeatures.enableAll()
}

// Check rule matching
val result = AppFeatures.darkMode.explain(ctx)
println(result.decision) // DISABLED, INACTIVE, or DEFAULT?
```

**Verification**: `explain()` shows `RegistryDisabled` or specific reason.

**Related**: [Reference: Namespace Operations](/reference/api/namespace-operations)

---

## Bucketing Issues

### Non-deterministic ramp-ups

**Symptom**: Same user gets different ramp-up results on each evaluation.

**Likely causes**:
1. StableId not persistent (session ID, random UUID)
2. Salt changing between evaluations
3. Context stableId field changes

**Fix**:
```kotlin
// Wrong: random ID
val ctx = Context(stableId = StableId.of(UUID.randomUUID().toString()))

// Correct: persistent ID
val ctx = Context(stableId = StableId.of(userId)) // Database ID
```

**Verification**:
```kotlin
val results = (1..100).map { AppFeatures.feature.evaluate(ctx) }
assertTrue(results.all { it == results.first() }) // All same
```

**Related**: [Guide: Roll Out Gradually](/guides/roll-out-gradually), [Design Theory: Determinism Proofs](/design-theory/determinism-proofs)

---

### Wrong percentage distribution

**Symptom**: 10% ramp-up results in 50% of users in treatment.

**Likely causes**:
1. Rule specificity causing wrong rule to match first
2. Multiple ramp-up rules combining unexpectedly
3. Context filtering before evaluation (sampling bias)

**Fix**:
```kotlin
// Test distribution with large sample
val sampleSize = 10_000
val inTreatment = (0 until sampleSize).count { i ->
    val ctx = Context(stableId = StableId.of("user-$i"))
    AppFeatures.feature.evaluate(ctx)
}
val actualPct = (inTreatment.toDouble() / sampleSize) * 100
println("Actual: $actualPct%, Expected: 10%")
```

**Verification**: Actual percentage within ±1% of expected.

**Related**: [Guide: Test Features](/guides/test-features), [Reference: RampUp Bucketing](/reference/api/ramp-up-bucketing)

---

## Parsing Issues

### JSON fails to load

**Symptom**: `ParseResult.Failure` returned from `decode()`.

**Likely causes**:
1. Malformed JSON syntax
2. Unknown feature key
3. Type mismatch (wrong value type)
4. Schema validation failure (custom data classes)

**Fix**:
```kotlin
when (val result = ConfigurationSnapshotCodec.decode(json)) {
    is ParseResult.Failure -> {
        println("Error: ${result.error.message}")
        when (result.error) {
            is ParseError.InvalidJson -> println("JSON syntax error")
            is ParseError.FeatureNotFound -> println("Unknown feature key")
            is ParseError.TypeMismatch -> println("Value type mismatch")
            // ... handle specific errors
        }
    }
}
```

**Verification**: Parse succeeds with corrected JSON.

**Related**: [Guide: Load Remote Config](/guides/load-remote-config), [Production Operations: Failure Modes](/production-operations/failure-modes)

---

### Type mismatch error

**Symptom**: `ParseError.TypeMismatch` when loading JSON.

**Likely causes**:
1. JSON value type doesn't match feature type (string for boolean, etc.)
2. Enum constant not found
3. Schema violation in custom data class

**Fix**:
```kotlin
// Feature defined as boolean
val darkMode by boolean<Context>(default = false)

// JSON must specify BOOLEAN type
{
  "key": "feature::app::darkMode",
  "defaultValue": { "type": "BOOLEAN", "value": false } // Not STRING
}
```

**Verification**: JSON schema validated, parse succeeds.

**Related**: [Serialization: Persistence Format](/serialization/persistence-format)

---

## Performance Issues

### Slow evaluation

**Symptom**: `evaluate()` takes > 1ms consistently.

**Likely causes**:
1. Too many rules (> 50 per feature)
2. Expensive extension predicates
3. Observability hooks blocking
4. Registry contention (many threads loading config)

**Fix**:
```kotlin
// Profile evaluation
val start = System.nanoTime()
val value = AppFeatures.feature.evaluate(ctx)
val durationUs = (System.nanoTime() - start) / 1000
println("Evaluation: ${durationUs}μs")

// Simplify rules or use specificity to short-circuit early
```

**Verification**: Evaluation < 100μs for typical workloads.

**Related**: [Production Operations: Thread Safety](/production-operations/thread-safety)

---

## Integration Issues

### Configuration not loading

**Symptom**: `load()` succeeds but evaluations still use old values.

**Likely causes**:
1. Wrong namespace loaded
2. Load called on different registry instance
3. Configuration not actually loaded (silent failure)

**Fix**:
```kotlin
// Verify load on correct namespace
when (val result = ConfigurationSnapshotCodec.decode(json)) {
    is ParseResult.Success -> {
        AppFeatures.load(result.value) // Same namespace as evaluation

        // Immediate verification
        val ctx = Context(...)
        val value = AppFeatures.darkMode.explain(ctx)
        println("Loaded config: ${value.decision}")
    }
    is ParseResult.Failure -> error("Config didn't load: ${result.error}")
}
```

**Verification**: `explain()` shows new rule matched.

**Related**: [Reference: Namespace Operations](/reference/api/namespace-operations)

### Feature not found

**Symptom**: Evaluation or loading reports unknown or missing feature key.

**Likely causes**:
1. Typo in feature key from remote payload
2. Feature exists in a different namespace
3. Code and remote configuration are out of sync

**Fix**: Confirm key spelling, namespace target, and deployed artifact version before retrying load.

---

## Next steps

- Prevent config regressions: [How-To: Load Configuration Safely from Remote](/how-to-guides/safe-remote-config)
- Validate rollout determinism: [How-To: Roll Out a Feature Gradually](/how-to-guides/rolling-out-gradually)
- Expand tests around failures: [How-To: Test Your Feature Flags](/how-to-guides/testing-features)
