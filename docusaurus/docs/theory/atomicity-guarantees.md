# Atomicity guarantees

This page is the canonical concurrency model for snapshot updates and reads.

## Read this page when

- You are reasoning about concurrent loads and evaluations.
- You are reviewing runtime update internals.
- You need precise "no partial reads" guarantees.

## Claim in scope

Readers observe whole snapshots only: either the previous snapshot or the new
snapshot, never an intermediate state.

## Mechanism in scope

- Snapshot pointers are stored in atomic references.
- Runtime writes publish complete snapshots as single logical updates.
- Reads fetch one published snapshot before evaluation.

## Linearizability implications

- Update operations appear to occur at a single point in time.
- Concurrent readers agree on a coherent snapshot history.
- Rollback and normal load operations preserve whole-snapshot visibility.

## Boundaries of the claim

- Atomic snapshot publication does not validate payload correctness.
- Mutable shared state outside snapshot publication is outside this guarantee.

## Related pages

- [Runtime operations](/runtime/operations)
- [Runtime lifecycle](/runtime/lifecycle)
- [Determinism proofs](/theory/determinism-proofs)
- [Namespace isolation](/theory/namespace-isolation)

## Next steps

1. Add concurrency smoke tests for load plus evaluate contention.
2. Keep snapshots immutable after publication.
3. Pair atomicity with parse boundaries in [Parse don’t validate](/theory/parse-dont-validate).
