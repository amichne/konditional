---
target_id: entrypoint:konditional-core/src/main/kotlin/io/amichne/konditional/core/registry/AxisCatalog.kt
scope_sig_paths:
  - konditional-core/src/main/kotlin/io/amichne/konditional/core/registry/AxisCatalog.kt.sig
symbol_ids:
  - method:0ae3dfaee8896520
  - method:26f0f908e246b13c
  - method:eeabfe3e6208359d
claims:
  - claim_a48843ad640e_an01
  - claim_a48843ad640e_an02
  - claim_a48843ad640e_an03
---

# AxisCatalog entrypoint

## Inputs

This entrypoint exposes a `mutation-and-lookup surface`. The signature-declared method family
is `register`, `axisForOrThrow`, `axisFor`, with parameter/shape contracts defined by:

- `fun register(axis: Axis<*>)`
- `fun <T> axisForOrThrow(type: KClass<out T>): Axis<T> where T : AxisValue<T>, T : Enum<T>`
- `fun <T> axisFor(type: KClass<out T>): Axis<T>? where T : AxisValue<T>, T : Enum<T>`

## Outputs

Return projections declared in this surface include `Axis<T> where T : AxisValue<T>, T : Enum<T>`, `Axis<T>? where T : AxisValue<T>, T : Enum<T>`. When
multiple return projections are present, they define complementary
entrypoints within the same target-scoped API seam.

## Determinism

Determinism at this layer comes from explicit, typed callable signatures:
inputs and output types are declared up front, and there are no ambient
runtime parameters encoded in the symbol surface.

## Operational notes

Linked contract types visible from signatures: `Axis`, `AxisValue`, `ConcurrentHashMap`, `KClass`, `Enum`.
Category mix for this target: `read, write`.
This surface primarily enables: closed-loop workflows where callers update state and immediately query the same contract surface.
