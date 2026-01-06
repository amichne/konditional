# Serialization

API reference for JSON snapshot/patch operations: parsing, serialization, and incremental updates.

`Configuration` lives in `io.amichne.konditional.serialization.instance.Configuration` and implements `ConfigurationView` (from `:konditional-core`).

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

    fun decode(json: String, options: SnapshotLoadOptions): ParseResult<Configuration>
}
```

### Precondition

Features must be registered before parsing. In practice: ensure your `Namespace` objects (and their delegated feature
properties) have been initialized before calling `decode(...)`.

See [Fundamentals: Definition vs Initialization](/fundamentals/definition-vs-initialization).

### Example

```kotlin
when (val result = ConfigurationSnapshotCodec.decode(json)) {
    is ParseResult.Success -> AppFeatures.load(result.value)
    is ParseResult.Failure -> logger.error { "Parse failed: ${result.error.message}" }
}
```

---

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

### Example

```kotlin
val currentConfig = AppFeatures.configuration
when (val result = ConfigurationSnapshotCodec.applyPatchJson(currentConfig, patchJson)) {
    is ParseResult.Success -> AppFeatures.load(result.value)
    is ParseResult.Failure -> logger.error { "Patch failed: ${result.error.message}" }
}
```

Patch JSON shape is documented in [Persistence & Storage Format](/persistence-format).

---

## `NamespaceSnapshotLoader<M>`

Namespace-scoped JSON loader.

On success, `load(...)` loads the new configuration into the namespace.

```kotlin
class NamespaceSnapshotLoader<M : Namespace>(namespace: M) {
    fun load(json: String): ParseResult<Configuration>
    fun load(json: String, options: SnapshotLoadOptions): ParseResult<Configuration>
}
```

Encoding a namespace snapshot remains explicit and side-effect free:

```kotlin
val json = ConfigurationSnapshotCodec.encode(AppFeatures.configuration)
```

---

## `SnapshotLoadOptions`

Controls how unknown feature keys are handled during snapshot/patch loads.

```kotlin
data class SnapshotLoadOptions(
    val unknownFeatureKeyStrategy: UnknownFeatureKeyStrategy = UnknownFeatureKeyStrategy.Fail,
    val onWarning: (SnapshotWarning) -> Unit = {},
) {
    companion object {
        fun strict(): SnapshotLoadOptions
        fun skipUnknownKeys(onWarning: (SnapshotWarning) -> Unit = {}): SnapshotLoadOptions
    }
}
```

### `UnknownFeatureKeyStrategy`

```kotlin
sealed interface UnknownFeatureKeyStrategy {
    data object Fail : UnknownFeatureKeyStrategy
    data object Skip : UnknownFeatureKeyStrategy
}
```

### `SnapshotWarning`

```kotlin
data class SnapshotWarning(
    val kind: Kind,
    val message: String,
    val key: FeatureId? = null,
) {
    enum class Kind { UNKNOWN_FEATURE_KEY }
}
```

### Example: forward-compatible loads

```kotlin
val options = SnapshotLoadOptions.skipUnknownKeys { warning ->
    logger.warn { warning.message }
}

when (val result = ConfigurationSnapshotCodec.decode(json, options)) {
    is ParseResult.Success -> AppFeatures.load(result.value)
    is ParseResult.Failure -> logger.error { result.error.message }
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

---

## Next Steps

- [Namespace Operations](/api-reference/namespace-operations) — Load/rollback/kill-switch
- [Feature Operations](/api-reference/feature-operations) — Evaluate/explain/shadow
- [Persistence & Storage Format](/persistence-format) — Snapshot/patch JSON shapes
