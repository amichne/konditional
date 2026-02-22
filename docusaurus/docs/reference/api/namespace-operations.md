---
title: Namespace operations API
---

# Namespace operations API

This page defines the runtime mutation and lifecycle extensions for namespaces.

## Read this page when

- You need to load trusted materialized configuration into a namespace.
- You need rollback/history contracts for runtime operations.
- You are validating kill-switch behavior and scope.

## API and contract reference

```kotlin
fun Namespace.load(configuration: MaterializedConfiguration)
fun Namespace.rollback(steps: Int = 1): Boolean
val Namespace.history: List<ConfigurationView>
val Namespace.historyMetadata: List<ConfigurationMetadataView>
```

Kill-switch controls from `NamespaceRegistry`:

```kotlin
fun disableAll()
fun enableAll()
val isAllDisabled: Boolean
```

Runtime contract:

- `load(...)` requires `:konditional-runtime` and loads only trusted materialized
  values.
- `rollback(...)` returns `false` when requested history depth is unavailable.
- History is namespace-scoped.
- Kill-switch scope is namespace-local and returns defaults when enabled.

## Deterministic API and contract notes

- Default runtime implementation uses atomic snapshot references; readers see
  either old or new snapshots, never partial updates.
- `rollback(steps)` is linearizable with synchronized write coordination.
- Namespace isolation prevents cross-namespace mutation effects.

## Canonical conceptual pages

- [Theory: Atomicity guarantees](/theory/atomicity-guarantees)
- [Theory: Namespace isolation](/theory/namespace-isolation)
- [How-to: Namespace isolation](/how-to-guides/namespace-isolation)

## Next steps

- [Namespace snapshot loader API](/reference/api/snapshot-loader)
- [Boundary result API](/reference/api/parse-result)
- [Feature evaluation API](/reference/api/feature-evaluation)
