---
title: Migration and Shadowing
sidebar_position: 6
---

# Migration and Shadowing

## Invariant

Shadow evaluation must not change baseline behavior returned to callers.

## Algebra

For each evaluation:

1. Baseline result is computed and returned.
2. Candidate result is computed for comparison only.
3. Mismatch signal is emitted if configured.

Caller-visible semantics always equal baseline semantics during migration.

## Mismatch Types

- `VALUE`: baseline and candidate values differ.
- `DECISION`: decision category differs (when decision mismatch reporting is enabled).

## Operational Guidance

- Start with value mismatch reporting only.
- Define acceptable mismatch thresholds before cutover.
- Keep rollback path available until post-cutover stability period ends.

## Test Evidence

| Test | Evidence |
| --- | --- |
| `KillSwitchTest` | Baseline safety controls remain authoritative during rollout/migration workflows. |

## Next Steps

- [Guide: Migration from Legacy](/guides/migration-from-legacy)
- [Guide: Enterprise Adoption](/guides/enterprise-adoption)
