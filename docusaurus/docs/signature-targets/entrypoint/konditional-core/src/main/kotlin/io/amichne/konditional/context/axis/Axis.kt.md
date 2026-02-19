---
target_id: entrypoint:konditional-core/src/main/kotlin/io/amichne/konditional/context/axis/Axis.kt
scope_sig_paths:
  - konditional-core/src/main/kotlin/io/amichne/konditional/context/axis/Axis.kt.sig
symbol_ids:
  - method:4e2382c78be4a53d
  - method:e5d5bd612d822abf
  - method:eba599973597d3f4
claims:
  - claim_c0c3d56d1960_an01
  - claim_c0c3d56d1960_an02
  - claim_c0c3d56d1960_an03
---

# Axis entrypoint

## Inputs

This entrypoint exposes a `construction/composition surface`. The signature-declared method family
is `equals`, `toString`, `hashCode`, with parameter/shape contracts defined by:

- `override fun equals(other: Any?): Boolean`
- `override fun toString(): String`
- `override fun hashCode(): Int`

## Outputs

Return projections declared in this surface include `Boolean`, `String`, `Int`. When
multiple return projections are present, they define complementary
entrypoints within the same target-scoped API seam.

## Determinism

Determinism at this layer comes from explicit, typed callable signatures:
inputs and output types are declared up front, and there are no ambient
runtime parameters encoded in the symbol surface.

## Operational notes

Linked contract types visible from signatures: `AxisCatalog`, `KClass`.
Category mix for this target: `construct, read`.
This surface primarily enables: composable schema/value construction flows through constrained DSL entrypoints.
