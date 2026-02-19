---
target_id: entrypoint:konditional-core/src/main/kotlin/io/amichne/konditional/core/dsl/rules/RuleScopeBase.kt
scope_sig_paths:
  - konditional-core/src/main/kotlin/io/amichne/konditional/core/dsl/rules/RuleScopeBase.kt.sig
symbol_ids:
  - method:5aa3ccc6acc5755e
  - method:a4b0897bdf04be8f
claims:
  - claim_d82accc9ee6a_an01
  - claim_d82accc9ee6a_an02
  - claim_d82accc9ee6a_an03
---

# RuleScopeBase entrypoint

## Inputs

This entrypoint exposes a `lookup/projection surface`. The signature-declared method family
is `always`, `matchAll`, with parameter/shape contracts defined by:

- `fun always() {}`
- `fun matchAll()`

## Outputs

Return projections declared in this surface include `(not-explicit-in-signature-snippet)`. When
multiple return projections are present, they define complementary
entrypoints within the same target-scoped API seam.

## Determinism

Determinism at this layer comes from explicit, typed callable signatures:
inputs and output types are declared up front, and there are no ambient
runtime parameters encoded in the symbol surface.

## Operational notes

Linked contract types visible from signatures: `KonditionalDsl`.
Category mix for this target: `read`.
This surface primarily enables: stable projection and query-style usage patterns over explicit typed inputs.
