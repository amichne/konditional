---
target_id: entrypoint:konditional-core/src/main/kotlin/io/amichne/konditional/core/dsl/rules/RuleSetBuilder.kt
scope_sig_paths:
  - konditional-core/src/main/kotlin/io/amichne/konditional/core/dsl/rules/RuleSetBuilder.kt.sig
symbol_ids:
  - method:6cc54f9f02722eba
  - method:f04fc525a255538f
claims:
  - claim_8523ce24b952_01
  - claim_8523ce24b952_02
  - claim_8523ce24b952_03
---

# RuleSetBuilder entrypoint

## Inputs

This entrypoint target exposes 2 signature symbol(s). Input parameter shapes
are defined by the declarations below:

- `fun ruleScoped( value: T, build: ContextRuleScope<C>.() -> Unit = {}, )`
- `fun rule( value: T, build: RuleScope<C>.() -> Unit = {}, )`

## Outputs

Return shapes are defined directly in the signature declarations for the
symbols in this target scope.

## Determinism

The documented API surface is signature-scoped: callable inputs are explicit
in method declarations, with no ambient parameters encoded at this layer.

## Operational notes

Symbol IDs in this target scope: `method:6cc54f9f02722eba`, `method:f04fc525a255538f`.
