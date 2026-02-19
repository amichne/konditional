---
target_id: entrypoint:konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/instance/Configuration.kt
scope_sig_paths:
  - konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/instance/Configuration.kt.sig
symbol_ids:
  - method:36e59f38debad3be
  - method:41a01ff65a618b92
  - method:61e3f939db3cb049
claims:
  - claim_809d597b1c32_01
  - claim_809d597b1c32_02
  - claim_809d597b1c32_03
---

# Configuration entrypoint

## Inputs

Input contracts are defined by the signature declarations in this target:

- `fun diff(other: Configuration): ConfigurationDiff`
- `fun withMetadata( version: String? = null, generatedAtEpochMillis: Long? = null, source: String? = null, ): Configuration`
- `fun withMetadata(metadata: ConfigurationMetadata): Configuration`

## Outputs

Output contracts are the return types encoded directly in these method
declarations.

## Determinism

This documentation is constrained to signature-level API contracts, where
callable behavior is represented by explicit typed declarations.

## Operational notes

Symbol IDs in this target scope: `method:36e59f38debad3be`, `method:41a01ff65a618b92`, `method:61e3f939db3cb049`.
