# Verified design synthesis

This page links the theory guarantees into one practical verification map for
implementation and review.

## Read this page when

- You need a single checklist before shipping behavior changes.
- You are reviewing cross-module changes touching core/runtime/serialization.
- You are planning tests that prove key invariants.

## Synthesis in scope

- **Type boundary**: compile-time typing plus parse-time materialization.
- **Deterministic semantics**: stable ordering, stable bucketing, pure matching.
- **Atomic state model**: whole-snapshot publication and read consistency.
- **Isolation model**: namespace-scoped state and lifecycle operations.
- **Migration safety**: shadow comparison without baseline behavior drift.

## Verification checklist

1. Prove parse-boundary rejection behavior with typed errors.
2. Prove deterministic outcomes for fixed `(context, snapshot)` fixtures.
3. Prove no partial reads under concurrent load/evaluate traffic.
4. Prove namespace failure isolation.
5. Prove shadow evaluation does not change baseline return values.

## Related pages

- [Type safety boundaries](/theory/type-safety-boundaries)
- [Parse don’t validate](/theory/parse-dont-validate)
- [Determinism proofs](/theory/determinism-proofs)
- [Atomicity guarantees](/theory/atomicity-guarantees)
- [Namespace isolation](/theory/namespace-isolation)
- [Migration and shadowing](/theory/migration-and-shadowing)

## Next steps

1. Use this checklist in pull request review templates.
2. Keep module docs concise and linked back to theory pages.
3. Expand test coverage where any checklist item is missing.
