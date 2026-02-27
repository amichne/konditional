---
title: Namespace Operations API
---

# Namespace Operations API

Namespace runtime operations from `io.amichne.konditional.runtime`.

## Runtime Extensions

```kotlin
fun Namespace.update(configuration: Configuration)
fun Namespace.dump(): String
val Namespace.json: String
fun Namespace.rollback(steps: Int = 1): Boolean
val Namespace.history: List<ConfigurationView>
val Namespace.historyMetadata: List<ConfigurationMetadataView>
```

## Load Flow

`Namespace.update(...)` accepts a decoded `Configuration`.

```kotlin
val result = NamespaceSnapshotLoader.forNamespace(AppFeatures).load(json)

result
  .onSuccess { configuration -> AppFeatures.update(configuration) }
  .onFailure { failure ->
    val parseError = failure.parseErrorOrNull()
    logger.error { parseError?.message ?: failure.message.orEmpty() }
  }
```

## Snapshot Export

`Namespace.dump()` serializes the namespace's current immutable snapshot into canonical JSON.
`Namespace.json` is a convenience accessor equivalent to `dump()`.

```kotlin
val snapshotJson = AppFeatures.dump()
val snapshotJsonViaProperty = AppFeatures.json
```

## Kill Switch

```kotlin
fun disableAll()
fun enableAll()
val isAllDisabled: Boolean
```

- `disableAll()` forces declared defaults.
- Scope is namespace-local.

## Atomicity

- Loads and rollbacks are linearizable in the default runtime registry.
- Readers observe whole snapshots only.

## Related

- [Snapshot Loader API](/reference/api/snapshot-loader)
- [Serialization API Reference](/serialization/reference)
