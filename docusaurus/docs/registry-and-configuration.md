---
title: Configuration & Registry Surface
---

# Configuration & Registry Surface

Core separates two responsibilities:

- **Definition**: what your flags are (types, defaults, legal values).
- **Configuration**: what is currently loaded and operationally active.

The runtime surface you interact with is a `NamespaceRegistry`, which is reachable via the `Namespace` object itself.

:::note Boundary is explicit
Configuration parsing and validation live in `:konditional-serialization` and `:konditional-runtime`. Core assumes
configuration is already validated; invalid updates are rejected at the boundary and do not partially mutate state.
:::

## NamespaceRegistry

`NamespaceRegistry` exposes:

- `configuration`: a read-only `ConfigurationView` of currently loaded flag definitions
- `isAllDisabled`, `disableAll()`, `enableAll()`: an emergency kill-switch
- `hooks`: lightweight `RegistryHooks` for logging and metrics

Mermaid overview:

```mermaid
flowchart LR
  N["Namespace (also a NamespaceRegistry)"] --> CV["ConfigurationView (flags + metadata)"]
  N --> KS["Kill-switch (disableAll/enableAll)"]
  N --> H["RegistryHooks (logger + metrics)"]
  CV --> FD["FlagDefinition&lt;T, C, Namespace&gt;"]
  FD --> E["Feature.evaluate/explain"]
```

## Why “kill-switch returns defaults”

The kill-switch is designed to be safe under pressure:

- It does not mutate definitions.
- It does not depend on remote configuration.
- It makes all evaluations return the declared default values.

This gives you a single operational lever that cannot create “half disabled” states.

## Atomic snapshot refresh (runtime)

Namespace configuration updates are applied as **atomic snapshot swaps**. Readers see either the old snapshot or the new
snapshot — never a partial state.

```mermaid
sequenceDiagram
    participant W as Writer (refresh thread)
    participant R as Reader (evaluation thread)
    participant AR as Active Snapshot

    par Concurrent work
        W->>AR: set(newSnapshot) (atomic swap)
        and
        R->>AR: get() (lock‑free read)
        alt Read before swap
            AR-->>R: oldSnapshot
        else Read after swap
            AR-->>R: newSnapshot
        end
    end
```

:::danger Do not mutate snapshots
Configuration snapshots are intended to be immutable. Mutating a snapshot after load violates the atomicity model and
can lead to undefined evaluation behavior.
:::

## Configuration lifecycle (high level)

```mermaid
flowchart LR
  Code["Flags defined in code"] --> Snap["Snapshot encode"]
  Snap --> Json["JSON snapshot"]
  Json --> Parse["Parse + validate"]
  Parse -->|Success| Load["Load (atomic swap)"]
  Parse -->|Failure| Reject["Reject + keep last-known-good"]
  Load --> Eval["Evaluation reads active snapshot"]
  style Load fill: #c8e6c9
  style Reject fill: #ffcdd2
```

Next:

- [Observability & Debugging](/observability-and-debugging)
