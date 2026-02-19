---
target_id: entrypoint:konditional-core/src/main/kotlin/io/amichne/konditional/core/ops/KonditionalLogger.kt
scope_sig_paths:
  - konditional-core/src/main/kotlin/io/amichne/konditional/core/ops/KonditionalLogger.kt.sig
symbol_ids:
  - method:039689712a52e36e
  - method:2a5e7b71c7fa8f18
  - method:72004e61fed0118b
  - method:f2a43676ed3441b3
claims:
  - claim_05f61eaae9db_01
  - claim_05f61eaae9db_02
  - claim_05f61eaae9db_03
---

# KonditionalLogger entrypoint

## Inputs

This entrypoint target exposes 4 signature symbol(s). Input parameter shapes
are defined by the declarations below:

- `fun debug(message: () -> String) {}`
- `fun info(message: () -> String) {}`
- `fun error(message: () -> String, throwable: Throwable? = null) {}`
- `fun warn(message: () -> String, throwable: Throwable? = null) {}`

## Outputs

Return shapes are defined directly in the signature declarations for the
symbols in this target scope.

## Determinism

The documented API surface is signature-scoped: callable inputs are explicit
in method declarations, with no ambient parameters encoded at this layer.

## Operational notes

Symbol IDs in this target scope: `method:039689712a52e36e`, `method:2a5e7b71c7fa8f18`, `method:72004e61fed0118b`, `method:f2a43676ed3441b3`.
