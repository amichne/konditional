# Evaluation Model

Konditional evaluation is total, deterministic, and specificity-driven for a
fixed context and snapshot.

## Total evaluation

A feature always returns a value. If no rule matches, evaluation returns the
declared default.

## Deterministic evaluation

For fixed `(context, snapshot)`, evaluation results are stable and reproducible.

## Evaluation order

1. Compute rule specificity.
2. Evaluate rules from highest specificity to lowest.
3. Apply ramp-up constraints for candidate matches.
4. Return first matching rule value.
5. Return default if no candidate matches.

## Targeting composition

Targeting is structural. Criteria within a rule are conjunctive, and axis
semantics are explicit within that structure.

## Deterministic ramp-up

Ramp-up assignment is derived from deterministic bucketing over stable identity
inputs, feature key, and salt.

## Related

- [Determinism proofs](/theory/determinism-proofs)
- [Rules and targeting](/rules-and-targeting/rule-composition)
- [Rollout strategies](/rules-and-targeting/rollout-strategies)

## Claim ledger

| claim_id | claim_statement | claim_kind | status |
| --- | --- | --- | --- |
| LRN-002-C1 | Evaluation is total because defaults provide a value when no targeting rule matches. | guarantee | supported |
| LRN-002-C2 | Rule selection honors specificity ordering before first-match resolution. | mechanism | supported |
| LRN-002-C3 | Targeting composition is structural and conjunctive with explicit axis semantics. | mechanism | supported |
| LRN-002-C4 | Ramp-up assignment is deterministic and reproducible for a stable identity tuple. | guarantee | supported |
