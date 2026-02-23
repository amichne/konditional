---
title: Configuration Lifecycle
sidebar_position: 7
---

# Configuration Lifecycle

Runtime configuration moves through controlled lifecycle operations scoped to one namespace.

## Lifecycle Operations

- `load(config)`: atomically swap in a trusted configuration.
- `rollback(steps)`: move back to a previous snapshot.
- `disableAll()`: emergency kill-switch to force declared defaults.
- `enableAll()`: restore normal evaluation behavior.

## Atomicity Posture

Readers observe either the old snapshot or the new snapshot. They do not observe partial updates.

## Operational Guidance

- Keep snapshot history small but sufficient for incident rollback windows.
- Run rollback drills in staging and production-like load.
- Treat disable-all as emergency control, not steady-state routing.

## Next Steps

- [Remote Configuration Guide](/guides/remote-configuration)
- [Atomicity Guarantees Theory](/theory/atomicity-guarantees)
