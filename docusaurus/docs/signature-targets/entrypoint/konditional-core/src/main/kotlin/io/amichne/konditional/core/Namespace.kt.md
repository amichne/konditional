---
target_id: entrypoint:konditional-core/src/main/kotlin/io/amichne/konditional/core/Namespace.kt
scope_sig_paths:
  - konditional-core/src/main/kotlin/io/amichne/konditional/core/Namespace.kt.sig
symbol_ids:
  - method:05f131326cca03e5
  - method:1967daa263b64dd3
  - method:287862c056cf0839
  - method:8edfb424b9657119
  - method:957aa0d699d986dc
  - method:a2bfa70772be8410
  - method:c153d09f70b5c315
claims:
  - claim_240371567a78_an01
  - claim_240371567a78_an02
  - claim_240371567a78_an03
---

# Namespace entrypoint

## Inputs

This entrypoint exposes a `construction/composition surface`. The signature-declared method family
is `hashCode`, `compiledSchema`, `declaredDefault`, `declaredDefinition`, `allFeatures`, `toString`, `equals`, with parameter/shape contracts defined by:

- `override fun hashCode(): Int`
- `fun compiledSchema(): CompiledNamespaceSchema`
- `fun declaredDefault(feature: Feature<*, *, *>): Any?`
- `fun declaredDefinition(feature: Feature<*, *, *>): FlagDefinition<*, *, *>?`
- `fun allFeatures(): List<Feature<*, *, *>>`
- `override fun toString(): String`
- `override fun equals(other: Any?): Boolean`

## Outputs

Return projections declared in this surface include `Int`, `CompiledNamespaceSchema`, `Any?`, `FlagDefinition<*, *, *>?`, `List<Feature<*, *, *>>`, `String`, `Boolean`. When
multiple return projections are present, they define complementary
entrypoints within the same target-scoped API seam.

## Determinism

Determinism at this layer comes from explicit, typed callable signatures:
inputs and output types are declared up front, and there are no ambient
runtime parameters encoded in the symbol surface.

## Operational notes

Linked contract types visible from signatures: `KonditionalInternalApi`, `Axis`, `AxisValue`, `FlagScope`, `BooleanFeature`.
Category mix for this target: `construct, read`.
This surface primarily enables: composable schema/value construction flows through constrained DSL entrypoints.
