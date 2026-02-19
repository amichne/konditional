---
target_id: entrypoint:konditional-core/src/main/kotlin/io/amichne/konditional/core/dsl/rules/RuleScope.kt
scope_sig_paths:
  - konditional-core/src/main/kotlin/io/amichne/konditional/core/dsl/rules/RuleScope.kt.sig
symbol_ids:
  - method:99321d9f814dc228
claims:
  - claim_b02dca45f5f9_an01
  - claim_b02dca45f5f9_an02
  - claim_b02dca45f5f9_an03
---

# RuleScope entrypoint

## Inputs

This entrypoint exposes a `construction/composition surface`. The signature-declared method family
is `yields`, with parameter/shape contracts defined by:

- `infix fun yields(value: T): Postfix`

## Outputs

Return projections declared in this surface include `Postfix`. When
multiple return projections are present, they define complementary
entrypoints within the same target-scoped API seam.

## Determinism

Determinism at this layer comes from explicit, typed callable signatures:
inputs and output types are declared up front, and there are no ambient
runtime parameters encoded in the symbol surface.

## Operational notes

Linked contract types visible from signatures: `KonditionalInternalApi`, `FlagScope`, `KonditionalDsl`, `LocaleTargetingScope`, `PlatformTargetingScope`.
Category mix for this target: `construct`.
This surface primarily enables: composable schema/value construction flows through constrained DSL entrypoints.
