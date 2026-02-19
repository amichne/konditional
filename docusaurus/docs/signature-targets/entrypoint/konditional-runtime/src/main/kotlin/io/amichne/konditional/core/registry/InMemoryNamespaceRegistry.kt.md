---
target_id: entrypoint:konditional-runtime/src/main/kotlin/io/amichne/konditional/core/registry/InMemoryNamespaceRegistry.kt
scope_sig_paths:
  - konditional-runtime/src/main/kotlin/io/amichne/konditional/core/registry/InMemoryNamespaceRegistry.kt.sig
symbol_ids:
  - method:2e6ab8de4d2246e0
  - method:37ad875188f91c3a
  - method:4d92cf4cf7b52ea4
  - method:544ca5673d66d9a3
  - method:5a8571b3e73fe7a4
  - method:6d9a28a3a353dc37
  - method:bc1dcf337e598cb2
  - method:ccad174fc52206f1
  - method:e55a0dd1537ba451
  - method:f214ea6372d01ea6
claims:
  - claim_a3ed394d79a1_an01
  - claim_a3ed394d79a1_an02
  - claim_a3ed394d79a1_an03
---

# InMemoryNamespaceRegistry entrypoint

## Inputs

This entrypoint exposes a `mutation-and-lookup surface`. The signature-declared method family
is `enableAll`, `setOverride`, `rollback`, `disableAll`, `flag`, `load`, `clearOverride`, `findFlag`, `setHooks`, `updateDefinition`, with parameter/shape contracts defined by:

- `override fun enableAll()`
- `override fun <T : Any, C : Context, M : Namespace> setOverride( feature: Feature<T, C, M>, value: T, )`
- `override fun rollback(steps: Int): Boolean`
- `override fun disableAll()`
- `override fun <T : Any, C : Context, M : Namespace> flag(key: Feature<T, C, M>): FlagDefinition<T, C, M>`
- `override fun load(config: ConfigurationView)`
- `override fun <T : Any, C : Context, M : Namespace> clearOverride( feature: Feature<T, C, M>, )`
- `override fun <T : Any, C : Context, M : Namespace> findFlag(key: Feature<T, C, M>): FlagDefinition<T, C, M>?`
- `...`

## Outputs

Return projections declared in this surface include `Boolean`, `FlagDefinition<T, C, M>`, `FlagDefinition<T, C, M>?`. When
multiple return projections are present, they define complementary
entrypoints within the same target-scoped API seam.

## Determinism

Determinism at this layer comes from explicit, typed callable signatures:
inputs and output types are declared up front, and there are no ambient
runtime parameters encoded in the symbol surface.

## Operational notes

Linked contract types visible from signatures: `KonditionalInternalApi`, `FlagDefinition`, `Feature`, `ConfigurationMetadataView`, `ConfigurationView`.
Category mix for this target: `read, write`.
This surface primarily enables: closed-loop workflows where callers update state and immediately query the same contract surface.
