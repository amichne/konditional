---
title: Feature evaluation API
description: Public feature evaluation surface.
---

# Feature evaluation API

This page defines the stable public evaluation entrypoint for feature reads.

## Read this page when

- You need the supported evaluation call in application code.
- You are migrating from removed explain/safe evaluation public APIs.
- You want to verify error and determinism behavior at call sites.

## API and contract reference

```kotlin
fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluate(
    context: C,
    registry: NamespaceRegistry = namespace,
): T
```

Contract:

- Returns the evaluated typed value.
- Uses the feature's namespace registry by default.
- Throws `IllegalStateException` if the feature is missing from the registry.

Removed public APIs:

- `evaluateSafely(...)`
- `explainSafely(...)`
- `explain(...)`
- `evaluateWithReason(...)`
- public `EvaluationResult<T>`

## Deterministic API and contract notes

- For fixed `(context, registry snapshot)`, `evaluate(...)` returns a stable
  value.
- Evaluation order is deterministic through rule specificity and stable
  tie-breaking.
- Snapshot reads are atomic at runtime, so evaluation observes whole
  configurations.

## Canonical conceptual pages

- [Theory: Determinism proofs](/theory/determinism-proofs)
- [Theory: Atomicity guarantees](/theory/atomicity-guarantees)
- [Theory: Type safety boundaries](/theory/type-safety-boundaries)

## Next steps

- [Namespace operations API](/reference/api/namespace-operations)
- [Ramp-up bucketing API](/reference/api/ramp-up-bucketing)
- [Observability reference](/observability/reference)
