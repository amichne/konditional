# Serialization API Reference

JSON snapshot and patch APIs at the untrusted boundary.

## `ConfigurationSnapshotCodec.encode(...)`

```kotlin
object ConfigurationSnapshotCodec {
    fun encodeRaw(value: Configuration): String
    fun encode(value: MaterializedConfiguration): String
    fun encode(value: ConfigurationView): String
}
```

## `ConfigurationSnapshotCodec.decode(...)`

```kotlin
object ConfigurationSnapshotCodec {
    fun decode(
        json: String,
        options: SnapshotLoadOptions = SnapshotLoadOptions.strict(),
    ): Result<MaterializedConfiguration>

    fun decode(
        json: String,
        schema: CompiledNamespaceSchema,
        options: SnapshotLoadOptions = SnapshotLoadOptions.strict(),
    ): Result<MaterializedConfiguration>
}
```

Notes:

- `decode(json, options)` without schema is intentionally rejected.
- Successful decode returns trusted `MaterializedConfiguration` only.
- Failures are `Result.failure(KonditionalBoundaryFailure(parseError))`.

## `ConfigurationSnapshotCodec.applyPatchJson(...)`

```kotlin
object ConfigurationSnapshotCodec {
    fun applyPatchJson(
        currentConfiguration: MaterializedConfiguration,
        patchJson: String,
        options: SnapshotLoadOptions = SnapshotLoadOptions.strict(),
    ): Result<MaterializedConfiguration>

    fun applyPatchJson(
        currentConfiguration: ConfigurationView,
        schema: CompiledNamespaceSchema,
        patchJson: String,
        options: SnapshotLoadOptions = SnapshotLoadOptions.strict(),
    ): Result<MaterializedConfiguration>
}
```

## `SnapshotLoadOptions`

```kotlin
data class SnapshotLoadOptions(
    val unknownFeatureKeyStrategy: UnknownFeatureKeyStrategy = UnknownFeatureKeyStrategy.Fail,
    val missingDeclaredFlagStrategy: MissingDeclaredFlagStrategy = MissingDeclaredFlagStrategy.Reject,
    val onWarning: (SnapshotWarning) -> Unit = {},
)
```

Factory modes:

- `SnapshotLoadOptions.strict()`
- `SnapshotLoadOptions.skipUnknownKeys(...)`
- `SnapshotLoadOptions.fillMissingDeclaredFlags(...)`

`MissingDeclaredFlagStrategy`:

- `Reject` (default)
- `FillFromDeclaredDefaults`

## Error Introspection

```kotlin
val result = ConfigurationSnapshotCodec.decode(json, schema)
val parseError: ParseError? = result.parseErrorOrNull()
```

## Related

- [NamespaceSnapshotLoader API](/reference/api/snapshot-loader)
- [Boundary Result API](/reference/api/parse-result)
- [Parse Don’t Validate Theory](/theory/parse-dont-validate)
