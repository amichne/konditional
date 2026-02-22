# Migration guide

This page is the API migration reference from older Konditional surfaces to the
current split-module contracts.

## Read this page when

- You are moving from legacy `io.amichne:konditional` usage.
- You are replacing removed APIs with current equivalents.
- You need dependency and symbol mapping without conceptual walkthroughs.

## API and contract reference

### Module split

```kotlin
dependencies {
    implementation("io.amichne:konditional-core:VERSION")
    implementation("io.amichne:konditional-runtime:VERSION")

    // Optional boundaries
    implementation("io.amichne:konditional-serialization:VERSION")
    implementation("io.amichne:konditional-observability:VERSION")
    implementation("io.amichne:konditional-opentelemetry:VERSION")
    implementation("io.amichne:konditional-kontracts:VERSION")
}
```

Runtime registry implementations are discovered through
`NamespaceRegistryFactory` via `ServiceLoader`.

### Symbol migration map

| Legacy surface | Current surface |
| --- | --- |
| `ParseResult<T>` | Kotlin `Result<T>` + `KonditionalBoundaryFailure` |
| `Feature.evaluateSafely(...)` | `Feature.evaluate(...)` |
| Public explain APIs | Observability and OpenTelemetry extension APIs |
| Monolithic namespace runtime assumptions | `:konditional-runtime` namespace operations extensions |
| Stringly parse failure handling | `ParseError` + `parseErrorOrNull()` helpers |

### Boundary migration snippet

```kotlin
val result = NamespaceSnapshotLoader(AppFeatures).load(json)

result
    .onSuccess { materialized -> AppFeatures.load(materialized) }
    .onFailure { failure ->
        val parseError = failure.parseErrorOrNull()
        logger.error { parseError?.message ?: failure.message.orEmpty() }
    }
```

## Deterministic API and contract notes

- Split modules preserve deterministic evaluation semantics when using the same
  namespace definitions and snapshots.
- Migration does not require changing feature declaration keys; namespace+feature
  identity remains stable.
- Boundary failures are explicit and typed, preventing implicit behavior drift.

## Canonical conceptual pages

- [Theory: Type safety boundaries](/theory/type-safety-boundaries)
- [Theory: Parse don't validate](/theory/parse-dont-validate)
- [Theory: Namespace isolation](/theory/namespace-isolation)
- [Theory: Migration and shadowing](/theory/migration-and-shadowing)

## Next steps

- [Feature evaluation API](/reference/api/feature-evaluation)
- [Namespace operations API](/reference/api/namespace-operations)
- [Boundary result API](/reference/api/parse-result)
- [Namespace snapshot loader API](/reference/api/snapshot-loader)
