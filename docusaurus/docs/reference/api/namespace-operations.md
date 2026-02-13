---
title: Namespace Operations API
---

# Namespace Operations API

Namespace operations control the active configuration snapshot and emergency behavior for one namespace at a time.

In this page you will find:

- Runtime lifecycle operations (`load`, `rollback`, `history`)
- Namespace kill-switch operations (`disableAll`, `enableAll`)
- Exact failure semantics from the default in-memory registry

---

## Runtime Extensions (`konditional-runtime`)

These are extension APIs from `io.amichne.konditional.runtime`:

```kotlin
fun Namespace.load(configuration: ConfigurationView)
fun Namespace.rollback(steps: Int = 1): Boolean
val Namespace.history: List<ConfigurationView>
val Namespace.historyMetadata: List<ConfigurationMetadataView>
```

Evidence:

- `konditional-runtime/src/main/kotlin/io/amichne/konditional/runtime/NamespaceOperations.kt`
- `konditional-runtime/src/main/kotlin/io/amichne/konditional/core/registry/InMemoryNamespaceRegistry.kt`

### `load(configuration)`

- Replaces the current snapshot and appends the previous snapshot to bounded history.
- The default in-memory registry serializes write operations with a lock and stores snapshots in atomic references.
- Concurrent loads are safe; effective result is last write wins.

```kotlin
when (val result = ConfigurationSnapshotCodec.decode(json)) {
    is ParseResult.Success -> AppFeatures.load(result.value)
    is ParseResult.Failure -> logger.error { result.error.message }
}
```

### `rollback(steps)`

- Returns `true` when rollback succeeds.
- Returns `false` when history does not contain enough snapshots.
- Throws `IllegalArgumentException` when `steps < 1` (via `require(steps >= 1)`).

```kotlin
val restored = AppFeatures.rollback(steps = 1)
```

---

## Kill-Switch Operations (`NamespaceRegistry`)

`Namespace` delegates `NamespaceRegistry`, so these operations are available directly:

```kotlin
fun disableAll()
fun enableAll()
val isAllDisabled: Boolean
```

Evidence:

- `konditional-core/src/main/kotlin/io/amichne/konditional/core/registry/NamespaceRegistry.kt`

Behavior:

- `disableAll()` forces evaluations to return each flag's default.
- `enableAll()` restores normal rule evaluation.
- Scope is namespace-local; other namespaces are unaffected.

---

## Concurrency and Atomicity Notes

- Reads (`namespace.configuration`) are atomic snapshot reads.
- Loads/rollbacks are linearized by registry synchronization in the default runtime implementation.
- Readers observe either the old or the new snapshot, never a partial merge.

For deeper guarantees, see [Atomicity Guarantees](/theory/atomicity-guarantees).

---

## Practical Pattern

```kotlin
val loader = NamespaceSnapshotLoader(AppFeatures)

when (loader.load(fetchRemoteConfig())) {
    is ParseResult.Success -> Unit
    is ParseResult.Failure -> {
        val rolledBack = AppFeatures.rollback(1)
        if (!rolledBack) AppFeatures.disableAll()
    }
}
```

---

## Related

- [NamespaceSnapshotLoader API](/reference/api/snapshot-loader)
- [ParseResult API](/reference/api/parse-result)
- [Runtime Operations](/runtime/operations)
- [Thread Safety](/production-operations/thread-safety)
