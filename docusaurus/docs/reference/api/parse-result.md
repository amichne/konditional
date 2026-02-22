---
title: Boundary result API
---

# Boundary result API

This page defines the public boundary result contract after `ParseResult`
removal.

## Read this page when

- You are handling decode/load failures at JSON boundaries.
- You are migrating from custom result wrappers to Kotlin `Result<T>`.
- You need typed parse failure extraction helpers.

## API and contract reference

`ParseResult` is not part of the public API. Boundary APIs return Kotlin
`Result<T>` values.

Success channel:

- `Result.success(value)`

Failure channel:

- `Result.failure(KonditionalBoundaryFailure(parseError))`

Structured boundary failure wrapper and helpers:

```kotlin
class KonditionalBoundaryFailure(
    val parseError: ParseError,
) : RuntimeException(parseError.message)

fun <T> parseFailure(error: ParseError): Result<T>
fun Throwable.parseErrorOrNull(): ParseError?
fun <T> Result<T>.parseErrorOrNull(): ParseError?
```

Typed parse error taxonomy:

- `ParseError.InvalidJson`
- `ParseError.InvalidSnapshot`
- `ParseError.InvalidHexId`
- `ParseError.InvalidVersion`
- `ParseError.InvalidRollout`
- `ParseError.FeatureNotFound`
- `ParseError.FlagNotFound`

## Deterministic API and contract notes

- Failure shape is deterministic for the same parser state and input payload.
- Parse helpers are pure adapters over `Throwable` and `Result`.
- Invalid boundary input never mutates runtime snapshots when used with loader
  APIs.

## Canonical conceptual pages

- [Theory: Parse don't validate](/theory/parse-dont-validate)
- [Theory: Type safety boundaries](/theory/type-safety-boundaries)
- [How-to: Handling failures](/how-to-guides/handling-failures)

## Next steps

- [Namespace snapshot loader API](/reference/api/snapshot-loader)
- [Namespace operations API](/reference/api/namespace-operations)
- [Migration guide](/reference/migration-guide)
