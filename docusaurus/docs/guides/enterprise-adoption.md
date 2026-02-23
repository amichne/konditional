---
title: Enterprise Adoption
sidebar_position: 8
---

# Enterprise Adoption

Operationalize Konditional in CI/CD with controlled config delivery, observability, and rollback governance.

**Prerequisites:** You have a validated baseline from [Quickstart](/quickstart/) and [Adoption Roadmap](/overview/adoption-roadmap).

## CI/CD Integration Pattern

1. Validate snapshot/patch payloads in pipeline before publish.
2. Promote immutable config artifacts through staged environments.
3. Load per-namespace in production with typed boundary checks.
4. Gate release on mismatch/error SLOs.

## Delivery Strategies

- Pull model: service fetches versioned config at interval.
- Push model: deployment system publishes signed payloads per namespace.
- Hybrid: push metadata signal, pull immutable artifact.

## Observability Baseline

Track at minimum:

- Snapshot load success/failure counts by namespace.
- Parse error classes by source/version.
- Rollback events and time-to-recovery.
- Shadow mismatch rates during migrations.

## Governance Controls

- Namespace ownership and on-call mapping.
- Required reviewers for schema/feature changes.
- Rollback runbook with explicit execution thresholds.

## Expected Outcome

After this guide, you have an auditable operating model for configuration delivery, rollback, and migration risk control.

## Next Steps

- [Reference: Module Dependency Map](/reference/module-dependency-map)
- [Theory: Atomicity Guarantees](/theory/atomicity-guarantees)
