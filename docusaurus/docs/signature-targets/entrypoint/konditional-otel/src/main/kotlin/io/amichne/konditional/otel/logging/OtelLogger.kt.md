---
target_id: entrypoint:konditional-otel/src/main/kotlin/io/amichne/konditional/otel/logging/OtelLogger.kt
scope_sig_paths:
  - konditional-otel/src/main/kotlin/io/amichne/konditional/otel/logging/OtelLogger.kt.sig
symbol_ids:
  - method:117fbcaf961bcc31
  - method:938d15e66f3b6794
  - method:c1fc1b2de8c6865f
  - method:fa901cede662ab3d
claims:
  - claim_72a20fcca082_01
  - claim_72a20fcca082_02
  - claim_72a20fcca082_03
---

# OtelLogger entrypoint

## Inputs

Input contracts are defined by the signature declarations in this target:

- `override fun info(message: () -> String)`
- `override fun error( message: () -> String, throwable: Throwable?, )`
- `override fun warn( message: () -> String, throwable: Throwable?, )`
- `override fun debug(message: () -> String)`

## Outputs

Output contracts are the return types encoded directly in these method
declarations.

## Determinism

This documentation is constrained to signature-level API contracts, where
callable behavior is represented by explicit typed declarations.

## Operational notes

Symbol IDs in this target scope: `method:117fbcaf961bcc31`, `method:938d15e66f3b6794`, `method:c1fc1b2de8c6865f`, `method:fa901cede662ab3d`.
