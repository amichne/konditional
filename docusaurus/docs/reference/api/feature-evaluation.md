---
title: Feature Evaluation API
description: Public feature evaluation surface.
---

# Feature Evaluation API

Public evaluation entrypoint:

```kotlin
fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluate(
    context: C,
    registry: NamespaceRegistry = namespace,
): T
```

Notes:

- `evaluate(...)` is the supported public API.
- Legacy public APIs removed:
  - `evaluateSafely(...)`
  - `explainSafely(...)`
  - `explain(...)`
  - `evaluateWithReason(...)`
  - public `EvaluationResult<T>`
- Internal diagnostics remain available for sibling modules via internal API opt-in.

## Related

- [Namespace Operations API](/reference/api/namespace-operations)
- [Observability Reference](/observability/reference)
