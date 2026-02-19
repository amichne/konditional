---
target_id: entrypoint:konditional-core/src/main/kotlin/io/amichne/konditional/core/dsl/rules/targeting/scopes/PlatformTargetingScope.kt
scope_sig_paths:
  - konditional-core/src/main/kotlin/io/amichne/konditional/core/dsl/rules/targeting/scopes/PlatformTargetingScope.kt.sig
symbol_ids:
  - method:b3d6150302047051
  - method:e2fd304718d3a296
claims:
  - claim_25fd3cbb5d24_an01
  - claim_25fd3cbb5d24_an02
  - claim_25fd3cbb5d24_an03
---

# PlatformTargetingScope entrypoint

## Inputs

This entrypoint exposes a `specialized entrypoint surface`. The signature-declared method family
is `android`, `ios`, with parameter/shape contracts defined by:

- `fun android()`
- `fun ios()`

## Outputs

Return projections declared in this surface include `(not-explicit-in-signature-snippet)`. When
multiple return projections are present, they define complementary
entrypoints within the same target-scoped API seam.

## Determinism

Determinism at this layer comes from explicit, typed callable signatures:
inputs and output types are declared up front, and there are no ambient
runtime parameters encoded in the symbol surface.

## Operational notes

Linked contract types visible from signatures: `Platform`, `PlatformTag`, `KonditionalDsl`.
Category mix for this target: `specialized`.
This surface primarily enables: a focused integration seam for one constrained contract operation.
