---
target_id: entrypoint:konditional-core/src/main/kotlin/io/amichne/konditional/core/dsl/rules/RuleSet.kt
scope_sig_paths:
  - konditional-core/src/main/kotlin/io/amichne/konditional/core/dsl/rules/RuleSet.kt.sig
symbol_ids:
  - method:d8c9b649bb0e2a06
claims:
  - claim_984a3e2270fa_an01
  - claim_984a3e2270fa_an02
  - claim_984a3e2270fa_an03
---

# RuleSet entrypoint

## Inputs

This entrypoint exposes a `construction/composition surface`. The signature-declared method family
is `plus`, with parameter/shape contracts defined by:

- `operator fun plus(other: RuleSet<RC, T, C, M>): RuleSet<RC, T, C, M>`

## Outputs

Return projections declared in this surface include `RuleSet<RC, T, C, M>`. When
multiple return projections are present, they define complementary
entrypoints within the same target-scoped API seam.

## Determinism

Determinism at this layer comes from explicit, typed callable signatures:
inputs and output types are declared up front, and there are no ambient
runtime parameters encoded in the symbol surface.

## Operational notes

Linked contract types visible from signatures: `KonditionalDsl`, `Feature`, `RuleSet`, `RC`.
Category mix for this target: `construct`.
This surface primarily enables: composable schema/value construction flows through constrained DSL entrypoints.
