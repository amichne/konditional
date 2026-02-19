---
target_id: entrypoint:konditional-core/src/main/kotlin/io/amichne/konditional/rules/versions/VersionRange.kt
scope_sig_paths:
  - konditional-core/src/main/kotlin/io/amichne/konditional/rules/versions/VersionRange.kt.sig
symbol_ids:
  - method:de958e79acdb2869
  - method:ff2dfb4f7251c7ca
claims:
  - claim_8cb14f9c9e57_an01
  - claim_8cb14f9c9e57_an02
  - claim_8cb14f9c9e57_an03
---

# VersionRange entrypoint

## Inputs

This entrypoint exposes a `lookup/projection surface`. The signature-declared method family
is `hasBounds`, `contains`, with parameter/shape contracts defined by:

- `open fun hasBounds(): Boolean`
- `open fun contains(v: Version): Boolean`

## Outputs

Return projections declared in this surface include `Boolean`. When
multiple return projections are present, they define complementary
entrypoints within the same target-scoped API seam.

## Determinism

Determinism at this layer comes from explicit, typed callable signatures:
inputs and output types are declared up front, and there are no ambient
runtime parameters encoded in the symbol surface.

## Operational notes

Linked contract types visible from signatures: `Version`, `pow`.
Category mix for this target: `read`.
This surface primarily enables: stable projection and query-style usage patterns over explicit typed inputs.
