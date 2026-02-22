# Start here

Konditional gives you a typed and deterministic feature control plane where
runtime configuration is constrained by explicit boundary outcomes.

## Read this page when

- You are deciding where to start with Konditional.
- You need the core guarantees before implementation.
- You want the shortest path from overview to runnable setup.

## Typed foundations

Feature ownership is namespace-scoped and declarations are typed at compile time
through namespace-owned feature definitions [CLM-PR01-01A].

## Boundary-safe ingestion

Runtime snapshots are loaded through a `Result` boundary, so parse failures stay
typed and inspectable instead of mutating behavior silently
[CLM-PR01-01B].

## How this docs set is organized

Use `overview/*` to understand fit and architecture decisions, then move to
`quickstart/*` for implementation with deterministic verification.

## Next steps

1. Confirm whether Konditional matches your constraints in
   [Product value and fit](/overview/product-value-fit).
2. Pick an implementation path in
   [First success map](/overview/first-success-map).
3. Start coding with [Quickstart](/quickstart/).

## Claim citations

| Claim ID | Explicit claim | Local evidence linkage | Registry link |
|---|---|---|---|
| CLM-PR01-01A | Konditional models feature declarations through namespace-owned typed feature definitions. | `#typed-foundations` | `/reference/claims-registry#clm-pr01-01a` |
| CLM-PR01-01B | Runtime configuration ingestion is exposed through a snapshot loader that returns Result and supports typed parse failures. | `#boundary-safe-ingestion` | `/reference/claims-registry#clm-pr01-01b` |
