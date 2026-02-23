---
title: Determinism Proofs
sidebar_position: 2
---

# Determinism Proofs

## Invariant

Given stable declaration, snapshot, and context inputs, evaluation returns the same output.

## Deterministic Inputs

- Stable feature declaration (`Namespace`, `Feature`, `FlagDefinition`).
- Stable loaded snapshot.
- Stable context values (`stableId`, locale, platform, version, axes).
- Stable salt and feature key for bucketing.

## Mechanism Sketch

- Rule precedence is deterministic (specificity + stable ordering).
- Bucketing is deterministic over `(salt, featureKey, stableId)`.
- Fallback path is deterministic (`defaultValue` when unmatched/inactive/disabled).

## Proof Outline

Let `E(F, C, S)` be evaluation of feature `F` with context `C` and snapshot `S`.

If `F`, `C`, and `S` are unchanged between evaluations, then:

`E(F, C, S) = E(F, C, S)`.

Non-determinism enters only when at least one input changes.

## Test Evidence

| Test | Evidence |
| --- | --- |
| `MissingStableIdBucketingTest` | Stable IDs and fallback behavior produce repeatable bucket outcomes. |
| `ConditionEvaluationTest` | Rule matching and precedence remain deterministic for equivalent inputs. |

## Next Steps

- [Quickstart: Add Deterministic Ramp-Up](/quickstart/add-deterministic-ramp-up)
- [Concept: Evaluation Model](/concepts/evaluation-model)
