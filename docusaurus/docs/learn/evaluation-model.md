# Evaluation model

This page explains the runtime decision path from feature lookup to final value.
It focuses on operational behavior and points to theory pages for proofs.

## Read this page when

- You need to predict which rule will win.
- You are debugging surprising flag outcomes.
- You are writing tests for ordering and rollout behavior.

## Steps in scope

1. Resolve the feature definition from the active snapshot.
2. If namespace kill-switch is active, return the feature default.
3. If feature `isActive` is `false`, return the feature default.
4. Sort rules by specificity with a stable tie-breaker.
5. Evaluate criteria for each rule in order.
6. For the first matching rule, evaluate ramp-up and allowlist.
7. Return that value, or the default if no rule qualifies.

### 1. Total: always returns a value {#1-total-always-returns-a-value}

If no rule is selected, evaluation returns the declared feature default.

### 2. Deterministic: same inputs = same outputs {#2-deterministic-same-inputs--same-outputs}

For a fixed `(context, snapshot)`, evaluation follows one deterministic rule
path and returns one deterministic value.

## Determinism scope

- Determinism is defined for the same `(context, snapshot)` pair.
- Different snapshots can produce different values for the same context.
- Different stable IDs can produce different rollout buckets by design.

## Related pages

- [Core API reference](/core/reference)
- [Rule DSL reference](/core/rules)
- [Determinism proofs](/theory/determinism-proofs)
- [Atomicity guarantees](/theory/atomicity-guarantees)

## Next steps

1. Add deterministic tests using [Determinism proofs](/theory/determinism-proofs).
2. Review rollout guidance in [Rollout strategies (legacy path)](/rules-and-targeting/rollout-strategies).
3. Connect runtime updates in [Runtime lifecycle](/runtime/lifecycle).
