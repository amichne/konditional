# Migration and shadowing

This page is the canonical model for changing evaluation behavior without
changing baseline production semantics.

## Read this page when

- You are migrating configurations or flag systems.
- You need mismatch reporting before promotion.
- You are planning a staged rollout with safety checks.

## Concepts in scope

- **Baseline evaluation**: the value returned to production callers.
- **Candidate evaluation**: comparison-only execution path.
- **Mismatch reporting**: structured mismatch output for analysis.
- **Promotion decision**: candidate becomes baseline only after confidence.

## Migration steps in scope

1. Keep baseline registry as the production source of truth.
2. Evaluate candidate registry in shadow mode.
3. Record mismatches with deterministic context identifiers.
4. Investigate and resolve mismatch causes.
5. Promote candidate snapshot only when mismatch rate is acceptable.

## Boundaries and costs

- Shadow evaluation preserves baseline return values by contract.
- Candidate evaluation and mismatch hooks add request-path cost.
- Hooks must stay lightweight to avoid latency regressions.

## Related pages

- [Runtime operations](/runtime/operations)
- [Determinism proofs](/theory/determinism-proofs)
- [Atomicity guarantees](/theory/atomicity-guarantees)
- [Configuration lifecycle](/learn/configuration-lifecycle)

## Next steps

1. Define sampling policy for shadow traffic.
2. Define mismatch severity thresholds before promotion.
3. Add rollback procedures via [Runtime operations](/runtime/operations).
