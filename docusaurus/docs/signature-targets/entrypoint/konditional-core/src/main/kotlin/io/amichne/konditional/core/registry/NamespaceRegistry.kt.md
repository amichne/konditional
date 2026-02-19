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
  - claim_89c702483ed1_01
  - claim_89c702483ed1_02
  - claim_89c702483ed1_03
---

# NamespaceRegistry entrypoint

## Inputs

This entrypoint target exposes 4 signature symbol(s). Input parameter shapes
are defined by the declarations below:

- `fun <T : Any, C : Context, M : Namespace> findFlag( key: Feature<T, C, M>, ): FlagDefinition<T, C, M>?`
- `fun allFlags(): Map<Feature<*, *, *>, FlagDefinition<*, *, *>>`
- `fun setHooks(hooks: RegistryHooks) val isAllDisabled: Boolean fun disableAll() fun enableAll() @Suppress("UNCHECKED_CAST") fun <T : Any, C : Context, M : Namespace> flag( key: Feature<T, C, M>, ): FlagDefinition<T, C, M>`
- `fun load(config: ConfigurationView) val history: List<ConfigurationView> fun rollback(steps: Int = 1): Boolean`

## Outputs

Return shapes are defined directly in the signature declarations for the
symbols in this target scope.

## Determinism

The documented API surface is signature-scoped: callable inputs are explicit
in method declarations, with no ambient parameters encoded at this layer.

## Operational notes

Symbol IDs in this target scope: `method:082be30d739a0b58`, `method:5f65ce52c1a9f483`, `method:b9b971ea403e0c05`, `method:d8bcc3b46adea678`.
