---
target_id: entrypoint:konditional-core/src/main/kotlin/io/amichne/konditional/rules/Rule.kt
scope_sig_paths:
  - konditional-core/src/main/kotlin/io/amichne/konditional/rules/Rule.kt.sig
symbol_ids:
  - method:dad44d3dc0354920
  - method:e90cca48f60402c6
claims:
  - claim_feaf226717b6_an01
  - claim_feaf226717b6_an02
  - claim_feaf226717b6_an03
---

# Rule entrypoint

## Inputs

This entrypoint exposes a `lookup/projection surface`. The signature-declared method family
is `matches`, `specificity`, with parameter/shape contracts defined by:

- `fun matches(context: C): Boolean`
- `fun specificity(): Int`

## Outputs

Return projections declared in this surface include `Boolean`, `Int`. When
multiple return projections are present, they define complementary
entrypoints within the same target-scoped API seam.

## Determinism

Determinism at this layer comes from explicit, typed callable signatures:
inputs and output types are declared up front, and there are no ambient
runtime parameters encoded in the symbol surface.

## Operational notes

Linked contract types visible from signatures: `RampUp`, `HexId`, `Targeting`.
Category mix for this target: `read`.
This surface primarily enables: stable projection and query-style usage patterns over explicit typed inputs.
