---
target_id: entrypoint:konditional-core/src/main/kotlin/io/amichne/konditional/rules/targeting/Targeting.kt
scope_sig_paths:
  - konditional-core/src/main/kotlin/io/amichne/konditional/rules/targeting/Targeting.kt.sig
symbol_ids:
  - method:1dc2cc50a99656da
  - method:26a8e1c65d32833b
  - method:97f055a9ffe2a1a1
  - method:a90410f4d59b73a0
  - method:b4d514854cf725ff
  - method:cf81421641711f3b
  - method:d8a8ca9060cf46ba
  - method:fed95f1c8b289cf1
claims:
  - claim_c167e42394be_an01
  - claim_c167e42394be_an02
  - claim_c167e42394be_an03
---

# Targeting entrypoint

## Inputs

This entrypoint exposes a `construction/composition surface`. The signature-declared method family
is `matches`, `specificity`, `plus`, with parameter/shape contracts defined by:

- `override fun matches(context: Context): Boolean`
- `override fun matches(context: Context.PlatformContext): Boolean`
- `override fun specificity(): Int`
- `override fun matches(context: Context.LocaleContext): Boolean`
- `override fun matches(context: Context.VersionContext): Boolean`
- `operator fun plus(other: All<C>): All<C>`
- `fun matches(context: C): Boolean fun specificity(): Int @JvmInline value class Locale(val ids: Set<String>) : Targeting<Context.LocaleContext>`
- `override fun matches(context: C): Boolean`

## Outputs

Return projections declared in this surface include `Boolean`, `Int`, `All<C>`, `Boolean fun specificity(): Int @JvmInline value class Locale(val ids: Set<String>) : Targeting<Context.LocaleContext>`. When
multiple return projections are present, they define complementary
entrypoints within the same target-scoped API seam.

## Determinism

Determinism at this layer comes from explicit, typed callable signatures:
inputs and output types are declared up front, and there are no ambient
runtime parameters encoded in the symbol surface.

## Operational notes

Linked contract types visible from signatures: `getAxisValue`, `Unbounded`, `VersionRange`, `PlatformContext`, `LocaleContext`.
Category mix for this target: `construct, read`.
This surface primarily enables: composable schema/value construction flows through constrained DSL entrypoints.
