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
  - claim_a3ed394d79a1_01
  - claim_a3ed394d79a1_02
  - claim_a3ed394d79a1_03
---

# InMemoryNamespaceRegistry entrypoint

## Inputs

Input contracts are defined by the signature declarations in this target:

- `override fun enableAll()`
- `override fun <T : Any, C : Context, M : Namespace> setOverride( feature: Feature<T, C, M>, value: T, )`
- `override fun rollback(steps: Int): Boolean`
- `override fun disableAll()`
- `override fun <T : Any, C : Context, M : Namespace> flag(key: Feature<T, C, M>): FlagDefinition<T, C, M>`
- `override fun load(config: ConfigurationView)`
- `override fun <T : Any, C : Context, M : Namespace> clearOverride( feature: Feature<T, C, M>, )`
- `override fun <T : Any, C : Context, M : Namespace> findFlag(key: Feature<T, C, M>): FlagDefinition<T, C, M>?`
- `override fun setHooks(hooks: RegistryHooks)`
- `override fun updateDefinition(definition: FlagDefinition<*, *, *>)`

## Outputs

Output contracts are the return types encoded directly in these method
declarations.

## Determinism

This documentation is constrained to signature-level API contracts, where
callable behavior is represented by explicit typed declarations.

## Operational notes

Symbol IDs in this target scope: `method:2e6ab8de4d2246e0`, `method:37ad875188f91c3a`, `method:4d92cf4cf7b52ea4`, `method:544ca5673d66d9a3`, `method:5a8571b3e73fe7a4`, `method:6d9a28a3a353dc37`, `method:bc1dcf337e598cb2`, `method:ccad174fc52206f1`, `method:e55a0dd1537ba451`, `method:f214ea6372d01ea6`.
