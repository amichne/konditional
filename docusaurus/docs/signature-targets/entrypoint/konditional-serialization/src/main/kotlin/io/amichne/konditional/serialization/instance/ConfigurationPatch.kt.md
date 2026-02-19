---
target_id: entrypoint:konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/instance/ConfigurationPatch.kt
scope_sig_paths:
  - konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/instance/ConfigurationPatch.kt.sig
symbol_ids:
  - method:2a16c349e1576162
  - method:589bc0cd7a3f5783
  - method:f9fe8366c4cfcfe7
claims:
  - claim_f47fb53cb98c_an01
  - claim_f47fb53cb98c_an02
  - claim_f47fb53cb98c_an03
---

# ConfigurationPatch entrypoint

## Inputs

This entrypoint exposes a `mutation/build surface`. The signature-declared method family
is `add`, `applyTo`, `remove`, with parameter/shape contracts defined by:

- `fun <T : Any, C : Context> add( entry: FlagDefinition<T, C, *>, )`
- `fun applyTo(configuration: Configuration): Configuration`
- `fun remove(key: Feature<*, *, *>)`

## Outputs

Return projections declared in this surface include `Configuration`. When
multiple return projections are present, they define complementary
entrypoints within the same target-scoped API seam.

## Determinism

Determinism at this layer comes from explicit, typed callable signatures:
inputs and output types are declared up front, and there are no ambient
runtime parameters encoded in the symbol surface.

## Operational notes

Linked contract types visible from signatures: `KonditionalInternalApi`, `FlagDefinition`, `Feature`, `Configuration`.
Category mix for this target: `write`.
This surface primarily enables: a focused integration seam for one constrained contract operation.
