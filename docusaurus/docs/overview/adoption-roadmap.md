---
title: Adoption Roadmap
sidebar_position: 5
---

# Adoption Roadmap

This roadmap favors low-risk rollout: prove value quickly, keep rollback immediate, and expand only after deterministic evidence.

<span id="claim-clm-pr01-05a"></span>
Adoption phases rely on runtime `load`, `disableAll`, and `rollback` operations.

## Phase 1: Pilot (One Namespace)

- Scope one production-adjacent namespace.
- Keep fallback defaults conservative.
- Load snapshots with strict options first.

## Phase 2: Operationalize

- Add automated config delivery and boundary-failure alerting.
- Track evaluation and load metrics in CI/CD and runtime observability.
- Document rollback drills with explicit owner responsibilities.

## Phase 3: Broad Rollout

- Expand namespace ownership to multiple teams.
- Standardize governance for schema and snapshot review.
- Add migration/shadow checks for legacy systems.

## Rollback Posture Per Phase

- Use `rollback(steps)` for fast reversal to known-good snapshots.
- Use `disableAll()` only as emergency kill switch.
- Keep last-known-good snapshots and reproducible load pipeline artifacts.

## Next Steps

- [Migration from Legacy](/guides/migration-from-legacy) - Add dual-run without behavior drift.
- [Enterprise Adoption](/guides/enterprise-adoption) - Integrate CI/CD and telemetry controls.

## Claim Coverage

| Claim ID | Statement |
| --- | --- |
| CLM-PR01-05A | Adoption phases rely on load, disable, and rollback runtime operations. |
