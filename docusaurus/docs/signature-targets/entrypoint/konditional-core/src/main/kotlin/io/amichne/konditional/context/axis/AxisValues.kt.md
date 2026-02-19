---
target_id: entrypoint:konditional-core/src/main/kotlin/io/amichne/konditional/context/axis/AxisValues.kt
scope_sig_paths:
  - konditional-core/src/main/kotlin/io/amichne/konditional/context/axis/AxisValues.kt.sig
symbol_ids:
  - method:2b4eed5581832292
  - method:bacafb9e83436b05
  - method:ccac6fef985f54d5
  - method:eba5c6b27ab8966e
claims:
  - claim_32d0abe2d95d_an01
  - claim_32d0abe2d95d_an02
  - claim_32d0abe2d95d_an03
---

# AxisValues entrypoint

## Inputs

This entrypoint exposes a `construction/composition surface`. The signature-declared method family
is `toString`, `equals`, `get`, `hashCode`, with parameter/shape contracts defined by:

- `override fun toString(): String`
- `override fun equals(other: Any?): Boolean`
- `operator fun <T> get(axis: Axis<T>): Set<T> where T : AxisValue<T>, T : Enum<T>`
- `override fun hashCode(): Int`

## Outputs

Return projections declared in this surface include `String`, `Boolean`, `Set<T> where T : AxisValue<T>, T : Enum<T>`, `Int`. When
multiple return projections are present, they define complementary
entrypoints within the same target-scoped API seam.

## Determinism

Determinism at this layer comes from explicit, typed callable signatures:
inputs and output types are declared up front, and there are no ambient
runtime parameters encoded in the symbol surface.

## Operational notes

Linked contract types visible from signatures: `KClass`, `Axis`, `AxisValue`, `Enum`.
Category mix for this target: `construct, read`.
This surface primarily enables: composable schema/value construction flows through constrained DSL entrypoints.
