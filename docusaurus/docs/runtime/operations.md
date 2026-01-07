# Runtime Operations

API reference for managing namespace configuration lifecycle: loading, rollback, and kill-switch operations.

`load` / `rollback` / `historyMetadata` are runtime-only extensions from `io.amichne.konditional.runtime`.

---

## `Namespace.load(configuration)`

Atomically replace the active configuration snapshot.

```kotlin
fun Namespace.load(configuration: ConfigurationView)
```

### Parameters

- `configuration` - new configuration snapshot (typically from `ConfigurationSnapshotCodec.decode(...)`)

### Behavior

- Performs atomic swap via `AtomicReference.set(...)`
- Readers see either old or new snapshot (never mixed)
- Adds current config to rollback history (bounded)

### Example

```kotlin
when (val result = ConfigurationSnapshotCodec.decode(json)) {
    is ParseResult.Success -> AppFeatures.load(result.value)
    is ParseResult.Failure -> logError(result.error.message)
}
```

---

## `Namespace.configuration: ConfigurationView`

Get the current active configuration snapshot.

```kotlin
val Namespace.configuration: ConfigurationView
```

### Example

```kotlin
val current = AppFeatures.configuration
println("Version: ${current.metadata.version}")
```

---

<details>
<summary>Advanced Options</summary>

## `Namespace.rollback(steps): Boolean`

Revert to a prior configuration from rollback history.

```kotlin
fun Namespace.rollback(steps: Int = 1): Boolean
```

### Returns

- `true` if rollback succeeded
- `false` if not enough history is available

---

## `Namespace.historyMetadata: List<ConfigurationMetadataView>`

Read metadata for the rollback history.

```kotlin
val Namespace.historyMetadata: List<ConfigurationMetadataView>
```

---

## `Namespace.disableAll()` / `Namespace.enableAll()`

Emergency kill-switch to return defaults for all features.

```kotlin
fun Namespace.disableAll()
fun Namespace.enableAll()
```

**Guarantee**: When disabled, evaluations return declared defaults.

**Mechanism**: Registry-level boolean kill-switch.

**Boundary**: This does not change feature definitions or loaded configuration.

---

## `Namespace.setHooks(hooks)`

Attach dependency-free logging/metrics hooks to a namespace registry.

```kotlin
fun Namespace.setHooks(hooks: RegistryHooks)
```

See [Observability](/observability/) for `RegistryHooks` and related interfaces.

</details>

---

## Next steps

- [Configuration lifecycle](/runtime/lifecycle)
- [Serialization module](/serialization/)
- [Observability](/observability/)
