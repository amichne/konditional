---
target_id: entrypoint:konditional-runtime/src/main/kotlin/io/amichne/konditional/serialization/snapshot/NamespaceSnapshotLoader.kt
scope_sig_paths:
  - konditional-runtime/src/main/kotlin/io/amichne/konditional/serialization/snapshot/NamespaceSnapshotLoader.kt.sig
symbol_ids:
  - method:d81e0ac72584181e
claims:
  - claim_ab0253af6ab8_an01
  - claim_ab0253af6ab8_an02
  - claim_ab0253af6ab8_an03
---

# NamespaceSnapshotLoader entrypoint

## Inputs

This entrypoint exposes a `mutation/build surface`. The signature-declared method family
is `load`, with parameter/shape contracts defined by:

- `override fun load( json: String, options: SnapshotLoadOptions, ): Result<MaterializedConfiguration>`

## Outputs

Return projections declared in this surface include `Result<MaterializedConfiguration>`. When
multiple return projections are present, they define complementary
entrypoints within the same target-scoped API seam.

## Determinism

Determinism at this layer comes from explicit, typed callable signatures:
inputs and output types are declared up front, and there are no ambient
runtime parameters encoded in the symbol surface.

## Operational notes

Linked contract types visible from signatures: `KonditionalInternalApi`, `NamespaceRegistryRuntime`, `ParseError`, `parseErrorOrNull`, `parseFailure`.
Category mix for this target: `write`.
This surface primarily enables: a focused integration seam for one constrained contract operation.
