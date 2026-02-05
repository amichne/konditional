# Serialization API Reference

API reference for JSON snapshot/patch operations: parsing, serialization, and incremental updates.

---

## `ConfigurationSnapshotCodec.encode(configuration): String`

Serialize a configuration snapshot to JSON.

```kotlin
object ConfigurationSnapshotCodec {
    fun encode(configuration: Configuration): String
    fun encode(configuration: ConfigurationView): String
}
```

### Example

```kotlin
val snapshotJson = ConfigurationSnapshotCodec.encode(AppFeatures.configuration)
persistToStorage(snapshotJson)
```

---

## `ConfigurationSnapshotCodec.decode(json, options): ParseResult<Configuration>`

Parse a snapshot JSON payload into a validated `Configuration`.

```kotlin
object ConfigurationSnapshotCodec {
    fun decode(json: String): ParseResult<Configuration>
    fun decode(
        json: String,
        options: SnapshotLoadOptions
    ): ParseResult<Configuration>
}
```

### Precondition

Features must be registered before parsing. Ensure your `Namespace` objects are initialized before calling`decode(...)`.

### Example

```kotlin
when (val result = ConfigurationSnapshotCodec.decode(json)) {
    is ParseResult.Success -> AppFeatures.load(result.value)
    is ParseResult.Failure -> logger.error { "Parse failed: ${result.error.message}" }
}
```

---

<details>
<summary>Advanced Options</summary>

## `ConfigurationSnapshotCodec.applyPatchJson(configuration, patchJson, options): ParseResult<Configuration>`

Apply an incremental patch to an existing configuration snapshot.

```kotlin
object ConfigurationSnapshotCodec {
    fun applyPatchJson(
        currentConfiguration: ConfigurationView,
        patchJson: String,
        options: SnapshotLoadOptions = SnapshotLoadOptions.strict(),
    ): ParseResult<Configuration>
}
```

---

## `NamespaceSnapshotLoader<M>`

Namespace-scoped JSON loader. On success, `load(...)` parses and loads the new configuration into the namespace.

```kotlin
class NamespaceSnapshotLoader<M : Namespace>(namespace: M) {
    fun load(json: String): ParseResult<Configuration>
    fun load(json: String, options: SnapshotLoadOptions): ParseResult<Configuration>
}
```

---

## `SnapshotLoadOptions`

Controls how unknown feature keys are handled during snapshot/patch loads.

```kotlin
data class SnapshotLoadOptions(
    val unknownFeatureKeyStrategy: UnknownFeatureKeyStrategy = UnknownFeatureKeyStrategy.Fail,
    val onWarning: (SnapshotWarning) -> Unit = {},
)
```

### `UnknownFeatureKeyStrategy`

```kotlin
sealed interface UnknownFeatureKeyStrategy {
    data object Fail : UnknownFeatureKeyStrategy
    data object Skip : UnknownFeatureKeyStrategy
}
```

---

## `ParseError`

Error types returned via `ParseResult.Failure`.

```kotlin
sealed interface ParseError {
    val message: String

    data class InvalidJson(val reason: String) : ParseError
    data class InvalidSnapshot(val reason: String) : ParseError

    data class FeatureNotFound(val key: FeatureId) : ParseError
    data class FlagNotFound(val key: FeatureId) : ParseError

    data class InvalidHexId(val input: String, val message: String) : ParseError
    data class InvalidRollout(val value: Double, val message: String) : ParseError
    data class InvalidVersion(val input: String, val message: String) : ParseError
}
```

</details>

---

## Next steps

- [Persistence format](/serialization/persistence-format)
- [Runtime operations](/runtime/operations)
