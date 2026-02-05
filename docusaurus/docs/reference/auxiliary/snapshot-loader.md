---
title: NamespaceSnapshotLoader API
---

# NamespaceSnapshotLoader API

## What It Does

`NamespaceSnapshotLoader` combines JSON parsing and namespace loading into a single operation, providing a convenient API for loading remote configuration.

## NamespaceSnapshotLoader

### Signature

```kotlin
class NamespaceSnapshotLoader<M : Namespace>(
    private val namespace: M
)
```

**Evidence**: `konditional-runtime/src/main/kotlin/io/amichne/konditional/reference/serialization/indexsnapshot/NamespaceSnapshotLoader.kt:18`

### Parameters

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `namespace` | `M extends Namespace` | Yes | The namespace to load configuration into |

### Examples

**Create loader**:
```kotlin
val loader = NamespaceSnapshotLoader(AppFeatures)
```

---

## load()

Parse JSON and load into namespace in one operation.

### Signature

```kotlin
fun load(json: String): ParseResult<Unit>
```

### Parameters

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `json` | `String` | Yes | JSON configuration snapshot |

### Return Value

Returns `ParseResult<Unit>`:
- `Success(Unit)`: JSON parsed and loaded successfully
- `Failure(error)`: Parse failed, last-known-good remains active

### Errors / Failure Modes

- **Parse errors**: Invalid JSON, unknown features, type mismatches → `ParseResult.Failure`
- **No exceptions**: Always returns `ParseResult`, never throws for validation errors
- **Thread-safe**: Safe to call from multiple threads (last write wins)

### Examples

**Minimal**:
```kotlin
val loader = NamespaceSnapshotLoader(AppFeatures)
val result = loader.load(json)
```

**Typical**:
```kotlin
val loader = NamespaceSnapshotLoader(AppFeatures)

when (val result = loader.load(fetchRemoteConfig())) {
    is ParseResult.Success -> {
        logger.info("Configuration loaded successfully")
        metrics.increment("config.load.success")
    }
    is ParseResult.Failure -> {
        logger.error("Config parse failed: ${result.error.message}")
        metrics.increment("config.load.failure")
        alertOps("Configuration rejected", result.error)
    }
}
```

**Edge case** (unknown keys):
```kotlin
val json = """{ "unknownFeature": { "rules": [] } }"""
val result = loader.load(json)
// Returns: Failure(FeatureNotFound)
```

### Semantics / Notes

- **Convenience wrapper**: Combines `ConfigurationSnapshotCodec.decode()` + `namespace.load()`
- **Atomic**: Parse and load are atomic (both succeed or both fail)
- **Idempotency**: Safe to call multiple times with same JSON
- **Concurrency**: Thread-safe via atomic namespace operations

### Compatibility

- **Introduced**: v0.1.0
- **Deprecated**: None
- **Alternatives**: Use `ConfigurationSnapshotCodec.decode()` + `namespace.load()` separately for more control

---

## Usage Patterns

### Pattern: Periodic Refresh

```kotlin
class ConfigRefreshService {
    private val loader = NamespaceSnapshotLoader(AppFeatures)

    fun refreshConfig() {
        val json = try {
            httpClient.get("https://config.example.com/features.json")
        } catch (e: Exception) {
            logger.warn("Config fetch failed", e)
            return
        }

        when (val result = loader.load(json)) {
            is ParseResult.Success -> logger.info("Config refreshed")
            is ParseResult.Failure -> logger.error("Config rejected: ${result.error}")
        }
    }

    fun startPeriodicRefresh(intervalMs: Long) {
        scheduler.scheduleAtFixedRate(
            ::refreshConfig,
            intervalMs,
            intervalMs,
            TimeUnit.MILLISECONDS
        )
    }
}
```

### Pattern: Load with Fallback

```kotlin
fun loadConfigWithFallback(primaryUrl: String, fallbackUrl: String) {
    val loader = NamespaceSnapshotLoader(AppFeatures)

    val primaryResult = loader.load(fetchConfig(primaryUrl))
    if (primaryResult is ParseResult.Success) {
        logger.info("Loaded from primary source")
        return
    }

    logger.warn("Primary failed, trying fallback")
    val fallbackResult = loader.load(fetchConfig(fallbackUrl))
    if (fallbackResult is ParseResult.Success) {
        logger.info("Loaded from fallback source")
    } else {
        logger.error("Both sources failed")
    }
}
```

### Pattern: Load with Validation

```kotlin
fun loadConfigWithValidation(json: String) {
    val loader = NamespaceSnapshotLoader(AppFeatures)

    // Custom pre-validation
    if (!json.contains("\"metadata\"")) {
        logger.error("Config missing metadata field")
        return
    }

    when (val result = loader.load(json)) {
        is ParseResult.Success -> {
            // Post-validation: verify config makes sense
            val ctx = Context(stableId = StableId.of("validation-user"))
            val value = AppFeatures.darkMode.evaluate(ctx)
            logger.info("Config loaded, validation feature = $value")
        }
        is ParseResult.Failure -> logger.error("Parse failed: ${result.error}")
    }
}
```

---

## Comparison to Manual Approach

| Approach | Code | Error Handling | Use Case |
|----------|------|----------------|----------|
| **NamespaceSnapshotLoader** | `loader.load(json)` | Single `ParseResult` | Simple config loading, most use cases |
| **Manual** | `decode(json).fold { load(it) }` | Separate decode + load | Need intermediate access to `Configuration` |

**Recommendation**: Use `NamespaceSnapshotLoader` unless you need to inspect or transform the `Configuration` object before loading.

---

## Related

- [Guide: Load Remote Config](/reference/auxiliary/snapshot-loader) — Using this API in practice
- [Reference: Namespace Operations](/reference/auxiliary/namespace-operations) — Manual load() operation
- [Reference: ParseResult](/reference/auxiliary/parse-result) — Result type utilities
- [Production Operations: Failure Modes](/troubleshooting) — What can go wrong
- [Serialization: Persistence Format](/reference/serialization/persistence-format) — JSON schema
