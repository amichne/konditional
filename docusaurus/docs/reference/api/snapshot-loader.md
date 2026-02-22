---
title: NamespaceSnapshotLoader API
---

# NamespaceSnapshotLoader API

This page defines the namespace-scoped snapshot ingestion contract.

## Read this page when

- You are decoding untrusted JSON into trusted materialized configuration.
- You need load semantics for success and failure paths.
- You need to choose strict vs. migration-oriented load options.

## API and contract reference

### Side-effecting loader contract

```kotlin
interface SnapshotLoader<T> {
    fun load(
        json: String,
        options: SnapshotLoadOptions = SnapshotLoadOptions.strict(),
    ): Result<T>
}
```

### Namespace-scoped implementation

```kotlin
class NamespaceSnapshotLoader<M : Namespace>(
    private val namespace: M,
    private val codec: SnapshotCodec<MaterializedConfiguration> =
        ConfigurationSnapshotCodec,
) : SnapshotLoader<MaterializedConfiguration> {
    override fun load(
        json: String,
        options: SnapshotLoadOptions,
    ): Result<MaterializedConfiguration>

    companion object {
        fun <M : Namespace> forNamespace(namespace: M): NamespaceSnapshotLoader<M>
    }
}
```

### Option policies

```kotlin
data class SnapshotLoadOptions(
    val unknownFeatureKeyStrategy: UnknownFeatureKeyStrategy =
        UnknownFeatureKeyStrategy.Fail,
    val missingDeclaredFlagStrategy: MissingDeclaredFlagStrategy =
        MissingDeclaredFlagStrategy.Reject,
    val onWarning: (SnapshotWarning) -> Unit = {},
)
```

Factories:

- `SnapshotLoadOptions.strict()`
- `SnapshotLoadOptions.skipUnknownKeys(...)`
- `SnapshotLoadOptions.fillMissingDeclaredFlags(...)`

## Deterministic API and contract notes

- Failure returns `Result.failure(KonditionalBoundaryFailure(parseError))` and
  leaves namespace state unchanged.
- Success loads a fully materialized trusted configuration into the namespace
  runtime registry.
- Namespace context is appended to parse errors for load failures.
- For fixed `(json, schema, options)`, decode result shape is deterministic.

## Canonical conceptual pages

- [Theory: Parse don't validate](/theory/parse-dont-validate)
- [Theory: Atomicity guarantees](/theory/atomicity-guarantees)
- [How-to: Safe remote config loading](/how-to-guides/safe-remote-config)

## Next steps

- [Boundary result API](/reference/api/parse-result)
- [Namespace operations API](/reference/api/namespace-operations)
- [Feature evaluation API](/reference/api/feature-evaluation)
