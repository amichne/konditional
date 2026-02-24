---
title: Atomicity Guarantees
sidebar_position: 5
---

# Atomicity Guarantees

## Invariant

Readers never observe partial configuration updates.

## Operational Contract

- Runtime load swaps complete snapshots atomically.
- Concurrent readers observe old or new snapshot, never mixed state.
- Rollback restores complete prior snapshots.

## Scope

Guarantee applies within each namespace runtime registry instance.

## Test Evidence

| Test | Evidence |
| --- | --- |
| `NamespaceLinearizabilityTest` | Load/read operations remain linearizable under concurrency. |
| `ConcurrencyAttacksTest` | Concurrent stress cases do not expose partial state to readers. |

## Next Steps

- [Concept: Configuration Lifecycle](/concepts/configuration-lifecycle)
- [Guide: Remote Configuration](/guides/remote-configuration)
