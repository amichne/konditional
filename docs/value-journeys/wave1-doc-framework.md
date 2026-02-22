# Wave 1 evidence-backed documentation framework

This document records the Wave 1 artifact map and baseline drift register used
to execute the theory + learn rewrite.

## Artifact ID mapping

| artifact_id | doc_scope | doc_page | owner_modules |
| --- | --- | --- | --- |
| TH-001 | theory | `theory/parse-dont-validate` | konditional-runtime, konditional-serialization, konditional-core |
| TH-002 | theory | `theory/type-safety-boundaries` | konditional-core, konditional-runtime, konditional-serialization |
| TH-003 | theory | `theory/determinism-proofs` | konditional-core, konditional-runtime |
| TH-004 | theory | `theory/atomicity-guarantees` | konditional-runtime, konditional-core |
| TH-005 | theory | `theory/namespace-isolation` | konditional-core, konditional-runtime, konditional-serialization |
| LRN-001 | learn | `learn/core-primitives` | konditional-core, konditional-runtime, konditional-serialization |
| LRN-002 | learn | `learn/evaluation-model` | konditional-core |
| LRN-003 | learn | `learn/type-safety` | konditional-core, konditional-runtime, konditional-serialization |
| LRN-004 | learn | `learn/configuration-lifecycle` | konditional-runtime, konditional-serialization, konditional-core |
| JV-001 | value_journey | `value-journeys/jv-001-confident-rollouts` | konditional-runtime, konditional-core |
| JV-002 | value_journey | `value-journeys/jv-002-safe-snapshot-ingestion` | konditional-runtime, konditional-serialization, konditional-core |

## Baseline drift register

| drift_id | severity | description | resolution status |
| --- | --- | --- | --- |
| DRIFT-001 | high | `configuration-lifecycle` used `fromJson(...)` wording even though supported runtime entrypoint is `NamespaceSnapshotLoader(...).load(...)`. | fixed in Wave 1 rewrite |
| DRIFT-002 | high | Namespace/theory examples implied schema-less decode success where codec requires schema-scoped decode for trusted materialization. | fixed in Wave 1 rewrite |
| DRIFT-003 | medium | Boundary examples referenced `Configuration` in decode success paths where boundary contract is `MaterializedConfiguration`. | fixed in Wave 1 rewrite |

## Enforcement contract

1. Every non-trivial claim must exist in `journey-claims.json`.
2. Every rewritten theory/learn page must include a `Claim ledger` section.
3. Validator status mismatch (`declared` vs `computed`) is a reportable issue.
4. Strict CI mode fails on unresolved non-trivial claims.
