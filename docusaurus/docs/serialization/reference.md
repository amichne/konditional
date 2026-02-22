# Serialization reference

This page is the boundary API reference for snapshot encode/decode and patch
application.

## Read this page when

- You need exact function signatures for parse-boundary code.
- You are wiring `Result`-based load flows.
- You are configuring unknown-key or missing-flag policies.

## APIs in scope

### `ConfigurationSnapshotCodec.encode(...)`

```kotlin
object ConfigurationSnapshotCodec {
    fun encodeRaw(value: Configuration): String
    fun encode(value: MaterializedConfiguration): String
    fun encode(value: ConfigurationView): String
}
```

### `ConfigurationSnapshotCodec.decode(...)`

```kotlin
object ConfigurationSnapshotCodec {
    fun decode(
        json: String,
        schema: CompiledNamespaceSchema,
        options: SnapshotLoadOptions = SnapshotLoadOptions.strict(),
    ): Result<MaterializedConfiguration>
}
```

### `ConfigurationSnapshotCodec.applyPatchJson(...)`

```kotlin
object ConfigurationSnapshotCodec {
    fun applyPatchJson(
        currentConfiguration: MaterializedConfiguration,
        patchJson: String,
        options: SnapshotLoadOptions = SnapshotLoadOptions.strict(),
    ): Result<MaterializedConfiguration>
}
```

### `SnapshotLoadOptions`

```kotlin
data class SnapshotLoadOptions(
    val unknownFeatureKeyStrategy: UnknownFeatureKeyStrategy,
    val missingDeclaredFlagStrategy: MissingDeclaredFlagStrategy,
    val onWarning: (SnapshotWarning) -> Unit,
)
```

## Boundary contract

- Parse failures are returned as `Result.failure(...)`.
- `parseErrorOrNull()` gives typed parse details.
- Invalid payloads never become active snapshots by default.

## Related pages

- [Serialization module](/serialization)
- [Persistence format](/serialization/persistence-format)
- [Runtime operations](/runtime/operations)
- [Parse don’t validate](/theory/parse-dont-validate)

## Next steps

1. Choose strict or relaxed load options for your environment.
2. Add boundary tests for parse error paths.
3. Integrate load and rollback in [Runtime lifecycle](/runtime/lifecycle).
