---
target_id: entrypoint:kontracts/src/main/kotlin/io/amichne/kontracts/schema/FieldSchema.kt
scope_sig_paths:
  - kontracts/src/main/kotlin/io/amichne/kontracts/schema/FieldSchema.kt.sig
symbol_ids:
  - method:ec2e5e303c39cf70
claims:
  - claim_db62a49e98b5_an01
  - claim_db62a49e98b5_an02
  - claim_db62a49e98b5_an03
---

# FieldSchema entrypoint

## Inputs

This entrypoint exposes a `construction/composition surface`. The signature-declared method family
is `toString`, with parameter/shape contracts defined by:

- `override fun toString()`

## Outputs

Return projections declared in this surface include `(not-explicit-in-signature-snippet)`. When
multiple return projections are present, they define complementary
entrypoints within the same target-scoped API seam.

## Determinism

Determinism at this layer comes from explicit, typed callable signatures:
inputs and output types are declared up front, and there are no ambient
runtime parameters encoded in the symbol surface.

## Operational notes

Linked contract types visible from signatures: `(none)`.
Category mix for this target: `construct, read`.
This surface primarily enables: composable schema/value construction flows through constrained DSL entrypoints.
