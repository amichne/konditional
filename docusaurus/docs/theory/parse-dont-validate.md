# Parse Don’t Validate

Konditional treats JSON/config input as untrusted and parses it into trusted typed models.

## Boundary Contract

Boundary APIs return Kotlin `Result<T>`:

- `Result.success(trustedValue)`
- `Result.failure(KonditionalBoundaryFailure(parseError))`

This keeps the boundary explicit while using Kotlin-native result handling.

## Why This Prevents Invalid State

- Untrusted payloads are rejected before they reach runtime snapshots.
- Runtime ingest accepts only `MaterializedConfiguration`.
- Registry updates occur only on successful materialization.
- Last-known-good snapshot remains active on failures.

## Structural Planes

- Schema plane: `CompiledNamespaceSchema` (compile-time declarations)
- Data plane: incoming payload JSON
- Pure materialization: `materialize(schema, data, options) -> Result<MaterializedConfiguration>`

## Missing Declared Flag Policy

`SnapshotLoadOptions.missingDeclaredFlagStrategy` controls absent schema flags:

- `Reject` (default)
- `FillFromDeclaredDefaults`

## Example

```kotlin
val result = ConfigurationSnapshotCodec.decode(json, AppFlags.compiledSchema())

result
  .onSuccess { materialized -> AppFlags.load(materialized) }
  .onFailure { failure ->
    val parseError = result.parseErrorOrNull()
    logger.error { parseError?.message ?: failure.message.orEmpty() }
  }
```

## Related

- [Serialization API Reference](/serialization/reference)
- [Snapshot Loader API](/reference/api/snapshot-loader)
