# Namespace Operations

API reference for managing namespace configuration lifecycle: loading, rollback, and kill-switch operations.

---

## `Namespace.load(configuration)`

Atomically replace the active configuration snapshot.

```kotlin
fun Namespace.load(configuration: Configuration)
```

### Parameters

- `configuration` — New configuration snapshot (typically from `SnapshotSerializer.fromJson(...)`)

### Behavior

- Performs atomic swap via `AtomicReference.set(...)`
- Readers see either old OR new snapshot (never mixed)
- Adds current config to rollback history (bounded)

### Example

```kotlin
when (val result = SnapshotSerializer.fromJson(json)) {
    is ParseResult.Success -> AppFeatures.load(result.value)
    is ParseResult.Failure -> logError(result.error.message)
}
```

### Thread Safety

- **Lock-free reads** — Concurrent evaluations don't block
- **Atomic writes** — Last write wins (linearizable)

See [Fundamentals: Refresh Safety](/fundamentals/refresh-safety) for details.

---

## `Namespace.fromJson(json): ParseResult<Unit>`

Parse and load JSON configuration in one step (convenience wrapper).

```kotlin
fun Namespace.fromJson(json: String): ParseResult<Unit>
```

### Parameters

- `json` — JSON snapshot payload

### Returns

- `ParseResult.Success` if JSON is valid and loaded
- `ParseResult.Failure` if parse fails (last-known-good remains active)

### Example

```kotlin
val json = File("flags.json").readText()

when (val result = AppFeatures.fromJson(json)) {
    is ParseResult.Success -> logger.info("Config loaded")
    is ParseResult.Failure -> logger.error("Parse failed: ${result.error}")
}
```

### Precondition

Features must be registered before calling `fromJson(...)`. See [Fundamentals: Definition vs Initialization](/fundamentals/definition-vs-initialization).

---

## `Namespace.toJson(): String`

Export the current configuration snapshot to JSON.

```kotlin
fun Namespace.toJson(): String
```

### Returns

JSON string representing the current namespace configuration.

### Example

```kotlin
val json = AppFeatures.toJson()
File("flags.json").writeText(json)
```

### Use Cases

- Externalize current config for storage
- Generate baseline config for remote distribution
- Audit current state

---

## `Namespace.configuration: Configuration`

Get the current active configuration snapshot.

```kotlin
val Namespace.configuration: Configuration
```

### Returns

The active `Configuration` snapshot.

### Example

```kotlin
val currentConfig = AppFeatures.configuration
val metadata = currentConfig.metadata
println("Version: ${metadata?.version}")
```

---

## `Namespace.rollback(steps): Boolean`

Revert to a prior configuration from rollback history.

```kotlin
fun Namespace.rollback(steps: Int = 1): Boolean
```

### Parameters

- `steps` — Number of configurations to roll back (default: 1)

### Returns

- `true` if rollback succeeded
- `false` if not enough history available

### Example

```kotlin
val success = AppFeatures.rollback(steps = 1)
if (success) {
    logger.info("Rolled back to previous config")
} else {
    logger.warn("Rollback failed: insufficient history")
}
```

### Behavior

- Registry maintains a bounded history (configurable, default: 5)
- Rollback atomically swaps to the prior snapshot
- Does not affect feature definitions in code

---

## `Namespace.historyMetadata: List<ConfigurationMetadata>`

Get metadata for all configurations in rollback history.

```kotlin
val Namespace.historyMetadata: List<ConfigurationMetadata>
```

### Returns

List of `ConfigurationMetadata` (version, timestamp, source) for each snapshot in history.

### Example

```kotlin
val history = AppFeatures.historyMetadata
history.forEach { meta ->
    println("Version: ${meta.version}, Loaded at: ${meta.generatedAtEpochMillis}")
}
```

---

## `Namespace.disableAll()`

Emergency kill-switch: disable all evaluations in this namespace (return defaults).

```kotlin
fun Namespace.disableAll()
```

### Behavior

- All evaluations return declared defaults
- Does not change feature definitions
- Scoped to this namespace only (other namespaces unaffected)

### Example

```kotlin
AppFeatures.disableAll()

val enabled = AppFeatures.darkMode.evaluate(context)  // Returns default (false)
```

### Use Cases

- Emergency rollback when config is suspect
- Incident response (kill all experiments)
- Testing default-only behavior

---

## `Namespace.enableAll()`

Re-enable all evaluations after `disableAll()`.

```kotlin
fun Namespace.enableAll()
```

### Example

```kotlin
AppFeatures.enableAll()

val enabled = AppFeatures.darkMode.evaluate(context)  // Normal evaluation resumes
```

---

## `Namespace.registry: NamespaceRegistry`

Access the underlying registry (advanced use only).

```kotlin
val Namespace.registry: NamespaceRegistry
```

### Returns

The `NamespaceRegistry` instance for this namespace.

### Warning

Direct registry manipulation bypasses safety guarantees. Prefer public API methods (`load`, `rollback`, `disableAll`).

---

## Next Steps

- [Feature Operations](/api-reference/feature-operations) — Evaluation API
- [Serialization](/api-reference/serialization) — JSON snapshot/patch operations
- [Fundamentals: Configuration Lifecycle](/fundamentals/configuration-lifecycle) — Lifecycle details
- [Fundamentals: Refresh Safety](/fundamentals/refresh-safety) — Atomic update guarantees
