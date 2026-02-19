---
target_id: entrypoint:konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/instance/Configuration.kt
scope_sig_paths:
  - konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/instance/Configuration.kt.sig
symbol_ids:
  - method:36e59f38debad3be
  - method:41a01ff65a618b92
  - method:61e3f939db3cb049
claims:
  - claim_809d597b1c32_an01
  - claim_809d597b1c32_an02
  - claim_809d597b1c32_an03
---

# Configuration entrypoint

## Inputs

This entrypoint exposes a `mutation/build surface`. The signature-declared method family
is `diff`, `withMetadata`, with parameter/shape contracts defined by:

- `fun diff(other: Configuration): ConfigurationDiff`
- `fun withMetadata( version: String? = null, generatedAtEpochMillis: Long? = null, source: String? = null, ): Configuration`
- `fun withMetadata(metadata: ConfigurationMetadata): Configuration`

## Outputs

Return projections declared in this surface include `ConfigurationDiff`, `Configuration`. When
multiple return projections are present, they define complementary
entrypoints within the same target-scoped API seam.

## Determinism

Determinism at this layer comes from explicit, typed callable signatures:
inputs and output types are declared up front, and there are no ambient
runtime parameters encoded in the symbol surface.

## Operational notes

Linked contract types visible from signatures: `KonditionalInternalApi`, `FlagDefinition`, `Feature`, `ConfigurationMetadataView`, `ConfigurationView`.
Category mix for this target: `write`.
This surface primarily enables: a focused integration seam for one constrained contract operation.
