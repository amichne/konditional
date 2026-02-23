---
title: Namespace Isolation
sidebar_position: 3
---

# Namespace Isolation

## Invariant

Operations and state are scoped to a namespace; updates in namespace `A` do not mutate namespace `B`.

## Isolation Guarantees

- Declarations are namespace-owned.
- Runtime load/rollback/disable operations act on one namespace registry instance.
- Evaluations read from their owning namespace runtime state.

## Consequences

- Blast radius remains bounded by namespace ownership.
- Multi-team deployments can evolve namespaces independently.
- Incident rollback can be performed without cross-namespace side effects.

## Test Evidence

| Test | Evidence |
| --- | --- |
| `NamespaceLinearizabilityTest` | Concurrent operations preserve per-namespace atomic state transitions. |
| `NamespaceFeatureDefinitionTest` | Feature declarations remain scoped to their owning namespace. |

## Next Steps

- [Guide: Namespace per Team](/guides/namespace-per-team)
- [Concept: Namespaces](/concepts/namespaces)
