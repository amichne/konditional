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
  - claim_6731ecc6aade_01
  - claim_6731ecc6aade_02
  - claim_6731ecc6aade_03
---

# ConfigurationSnapshotCodec entrypoint

## Inputs

Input contracts are defined by the signature declarations in this target:

- `fun applyPatchJson( currentConfiguration: MaterializedConfiguration, patchJson: String, options: SnapshotLoadOptions = SnapshotLoadOptions.strict(), ): Result<MaterializedConfiguration>`
- `fun encode(value: ConfigurationView): String`
- `fun applyPatchJson( currentConfiguration: ConfigurationView, schema: CompiledNamespaceSchema, patchJson: String, options: SnapshotLoadOptions = SnapshotLoadOptions.strict(), ): Result<MaterializedConfiguration>`
- `override fun decode( json: String, schema: CompiledNamespaceSchema, options: SnapshotLoadOptions, ): Result<MaterializedConfiguration>`
- `override fun decode( json: String, options: SnapshotLoadOptions, ): Result<MaterializedConfiguration>`
- `fun encodeRaw(value: Configuration): String`
- `override fun encode(value: MaterializedConfiguration): String`

## Outputs

Output contracts are the return types encoded directly in these method
declarations.

## Determinism

This documentation is constrained to signature-level API contracts, where
callable behavior is represented by explicit typed declarations.

## Operational notes

Symbol IDs in this target scope: `method:269df93e671974ba`, `method:2f68837a39cf0b4f`, `method:4c639de98a993be7`, `method:6e99ec9b87b6f1c3`, `method:6ee7d2ccc23b20cf`, `method:aa3b66835acdf0c8`, `method:e93dea772debb271`.
