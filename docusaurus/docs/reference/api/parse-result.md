---
title: Boundary Result API
---

# Boundary Result API

`ParseResult` has been removed from Konditional's public API.

Konditional boundary APIs now return Kotlin `Result<T>`:

- Success: `Result.success(value)`
- Failure: `Result.failure(KonditionalBoundaryFailure(parseError))`

Use `parseErrorOrNull()` helpers to recover structured `ParseError` data:

```kotlin
val result = NamespaceSnapshotLoader(AppFeatures).load(json)

result
  .onSuccess { materialized ->
    AppFeatures.load(materialized)
  }
  .onFailure { failure ->
    val parseError = failure.parseErrorOrNull()
    logger.error { parseError?.message ?: failure.message.orEmpty() }
  }
```

## Structured Failure Type

```kotlin
class KonditionalBoundaryFailure(
  val parseError: ParseError,
) : RuntimeException(parseError.message)
```

## Helpers

```kotlin
fun Throwable.parseErrorOrNull(): ParseError?
fun <T> Result<T>.parseErrorOrNull(): ParseError?
```

## Related

- [Serialization API Reference](/serialization/reference)
- [NamespaceSnapshotLoader API](/reference/api/snapshot-loader)
- [Parse Don't Validate Theory](/theory/parse-dont-validate)
