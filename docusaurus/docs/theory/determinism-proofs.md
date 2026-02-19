# Determinism Proofs

Konditional guarantees deterministic evaluation for fixed `(context, snapshot)`
inputs. Determinism is compositional, not incidental.

## Determinism theorem

Given the same feature definition, the same evaluation context, and the same
active snapshot, evaluation produces the same value.

## Mechanism stack

### 1. Deterministic bucketing

Ramp-up bucket assignment is derived from a deterministic hash of
`(salt, featureKey, stableId)`.

### 2. Stable rule precedence

Rules are evaluated by structural specificity in stable order. Tie behavior is
explicit and deterministic for equal specificity sets.

### 3. Immutable snapshot read model

Evaluation reads trusted snapshot data. Runtime configuration replacement changes
future snapshot identity, which is an explicit boundary of the theorem.

## Determinism boundaries

- Same `stableId` and same snapshot: deterministic result.
- Different `stableId`: may yield different rollout membership by design.
- Snapshot change between calls: may yield different result by design.

## Operational implication

Determinism is guaranteed for decision replay, incident triage, and experiment
analysis only when replay inputs are truly equivalent.

## Related

- [Evaluation model](/learn/evaluation-model)
- [Atomicity guarantees](/theory/atomicity-guarantees)
- [Ramp-up bucketing API](/reference/api/ramp-up-bucketing)

## Claim ledger

| claim_id | claim_statement | claim_kind | status |
| --- | --- | --- | --- |
| TH-003-C1 | Ramp-up bucketing is deterministic for the same salt, feature key, and stable identifier. | guarantee | supported |
| TH-003-C2 | Stable identifier changes intentionally alter bucketing outcomes while preserving per-id determinism. | boundary | supported |
| TH-003-C3 | Rule precedence is derived from structural specificity before value selection. | mechanism | supported |
| TH-003-C4 | Evaluation for the same context and snapshot yields a stable value. | guarantee | supported |
| TH-003-C5 | Concurrent evaluations remain deterministic within the same active runtime snapshot. | guarantee | supported |
| TH-003-C6 | Determinism intentionally changes across configuration swaps because the snapshot identity changed. | boundary | supported |
