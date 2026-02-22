# Determinism proofs

This page is the canonical argument for deterministic evaluation outcomes in
Konditional.

## Read this page when

- You need to defend deterministic behavior in design or review.
- You are testing rollout bucketing and rule ordering.
- You are evaluating changes to criteria or hashing logic.

## Claim in scope

For a fixed `(feature definition, context, snapshot)`, evaluation returns the
same value on every run.

## Proof components

- **Stable rule ordering**: specificity sorting with deterministic tie-breaking.
- **Pure criteria checks**: matching depends on context and rule data only.
- **Deterministic ramp-up bucketing**: hash of stable inputs,
  `bucket = hash % 10_000`.
- **Snapshot consistency**: evaluation reads one concrete snapshot state.

## Boundaries of the claim

- Different snapshots can produce different outputs.
- Different stable IDs can produce different rollout outcomes by design.
- Non-deterministic custom predicates break determinism and are out of
  contract.

## Related pages

- [Evaluation model](/learn/evaluation-model)
- [Atomicity guarantees](/theory/atomicity-guarantees)
- [Core rule DSL](/core/rules)
- [Migration and shadowing](/theory/migration-and-shadowing)

## Next steps

1. Add determinism regression tests for fixed fixtures.
2. Add distribution tests for ramp-up cohorts.
3. Review parse-boundary assumptions in [Parse don’t validate](/theory/parse-dont-validate).
