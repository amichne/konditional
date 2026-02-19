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
  - claim_32d0abe2d95d_01
  - claim_32d0abe2d95d_02
  - claim_32d0abe2d95d_03
---

# AxisValues entrypoint

## Inputs

This entrypoint target exposes 4 signature symbol(s). Input parameter shapes
are defined by the declarations below:

- `override fun toString(): String`
- `override fun equals(other: Any?): Boolean`
- `operator fun <T> get(axis: Axis<T>): Set<T> where T : AxisValue<T>, T : Enum<T>`
- `override fun hashCode(): Int`

## Outputs

Return shapes are defined directly in the signature declarations for the
symbols in this target scope.

## Determinism

The documented API surface is signature-scoped: callable inputs are explicit
in method declarations, with no ambient parameters encoded at this layer.

## Operational notes

Symbol IDs in this target scope: `method:2b4eed5581832292`, `method:bacafb9e83436b05`, `method:ccac6fef985f54d5`, `method:eba5c6b27ab8966e`.
