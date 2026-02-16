---
title: NamespaceSnapshotLoader API
---

# NamespaceSnapshotLoader API

`NamespaceSnapshotLoader` performs namespace-scoped decode + ingest.

## Type Signature

```kotlin
class NamespaceSnapshotLoader<M : Namespace>(
    private val namespace: M,
    private val codec: SnapshotCodec<MaterializedConfiguration> = ConfigurationSnapshotCodec,
) : SnapshotLoader<MaterializedConfiguration>
```

## `load(json, options)`

```kotlin
override fun load(
    json: String,
    options: SnapshotLoadOptions = SnapshotLoadOptions.strict(),
): Result<MaterializedConfiguration>
```

Semantics:

- Success:
  - returns `Result.success(materializedConfiguration)`
  - updates namespace runtime snapshot atomically
- Failure:
  - returns `Result.failure(KonditionalBoundaryFailure(parseError))`
  - active snapshot is unchanged

## Example

```kotlin
val loader = NamespaceSnapshotLoader(AppFeatures)

loader.load(fetchRemoteConfig())
  .onSuccess { materialized ->
    logger.info { "Loaded version=${materialized.configuration.metadata.version}" }
  }
  .onFailure { failure ->
    val parseError = failure.parseErrorOrNull()
    logger.error { parseError?.message ?: failure.message.orEmpty() }
  }
```

## Related

- [Serialization API Reference](/serialization/reference)
- [Boundary Result API](/reference/api/parse-result)
