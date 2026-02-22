# Adoption roadmap

Adopt Konditional in phases so you can preserve rollback safety while expanding
typed ownership across teams.

## Read this page when

- Your first quickstart flow is complete.
- You need a staged rollout plan for production adoption.
- You want explicit checkpoints for load, disable, and rollback readiness.

## Phase 1: mirror existing controls

Represent current controls in namespace-scoped typed definitions without
changing behavior envelopes yet [CLM-PR01-05A].

## Phase 2: enable runtime snapshot loading

Move configuration materialization into explicit load operations with typed
boundary handling and auditability [CLM-PR01-05A].

## Phase 3: operational fallback drills

Practice disable and rollback operations in controlled windows before critical
launches [CLM-PR01-05A].

## Next steps

1. Revisit implementation details in [Quickstart](/quickstart/).
2. Strengthen safe ingestion in
   [Load first snapshot safely](/quickstart/load-first-snapshot-safely).
3. Complete release gates with
   [Verify end-to-end](/quickstart/verify-end-to-end).

## Claim citations

| Claim ID | Explicit claim | Local evidence linkage | Registry link |
|---|---|---|---|
| CLM-PR01-05A | Adoption phases rely on load, disable, and rollback runtime operations. | `#phase-3-operational-fallback-drills` | `/reference/claims-registry#clm-pr01-05a` |
