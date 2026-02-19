---
target_id: entrypoint:kontracts/src/main/kotlin/io/amichne/kontracts/dsl/BooleanSchemaBuilder.kt
scope_sig_paths:
  - kontracts/src/main/kotlin/io/amichne/kontracts/dsl/BooleanSchemaBuilder.kt.sig
symbol_ids:
  - method:84be3614d8f91225
claims:
  - claim_10441dae4525_an01
  - claim_10441dae4525_an02
  - claim_10441dae4525_an03
---

# BooleanSchemaBuilder entrypoint

## Inputs

This entrypoint exposes a `construction/composition surface`. The signature-declared method family
is `build`, with parameter/shape contracts defined by:

- `override fun build()`

## Outputs

Return projections declared in this surface include `(not-explicit-in-signature-snippet)`. When
multiple return projections are present, they define complementary
entrypoints within the same target-scoped API seam.

## Determinism

Determinism at this layer comes from explicit, typed callable signatures:
inputs and output types are declared up front, and there are no ambient
runtime parameters encoded in the symbol surface.

## Operational notes

Linked contract types visible from signatures: `BooleanSchema`.
Category mix for this target: `construct`.
This surface primarily enables: composable schema/value construction flows through constrained DSL entrypoints.
