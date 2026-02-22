# Adoption roadmap

Adopt Konditional in phases that preserve rollback and disable controls while
you shift traffic to typed feature ownership [CLM-PR01-05A].

## Phase 1: mirror existing controls

Model existing controls in namespace-scoped definitions while preserving current
behavior envelopes [CLM-PR01-05A].

## Phase 2: enable runtime snapshot loading

Use load operations to move config materialization under explicit, auditable
runtime control [CLM-PR01-05A].

## Phase 3: operational fallback drills

Exercise disable and rollback operations during rollout windows before critical
launches [CLM-PR01-05A].

## Claim citations

| Claim ID | Explicit claim | Local evidence linkage | Registry link |
|---|---|---|---|
| CLM-PR01-05A | Adoption phases rely on load, disable, and rollback runtime operations. | `#phase-3-operational-fallback-drills` | `/reference/claims-registry#clm-pr01-05a` |
