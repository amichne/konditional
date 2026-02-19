---
target_id: entrypoint:konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/FeatureRegistry.kt
scope_sig_paths:
  - konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/FeatureRegistry.kt.sig
symbol_ids:
  - method:703b4e48f5e7c132
  - method:c56b05cfddc953f5
claims:
  - claim_f8a0d6871728_an01
  - claim_f8a0d6871728_an02
  - claim_f8a0d6871728_an03
---

# FeatureRegistry entrypoint

## Inputs

This entrypoint exposes a `mutation/build surface`. The signature-declared method family
is `register`, `clear`, with parameter/shape contracts defined by:

- `fun <T : Any, C : Context> register(feature: Feature<T, C, *>)`
- `fun clear()`

## Outputs

Return projections declared in this surface include `(not-explicit-in-signature-snippet)`. When
multiple return projections are present, they define complementary
entrypoints within the same target-scoped API seam.

## Determinism

Determinism at this layer comes from explicit, typed callable signatures:
inputs and output types are declared up front, and there are no ambient
runtime parameters encoded in the symbol surface.

## Operational notes

Linked contract types visible from signatures: `KonditionalInternalApi`, `Feature`, `ParseError`, `parseFailure`, `FeatureId`.
Category mix for this target: `write`.
This surface primarily enables: a focused integration seam for one constrained contract operation.
