---
target_id: entrypoint:konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/instance/ConfigurationPatch.kt
scope_sig_paths:
  - konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/instance/ConfigurationPatch.kt.sig
symbol_ids:
  - method:2a16c349e1576162
  - method:589bc0cd7a3f5783
  - method:f9fe8366c4cfcfe7
claims:
  - claim_f47fb53cb98c_01
  - claim_f47fb53cb98c_02
  - claim_f47fb53cb98c_03
---

# ConfigurationPatch entrypoint

## Inputs

Input contracts are defined by the signature declarations in this target:

- `fun <T : Any, C : Context> add( entry: FlagDefinition<T, C, *>, )`
- `fun applyTo(configuration: Configuration): Configuration`
- `fun remove(key: Feature<*, *, *>)`

## Outputs

Output contracts are the return types encoded directly in these method
declarations.

## Determinism

This documentation is constrained to signature-level API contracts, where
callable behavior is represented by explicit typed declarations.

## Operational notes

Symbol IDs in this target scope: `method:2a16c349e1576162`, `method:589bc0cd7a3f5783`, `method:f9fe8366c4cfcfe7`.
