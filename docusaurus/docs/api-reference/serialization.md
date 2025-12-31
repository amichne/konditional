# Serialization

API reference for JSON snapshot/patch operations: parsing, serialization, and incremental updates.

---

## `SnapshotSerializer.serialize(configuration): String`

Serialize a `Configuration` to JSON.

```kotlin
object SnapshotSerializer {
    fun serialize(configuration: Configuration): String
}
```

### Example

```kotlin
val snapshotJson = SnapshotSerializer.serialize(AppFeatures.configuration)
persistToStorage(snapshotJson)
```

---

## `SnapshotSerializer.fromJson(json, options): ParseResult<Configuration>`

Parse a snapshot JSON payload into a validated `Configuration`.

```kotlin
object SnapshotSerializer {
    fun fromJson(json: String): ParseResult<Configuration>

    fun fromJson(
        json: String,
        options: SnapshotLoadOptions,
    ): ParseResult<Configuration>
}
```

### Precondition

Features must be registered before parsing. In practice: ensure your `Namespace` objects (and their delegated feature
properties) have been initialized before calling `fromJson(...)`.

See [Fundamentals: Definition vs Initialization](/fundamentals/definition-vs-initialization).

### Example

```kotlin
when (val result = SnapshotSerializer.fromJson(json)) {
    is ParseResult.Success -> AppFeatures.load(result.value)
    is ParseResult.Failure -> logger.error { "Parse failed: ${result.error.message}" }
}
```

---

## `SnapshotSerializer.applyPatchJson(configuration, patchJson, options): ParseResult<Configuration>`

Apply an incremental patch to an existing configuration snapshot.

```kotlin
object SnapshotSerializer {
    fun applyPatchJson(
        currentConfiguration: Configuration,
        patchJson: String,
    ): ParseResult<Configuration>

    fun applyPatchJson(
        currentConfiguration: Configuration,
        patchJson: String,
        options: SnapshotLoadOptions,
    ): ParseResult<Configuration>
}
```

### Example

```kotlin
val currentConfig = AppFeatures.configuration
when (val result = SnapshotSerializer.applyPatchJson(currentConfig, patchJson)) {
    is ParseResult.Success -> AppFeatures.load(result.value)
    is ParseResult.Failure -> logger.error { "Patch failed: ${result.error.message}" }
}
```

Patch JSON shape is documented in [Persistence & Storage Format](/persistence-format).

---

## `NamespaceSnapshotSerializer<M>`

Namespace-scoped JSON serializer/deserializer.

On success, `fromJson(...)` loads the new configuration into the namespace.

```kotlin
class NamespaceSnapshotSerializer<M : Namespace>(namespace: M) : Serializer<Configuration> {
    override fun toJson(): String
    override fun fromJson(json: String): ParseResult<Configuration>

    fun fromJson(json: String, options: SnapshotLoadOptions): ParseResult<Configuration>
}
```

For the common case you can use the `Namespace` convenience methods:

- `Namespace.toJson(): String`
- `Namespace.fromJson(json: String): ParseResult<Configuration>`

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

when (val result = SnapshotSerializer.fromJson(json, options)) {
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
