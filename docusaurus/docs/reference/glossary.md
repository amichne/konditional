# Glossary

This page is a compact reference dictionary for core Konditional API and
contract terms.

## Read this page when

- You need exact terminology while reading API reference pages.
- You are aligning team language for rollout, parsing, and runtime operations.
- You want canonical links for deeper conceptual context.

## API and contract terms

| Term | API/contract definition | Canonical conceptual page |
| --- | --- | --- |
| Atomic swap | Runtime snapshot replacement where readers see either old or new configuration, never partial state. | [Atomicity guarantees](/theory/atomicity-guarantees) |
| Context | Typed evaluation input required by `Feature.evaluate(...)`. | [Type safety boundaries](/theory/type-safety-boundaries) |
| Determinism | Same context plus same snapshot yields the same evaluation output. | [Determinism proofs](/theory/determinism-proofs) |
| Feature | Typed declared value owned by a namespace. | [Namespace isolation](/theory/namespace-isolation) |
| Kill-switch | Namespace-scoped override where evaluations return declared defaults. | [Atomicity guarantees](/theory/atomicity-guarantees) |
| Namespace | Isolation boundary with independent registry lifecycle and hooks. | [Namespace isolation](/theory/namespace-isolation) |
| Parse boundary | Untrusted input edge where JSON is converted into typed trusted models. | [Parse don't validate](/theory/parse-dont-validate) |
| Ramp-up | Percentage rollout gate using deterministic bucketing. | [Determinism proofs](/theory/determinism-proofs) |
| Result boundary | Kotlin `Result<T>` success/failure channel for decode/load APIs. | [Parse don't validate](/theory/parse-dont-validate) |
| Rollback | Namespace runtime operation that restores a previous snapshot from history. | [Atomicity guarantees](/theory/atomicity-guarantees) |
| Shadow evaluation | Baseline-returning evaluation with candidate comparison and mismatch reporting. | [Migration and shadowing](/theory/migration-and-shadowing) |
| StableId | Deterministic identity input for bucketing. | [Determinism proofs](/theory/determinism-proofs) |

## Deterministic API and contract notes

- Terms in this glossary map to concrete Kotlin APIs or structured boundary
  contracts.
- The glossary itself is non-normative; normative behavior lives in API
  references and source contracts.
- Concept pages linked above explain rationale and proof-level details.

## Canonical conceptual pages

- [Theory: Determinism proofs](/theory/determinism-proofs)
- [Theory: Atomicity guarantees](/theory/atomicity-guarantees)
- [Theory: Parse don't validate](/theory/parse-dont-validate)
- [Theory: Namespace isolation](/theory/namespace-isolation)
- [Theory: Migration and shadowing](/theory/migration-and-shadowing)

## Next steps

- [Feature evaluation API](/reference/api/feature-evaluation)
- [Namespace operations API](/reference/api/namespace-operations)
- [Migration guide](/reference/migration-guide)
