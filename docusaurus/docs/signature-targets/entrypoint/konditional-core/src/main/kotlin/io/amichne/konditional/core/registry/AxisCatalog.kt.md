---
target_id: entrypoint:konditional-core/src/main/kotlin/io/amichne/konditional/core/registry/AxisCatalog.kt
scope_sig_paths:
  - konditional-core/src/main/kotlin/io/amichne/konditional/core/registry/AxisCatalog.kt.sig
symbol_ids:
  - method:0ae3dfaee8896520
  - method:26f0f908e246b13c
  - method:eeabfe3e6208359d
claims:
  - claim_a48843ad640e_01
  - claim_a48843ad640e_02
  - claim_a48843ad640e_03
---

# AxisCatalog entrypoint

## Inputs

This entrypoint target exposes 3 signature symbol(s). Input parameter shapes
are defined by the declarations below:

- `fun register(axis: Axis<*>)`
- `fun <T> axisForOrThrow(type: KClass<out T>): Axis<T> where T : AxisValue<T>, T : Enum<T>`
- `fun <T> axisFor(type: KClass<out T>): Axis<T>? where T : AxisValue<T>, T : Enum<T>`

## Outputs

Return shapes are defined directly in the signature declarations for the
symbols in this target scope.

## Determinism

The documented API surface is signature-scoped: callable inputs are explicit
in method declarations, with no ambient parameters encoded at this layer.

## Operational notes

Symbol IDs in this target scope: `method:0ae3dfaee8896520`, `method:26f0f908e246b13c`, `method:eeabfe3e6208359d`.
