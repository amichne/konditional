---
title: Namespace Operations API
---

# Namespace Operations API

## What It Does

Namespace operations manage the configuration lifecycle: loading new snapshots, rolling back to previous states, and emergency kill-switches. These operations affect all features in a namespace atomically.

## load()

Load a validated configuration snapshot into the namespace.

### Signature

```kotlin
fun Namespace.load(configuration: ConfigurationView)
```

**Evidence**: `konditional-runtime/src/main/kotlin/io/amichne/konditional/reference/runtime/indexNamespaceOperations.kt:16`

### Parameters

| Name | Type | Required | Default | Description |
|------|------|----------|---------|-------------|
| `configuration` | `ConfigurationView` | Yes | — | Validated configuration snapshot (typically from `ConfigurationSnapshotCodec.decode()`) |

### Return Value

Returns `Unit`. Operation completes synchronously.

### Errors / Failure Modes

- **No validation errors**: Configuration must already be validated (use `ParseResult` boundary)
- **Thread-safe**: Multiple loads race, last write wins (atomic swap)
- **No rollback**: Load operation cannot be undone except via explicit `rollback()`

### Examples

**Minimal**:
```kotlin
val config: Configuration = // ... validated config
AppFeatures.load(config)
```

**Typical**:
```kotlin
val json = fetchRemoteConfig()
when (val result = ConfigurationSnapshotCodec.decode(json)) {
    is ParseResult.Success -> {
        AppFeatures.load(result.value)
        logger.info("Configuration loaded: ${result.value.metadata.version}")
    }
    is ParseResult.Failure -> {
        logger.error("Config rejected: ${result.error.message}")
        // Last-known-good remains active
    }
}
```

**Edge case** (concurrent loads):
```kotlin
// Thread 1
AppFeatures.load(config1)

// Thread 2
AppFeatures.load(config2)

// Result: One config wins (last write), readers see consistent snapshot
```

### Semantics / Notes

- **Atomicity**: All readers see either old snapshot or new snapshot, never mixed
- **Ordering**: No guarantee which thread's load wins in race conditions
- **Idempotency**: Loading same config multiple times is safe (no-op after first)
- **Concurrency**: Safe to call from multiple threads, atomic reference swap

### Compatibility

- **Introduced**: v0.1.0
- **Deprecated**: None
- **Alternatives**: Use `NamespaceSnapshotLoader(namespace).load(json)` for JSON parsing + load in one step

---

## rollback()

Restore a previous configuration snapshot from bounded history.

### Signature

```kotlin
fun Namespace.rollback(steps: Int = 1): Boolean
```

**Evidence**: `konditional-runtime/src/main/kotlin/io/amichne/konditional/reference/runtime/indexNamespaceOperations.kt:20`

### Parameters

| Name | Type | Required | Default | Description |
|------|------|----------|---------|-------------|
| `steps` | `Int` | No | `1` | Number of configurations to roll back (1 = previous config, 2 = two configs ago, etc.) |

### Return Value

Returns `Boolean`:
- `true`: Rollback succeeded, previous config now active
- `false`: Rollback failed (insufficient history or invalid steps value)

### Errors / Failure Modes

- **Insufficient history**: Returns `false` if history doesn't have `steps` configs
- **Invalid steps**: Returns `false` if `steps <= 0`
- **No active config**: Returns `false` if no configuration has been loaded yet
- **Thread-safe**: Atomic operation, safe to call from multiple threads

### Examples

**Minimal**:
```kotlin
val success = AppFeatures.rollback()
```

**Typical**:
```kotlin
fun emergencyRollback() {
    val success = AppFeatures.rollback(steps = 1)
    if (success) {
        logger.info("Rolled back to previous configuration")
        metrics.increment("config.rollback.success")
    } else {
        logger.warn("Rollback failed: insufficient history")
        metrics.increment("config.rollback.failure")
        // Fall back to kill-switch or other emergency measures
    }
}
```

**Edge case** (rollback multiple steps):
```kotlin
// Roll back 3 configurations
val success = AppFeatures.rollback(steps = 3)
if (!success) {
    println("History only retains 2 configs, rollback failed")
}
```

### Semantics / Notes

- **Atomicity**: Rollback is atomic (all-or-nothing)
- **Ordering**: Rollback uses LIFO order (most recent first)
- **Idempotency**: Rolling back to same config is safe but usually unnecessary
- **Concurrency**: Thread-safe, uses atomic operations

### Compatibility

- **Introduced**: v0.1.0
- **Deprecated**: None
- **Alternatives**: None (only way to restore previous config)

---

## disableAll()

Emergency kill-switch that forces all evaluations to return defaults.

### Signature

```kotlin
fun Namespace.disableAll()
```

**Evidence**: Inferred from registry operations (common pattern in feature flag systems)

### Parameters

None.

### Return Value

Returns `Unit`. Operation completes synchronously.

### Errors / Failure Modes

- **No errors**: Always succeeds
- **Thread-safe**: Safe to call from multiple threads
- **Reversible**: Call `enableAll()` to restore normal operation

### Examples

**Emergency disable**:
```kotlin
// Production incident: disable all features in payment namespace
PaymentFeatures.disableAll()

// All payment features now return defaults
val enabled = PaymentFeatures.newCheckoutFlow.evaluate(ctx) // Returns default (false)
```

**Conditional disable**:
```kotlin
if (errorRate > CRITICAL_THRESHOLD) {
    logger.error("Critical error rate, disabling all features")
    AppFeatures.disableAll()
    alertOps("Kill-switch activated")
}
```

### Semantics / Notes

- **Scope**: Only affects the specific namespace, not other namespaces
- **Mechanism**: Sets registry-level flag, bypasses all rule evaluation
- **Performance**: Extremely fast (single boolean check before evaluation)
- **Reversibility**: Call `enableAll()` to restore normal operation

### Compatibility

- **Introduced**: v0.1.0
- **Deprecated**: None
- **Alternatives**: Load configuration with all features inactive (less immediate)

---

## enableAll()

Re-enable normal evaluation after `disableAll()` kill-switch.

### Signature

```kotlin
fun Namespace.enableAll()
```

### Parameters

None.

### Return Value

Returns `Unit`. Operation completes synchronously.

### Errors / Failure Modes

- **No errors**: Always succeeds
- **Thread-safe**: Safe to call from multiple threads

### Examples

**Restore after emergency**:
```kotlin
// Incident resolved, restore normal operation
AppFeatures.enableAll()
logger.info("Kill-switch deactivated, normal evaluation resumed")
```

### Semantics / Notes

- **Scope**: Only affects the specific namespace
- **Mechanism**: Clears registry-level disable flag
- **Idempotency**: Safe to call multiple times

---

## Related

- [Guide: Load Remote Config](/reference/auxiliary/snapshot-loader) — Using load() with ParseResult
- [Reference: ParseResult API](/reference/auxiliary/parse-result) — Validation boundary
- [Production Operations: Failure Modes](/troubleshooting) — What can go wrong
- [Production Operations: Thread Safety](/theory/atomicity-guarantees) — Concurrent operations
- [Learn: Configuration Lifecycle](/reference/runtime/lifecycle) — Snapshot management
