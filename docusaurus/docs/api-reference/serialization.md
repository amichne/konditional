# Serialization

API reference for JSON snapshot/patch operations: parsing, serialization, and incremental updates.

---

## `SnapshotSerializer.fromJson(json, options): ParseResult<Configuration>`

Parse a JSON snapshot into a validated `Configuration`.

```kotlin
object SnapshotSerializer {
    fun fromJson(
        json: String,
        options: SnapshotLoadOptions = SnapshotLoadOptions.default
    ): ParseResult<Configuration>
}
```

### Parameters

- `json` — JSON snapshot payload
- `options` — Optional loading options (lenient mode, unknown key handling)

### Returns

- `ParseResult.Success(configuration)` if valid
- `ParseResult.Failure(error)` if parse fails

### Example

```kotlin
val json = File("flags.json").readText()

when (val result = SnapshotSerializer.fromJson(json)) {
    is ParseResult.Success -> {
        AppFeatures.load(result.value)
    }
    is ParseResult.Failure -> {
        logger.error("Parse failed: ${result.error.message}")
    }
}
```

### Validation Checks

- JSON syntax validity
- Schema structure (flags array, feature keys, value types)
- Feature existence (keys must be registered)
- Type correctness (values match declared types)

### Precondition

Features must be registered before parsing. See [Fundamentals: Definition vs Initialization](/fundamentals/definition-vs-initialization).

---

## `SnapshotSerializer.toJson(configuration): String`

Serialize a `Configuration` to JSON.

```kotlin
object SnapshotSerializer {
    fun toJson(configuration: Configuration): String
}
```

### Parameters

- `configuration` — Configuration snapshot to serialize

### Returns

JSON string representation.

### Example

```kotlin
val currentConfig = AppFeatures.configuration
val json = SnapshotSerializer.toJson(currentConfig)
File("backup.json").writeText(json)
```

---

## `SnapshotSerializer.applyPatchJson(config, patchJson): ParseResult<Configuration>`

Apply an incremental patch to a configuration.

```kotlin
object SnapshotSerializer {
    fun applyPatchJson(
        baseConfig: Configuration,
        patchJson: String
    ): ParseResult<Configuration>
}
```

### Parameters

- `baseConfig` — Current configuration
- `patchJson` — JSON patch payload

### Returns

- `ParseResult.Success(newConfig)` if patch is valid
- `ParseResult.Failure(error)` if patch fails

### Example

```kotlin
val patchJson = """
{
  "flags": [
    {
      "key": "feature::app::darkMode",
      "defaultValue": { "type": "BOOLEAN", "value": false },
      "rules": [ ... ]
    }
  ],
  "removeKeys": ["feature::app::LEGACY_FEATURE"]
}
"""

val currentConfig = AppFeatures.configuration
when (val result = SnapshotSerializer.applyPatchJson(currentConfig, patchJson)) {
    is ParseResult.Success -> AppFeatures.load(result.value)
    is ParseResult.Failure -> logger.error("Patch failed: ${result.error}")
}
```

### Patch Operations

- **Add/Update flags** — Flags in `"flags"` array
- **Remove flags** — Keys in `"removeKeys"` array

---

## `SnapshotLoadOptions`

Configuration options for snapshot deserialization.

```kotlin
data class SnapshotLoadOptions(
    val skipUnknownKeys: Boolean = false,
    val onUnknownKey: ((UnknownKeyWarning) -> Unit)? = null
)
```

### Fields

- `skipUnknownKeys` — If `true`, unknown keys are skipped instead of failing
- `onUnknownKey` — Callback invoked when unknown keys are encountered

### Example: Lenient Deserialization

```kotlin
val options = SnapshotLoadOptions(
    skipUnknownKeys = true,
    onUnknownKey = { warning ->
        logger.warn("Skipping unknown key: ${warning.key}")
    }
)

when (val result = SnapshotSerializer.fromJson(json, options)) {
    is ParseResult.Success -> AppFeatures.load(result.value)
    is ParseResult.Failure -> logger.error(result.error.message)
}
```

### Use Cases

- Forward compatibility during migrations
- Gradual feature rollout (config includes future features)
- Dev/staging with different feature sets than production

---

## `NamespaceSnapshotSerializer<M>.fromJson(json): ParseResult<Unit>`

Namespace-scoped deserialization (convenience wrapper).

```kotlin
class NamespaceSnapshotSerializer<M : Namespace>(namespace: M) {
    fun fromJson(json: String): ParseResult<Unit>
}
```

### Example

```kotlin
val serializer = NamespaceSnapshotSerializer(AppFeatures)

when (val result = serializer.fromJson(json)) {
    is ParseResult.Success -> logger.info("Loaded")
    is ParseResult.Failure -> logger.error(result.error.message)
}
```

---

## `Configuration.withMetadata(version, source, timestamp): Configuration`

Attach metadata to a configuration snapshot.

```kotlin
fun Configuration.withMetadata(
    version: String? = null,
    source: String? = null,
    generatedAtEpochMillis: Long? = null
): Configuration
```

### Parameters

- `version` — Version identifier (e.g., "rev-123")
- `source` — Source identifier (e.g., "s3://configs/global.json")
- `generatedAtEpochMillis` — Timestamp in epoch milliseconds

### Returns

New `Configuration` with updated metadata.

### Example

```kotlin
when (val result = SnapshotSerializer.fromJson(json)) {
    is ParseResult.Success -> {
        val withMeta = result.value.withMetadata(
            version = "rev-456",
            source = "s3://configs/production.json",
            generatedAtEpochMillis = System.currentTimeMillis()
        )
        AppFeatures.load(withMeta)
    }
    is ParseResult.Failure -> logger.error(result.error.message)
}
```

---

## `ParseError` (Sealed Interface)

Error types returned in `ParseResult.Failure`.

```kotlin
sealed interface ParseError {
    data class InvalidJson(val message: String) : ParseError
    data class InvalidSnapshot(val message: String) : ParseError
    data class FeatureNotFound(val key: String) : ParseError
    data class TypeMismatch(val key: String, val expected: String, val actual: String) : ParseError
}
```

### Handling Parse Errors

```kotlin
when (val result = SnapshotSerializer.fromJson(json)) {
    is ParseResult.Failure -> {
        when (val error = result.error) {
            is ParseError.InvalidJson -> logger.error("Malformed JSON: ${error.message}")
            is ParseError.FeatureNotFound -> logger.error("Unknown feature: ${error.key}")
            is ParseError.TypeMismatch -> logger.error("Type mismatch for ${error.key}")
            is ParseError.InvalidSnapshot -> logger.error("Invalid snapshot: ${error.message}")
        }
    }
}
```

---

## Next Steps

- [Namespace Operations](/api-reference/namespace-operations) — Load, rollback, kill-switch
- [Feature Operations](/api-reference/feature-operations) — Evaluation API
- [Persistence Format](/persistence-format) — JSON schema reference
- [Fundamentals: Configuration Lifecycle](/fundamentals/configuration-lifecycle) — Lifecycle details
