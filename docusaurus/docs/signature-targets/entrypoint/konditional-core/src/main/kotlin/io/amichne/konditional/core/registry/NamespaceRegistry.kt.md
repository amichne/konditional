---
target_id: entrypoint:konditional-core/src/main/kotlin/io/amichne/konditional/core/registry/NamespaceRegistry.kt
scope_sig_paths:
  - konditional-core/src/main/kotlin/io/amichne/konditional/core/registry/NamespaceRegistry.kt.sig
symbol_ids:
  - method:082be30d739a0b58
  - method:5f65ce52c1a9f483
  - method:b9b971ea403e0c05
  - method:d8bcc3b46adea678
claims:
  - claim_89c702483ed1_an01
  - claim_89c702483ed1_an02
  - claim_89c702483ed1_an03
---

# NamespaceRegistry entrypoint

## Inputs

This entrypoint exposes a `mutation-and-lookup surface`. The signature-declared method family
is `findFlag`, `allFlags`, `setHooks`, `disableAll`, `enableAll`, `flag`, `load`, `rollback`, with parameter/shape contracts defined by:

- `fun <T : Any, C : Context, M : Namespace> findFlag( key: Feature<T, C, M>, ): FlagDefinition<T, C, M>?`
- `fun allFlags(): Map<Feature<*, *, *>, FlagDefinition<*, *, *>>`
- `fun setHooks(hooks: RegistryHooks) val isAllDisabled: Boolean fun disableAll() fun enableAll() @Suppress("UNCHECKED_CAST") fun <T : Any, C : Context, M : Namespace> flag( key: Feature<T, C, M>, ): FlagDefinition<T, C, M>`
- `fun load(config: ConfigurationView) val history: List<ConfigurationView> fun rollback(steps: Int = 1): Boolean`

## Outputs

Return projections declared in this surface include `FlagDefinition<T, C, M>?`, `Map<Feature<*, *, *>, FlagDefinition<*, *, *>>`, `FlagDefinition<T, C, M>`, `Boolean`. When
multiple return projections are present, they define complementary
entrypoints within the same target-scoped API seam.

## Determinism

Determinism at this layer comes from explicit, typed callable signatures:
inputs and output types are declared up front, and there are no ambient
runtime parameters encoded in the symbol surface.

## Operational notes

Linked contract types visible from signatures: `KonditionalInternalApi`, `FlagDefinition`, `Feature`, `ConfigurationView`, `RegistryHooks`.
Category mix for this target: `read, write`.
This surface primarily enables: closed-loop workflows where callers update state and immediately query the same contract surface.
