---
title: Namespace Operations API
---

# Namespace Operations API

Namespace runtime operations from `io.amichne.konditional.runtime`.

## Runtime Extensions

```kotlin
fun Namespace.load(configuration: MaterializedConfiguration)
fun Namespace.rollback(steps: Int = 1): Boolean
val Namespace.history: List<ConfigurationView>
val Namespace.historyMetadata: List<ConfigurationMetadataView>
```

## Load Flow

`Namespace.load(...)` accepts only trusted `MaterializedConfiguration`.

```kotlin
val result = NamespaceSnapshotLoader(AppFeatures).load(json)

result
  .onSuccess { materialized -> AppFeatures.load(materialized) }
  .onFailure { failure ->
    val parseError = failure.parseErrorOrNull()
    logger.error { parseError?.message ?: failure.message.orEmpty() }
  }
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
