---
target_id: entrypoint:konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/snapshot/ConfigurationSnapshotCodec.kt
scope_sig_paths:
  - konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/snapshot/ConfigurationSnapshotCodec.kt.sig
symbol_ids:
  - method:269df93e671974ba
  - method:2f68837a39cf0b4f
  - method:4c639de98a993be7
  - method:6e99ec9b87b6f1c3
  - method:6ee7d2ccc23b20cf
  - method:aa3b66835acdf0c8
  - method:e93dea772debb271
claims:
  - claim_6731ecc6aade_an01
  - claim_6731ecc6aade_an02
  - claim_6731ecc6aade_an03
---

# ConfigurationSnapshotCodec entrypoint

## Inputs

This entrypoint exposes a `boundary transformation and runtime control surface`. The signature-declared method family
is `applyPatchJson`, `encode`, `decode`, `encodeRaw`, with parameter/shape contracts defined by:

- `fun applyPatchJson( currentConfiguration: MaterializedConfiguration, patchJson: String, options: SnapshotLoadOptions = SnapshotLoadOptions.strict(), ): Result<MaterializedConfiguration>`
- `fun encode(value: ConfigurationView): String`
- `fun applyPatchJson( currentConfiguration: ConfigurationView, schema: CompiledNamespaceSchema, patchJson: String, options: SnapshotLoadOptions = SnapshotLoadOptions.strict(), ): Result<MaterializedConfiguration>`
- `override fun decode( json: String, schema: CompiledNamespaceSchema, options: SnapshotLoadOptions, ): Result<MaterializedConfiguration>`
- `override fun decode( json: String, options: SnapshotLoadOptions, ): Result<MaterializedConfiguration>`
- `fun encodeRaw(value: Configuration): String`
- `override fun encode(value: MaterializedConfiguration): String`

## Outputs

Return projections declared in this surface include `Result<MaterializedConfiguration>`, `String`. When
multiple return projections are present, they define complementary
entrypoints within the same target-scoped API seam.

## Determinism

Determinism at this layer comes from explicit, typed callable signatures:
inputs and output types are declared up front, and there are no ambient
runtime parameters encoded in the symbol surface.

## Operational notes

Linked contract types visible from signatures: `Moshi`, `PolymorphicJsonAdapterFactory`, `KotlinJsonAdapterFactory`, `KonditionalInternalApi`, `ConfigurationMetadataView`.
Category mix for this target: `boundary, write`.
This surface primarily enables: bidirectional boundary transformations between typed models and serialized representations.
