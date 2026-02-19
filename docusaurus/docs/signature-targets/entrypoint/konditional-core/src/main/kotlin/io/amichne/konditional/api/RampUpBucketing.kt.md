---
target_id: entrypoint:konditional-core/src/main/kotlin/io/amichne/konditional/api/RampUpBucketing.kt
scope_sig_paths:
  - konditional-core/src/main/kotlin/io/amichne/konditional/api/RampUpBucketing.kt.sig
symbol_ids:
  - method:b8aca06589216d2f
claims:
  - claim_rampupbucketing_bucket_signature
  - claim_rampupbucketing_bucket_typed_stable_id
  - claim_rampupbucketing_bucket_int_output
---

# RampUpBucketing entrypoint

## Inputs

`bucket` accepts three inputs in declaration order: `stableId`, `featureKey`,
and `salt`.

## Outputs

`bucket` returns `Int`.

## Determinism

The signature exposes only explicit parameters and does not include ambient
inputs in its API shape.

## Operational notes

The first parameter is `StableId`, while `featureKey` and `salt` are `String`
values.
