---
title: Namespaces
sidebar_position: 1
---

# Namespaces

Namespaces define the blast-radius boundary for declarations, runtime operations, and rollback.

## What a Namespace Owns

- Feature declarations for one domain scope.
- Runtime configuration state for that scope.
- Lifecycle controls (`load`, `rollback`, `disableAll`, `enableAll`) via delegated registry behavior.

## Example

```kotlin
import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace

object CheckoutFlags : Namespace("checkout") {
  val newCheckout by boolean<Context>(default = false)
}

object SearchFlags : Namespace("search") {
  val semanticRanking by boolean<Context>(default = true)
}
```

Loading `CheckoutFlags` does not mutate `SearchFlags` state.

## Why This Matters

- Isolation: teams can own separate namespaces without cross-team accidental overrides.
- Operational safety: rollback and kill-switch are namespace-scoped.
- Determinism: evaluation reads a single namespace snapshot at a time.

## Next Steps

- [Configuration Lifecycle](/concepts/configuration-lifecycle) - See how namespace runtime state changes safely.
- [Namespace Isolation Theory](/theory/namespace-isolation) - Formal invariants and proof-oriented view.
