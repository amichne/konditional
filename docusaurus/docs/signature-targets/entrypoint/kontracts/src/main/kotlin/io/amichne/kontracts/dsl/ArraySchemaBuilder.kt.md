---
target_id: entrypoint:kontracts/src/main/kotlin/io/amichne/kontracts/dsl/ArraySchemaBuilder.kt
scope_sig_paths:
  - kontracts/src/main/kotlin/io/amichne/kontracts/dsl/ArraySchemaBuilder.kt.sig
symbol_ids:
  - method:255db508105a1da0
claims:
  - claim_6f0867829052_an01
  - claim_6f0867829052_an02
  - claim_6f0867829052_an03
---

# ArraySchemaBuilder entrypoint

## Inputs

This entrypoint exposes a `mutation/build surface`. The signature-declared method family
is `element`, with parameter/shape contracts defined by:

- `fun element(builder: RootObjectSchemaBuilder.() -> Unit)`

## Outputs

Return projections declared in this surface include `(not-explicit-in-signature-snippet)`. When
multiple return projections are present, they define complementary
entrypoints within the same target-scoped API seam.

## Determinism

Determinism at this layer comes from explicit, typed callable signatures:
inputs and output types are declared up front, and there are no ambient
runtime parameters encoded in the symbol surface.

## Operational notes

Linked contract types visible from signatures: `ArraySchema`, `JsonSchema`, `RootObjectSchemaBuilder`.
Category mix for this target: `write`.
This surface primarily enables: a focused integration seam for one constrained contract operation.
