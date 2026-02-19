---
target_id: entrypoint:konditional-core/src/main/kotlin/io/amichne/konditional/rules/versions/Unbounded.kt
scope_sig_paths:
  - konditional-core/src/main/kotlin/io/amichne/konditional/rules/versions/Unbounded.kt.sig
symbol_ids:
  - method:012706ab2a20e4bc
  - method:015cb0ee07b9683f
claims:
  - claim_82c9992bca07_an01
  - claim_82c9992bca07_an02
  - claim_82c9992bca07_an03
---

# Unbounded entrypoint

## Inputs

This entrypoint exposes a `lookup/projection surface`. The signature-declared method family
is `hasBounds`, `contains`, with parameter/shape contracts defined by:

- `override fun hasBounds(): Boolean`
- `override fun contains(v: Version): Boolean`

## Outputs

Return projections declared in this surface include `Boolean`. When
multiple return projections are present, they define complementary
entrypoints within the same target-scoped API seam.

## Determinism

Determinism at this layer comes from explicit, typed callable signatures:
inputs and output types are declared up front, and there are no ambient
runtime parameters encoded in the symbol surface.

## Operational notes

Linked contract types visible from signatures: `ToJson`, `Version`.
Category mix for this target: `read`.
This surface primarily enables: stable projection and query-style usage patterns over explicit typed inputs.
