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
    fun decode(
        json: String,
        featuresById: Map<FeatureId, Feature<*, *, *>>,
        options: SnapshotLoadOptions = SnapshotLoadOptions.strict(),
    ): ParseResult<Configuration>
}
```

### Precondition

For snapshots containing flags, provide an explicit trusted feature scope via `featuresById`.
Direct decode without `featuresById` fails fast for non-empty snapshots.

### Example

```kotlin
val featuresById = AppFeatures.allFeatures().associateBy { it.id }
when (val result = ConfigurationSnapshotCodec.decode(json, featuresById = featuresById)) {
    is ParseResult.Success -> AppFeatures.load(result.value)
    is ParseResult.Failure -> logger.error { "Parse failed: ${result.error.message}" }
}
```

### Trusted Type Resolution

Enum and data-class decoding use trusted feature metadata from `featuresById`.
Class-name hints embedded in payload values are treated as opaque metadata and are not used for reflective class loading.

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
