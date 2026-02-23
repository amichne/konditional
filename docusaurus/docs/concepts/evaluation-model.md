---
title: Evaluation Model
sidebar_position: 5
---

# Evaluation Model

Evaluation is total: for any declared feature and valid context, `evaluate(...)` returns a typed value.

## Execution Flow

1. Resolve current namespace snapshot.
2. Resolve feature definition for requested feature key.
3. If namespace kill-switch or feature inactive, return default.
4. Evaluate rules by deterministic precedence.
5. Apply ramp-up gate using stable bucketing when configured.
6. Return matched value or default fallback.

## Determinism Boundaries

Determinism assumes stable inputs:

- same feature declaration
- same loaded snapshot
- same context values (including stable ID)

Any change in those inputs can change the result, by design.

## Failure Posture

Evaluation itself does not throw parse errors. Boundary failures occur at snapshot/patch ingestion time and are surfaced as typed `Result` failures.

## Next Steps

- [Rules and Precedence](/concepts/rules-and-precedence)
- [Determinism Proofs](/theory/determinism-proofs)
