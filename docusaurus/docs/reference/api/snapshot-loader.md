---
title: NamespaceSnapshotLoader API
---

# NamespaceSnapshotLoader API

`NamespaceSnapshotLoader` performs namespace-scoped decode + ingest.

## Type Signature

```kotlin
class NamespaceSnapshotLoader<M : Namespace> private constructor(
    private val namespace: M,
)
```

## `load(json, options)`

```kotlin
fun load(
    json: String,
    options: SnapshotLoadOptions = SnapshotLoadOptions.strict(),
): Result<Configuration>
```

Semantics:

- Success:
  - returns `Result.success(configuration)`
  - updates namespace runtime snapshot atomically
- Failure:
  - returns `Result.failure(KonditionalBoundaryFailure(parseError))`
  - active snapshot is unchanged

## Example

```kotlin
val loader = NamespaceSnapshotLoader.forNamespace(AppFeatures)

loader.load(fetchRemoteConfig())
  .onSuccess { configuration ->
    logger.info { "Loaded version=${configuration.metadata.version}" }
  }
  .onFailure { failure ->
    val parseError = failure.parseErrorOrNull()
    logger.error { parseError?.message ?: failure.message.orEmpty() }
  }
```

## Related

- [Serialization API Reference](/serialization/reference)
- [Boundary Result API](/reference/api/parse-result)
