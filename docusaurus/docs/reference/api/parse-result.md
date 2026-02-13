---
title: ParseResult API
---

# ParseResult API

`ParseResult<T>` is Konditional's typed parse boundary. It represents either a parsed value or a structured parse error without using exceptions for normal control flow.

In this page you will find:

- The concrete `ParseResult` shape and constructor constraints
- The companion factories and extension utilities
- Composition patterns that stay in typed error space

---

## Type Shape

```kotlin
sealed interface ParseResult<out T> {
    data class Success<T> internal constructor(val value: T) : ParseResult<T>
    data class Failure internal constructor(val error: ParseError) : ParseResult<Nothing>

    companion object {
        fun <T> success(value: T): ParseResult<T>
        fun failure(error: ParseError): ParseResult<Nothing>
    }

    fun getOrThrow(): T
}
```

Evidence:

- `konditional-core/src/main/kotlin/io/amichne/konditional/core/result/ParseResult.kt`

### Public construction

`Success(...)` and `Failure(...)` constructors are `internal`. External code should create results through:

```kotlin
ParseResult.success(value)
ParseResult.failure(parseError)
```

---

## Guarantees and Boundaries

- Guarantee: parse APIs return either a typed value or a typed `ParseError`.
- Guarantee: exhaustive `when` on `ParseResult` is compiler-checkable.
- Boundary: business semantics are not validated by `ParseResult` itself.
- Boundary: `getOrThrow()` converts failures into an exception path and should be treated as fail-fast behavior.

---

## Core Usage

### Exhaustive handling

```kotlin
when (val result = ConfigurationSnapshotCodec.decode(json)) {
    is ParseResult.Success -> namespace.load(result.value)
    is ParseResult.Failure -> logger.error { "Parse failed: ${result.error.message}" }
}
```

### Folding into your own result type

```kotlin
val message = result.fold(
    onSuccess = { config -> "Loaded ${config.flags.size} flags" },
    onFailure = { error -> "Rejected: ${error.message}" },
)
```

---

## Extension Utilities (`core.result.utils`)

Import:

```kotlin
import io.amichne.konditional.core.result.utils.*
```

Available helpers:

- `fold(onSuccess, onFailure)`
- `map(transform)`
- `flatMap(transform)`
- `getOrNull()`
- `getOrDefault(default)`
- `getOrElse(onFailure)`
- `isSuccess()` / `isFailure()`
- `onSuccess(action)` / `onFailure(action)`
- `recover(transform)`
- `toResult()`

Evidence:

- `konditional-core/src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt`

### Composition example

```kotlin
val loadable = ConfigurationSnapshotCodec.decode(json)
    .onFailure { error -> logger.warn { "Decode rejected: ${error.message}" } }
    .map { config -> config.withMetadata(source = "remote") }

when (loadable) {
    is ParseResult.Success -> AppFeatures.load(loadable.value)
    is ParseResult.Failure -> Unit
}
```

---

## ParseError Families

Common parse failures returned via `ParseResult.Failure`:

- `ParseError.InvalidJson`
- `ParseError.InvalidSnapshot`
- `ParseError.FeatureNotFound`
- `ParseError.FlagNotFound`
- `ParseError.InvalidHexId`
- `ParseError.InvalidRollout`
- `ParseError.InvalidVersion`

Evidence:

- `konditional-core/src/main/kotlin/io/amichne/konditional/core/result/ParseError.kt`

---

## Related

- [Parse Don't Validate](/theory/parse-dont-validate)
- [NamespaceSnapshotLoader API](/reference/api/snapshot-loader)
- [Serialization API Reference](/serialization/reference)
