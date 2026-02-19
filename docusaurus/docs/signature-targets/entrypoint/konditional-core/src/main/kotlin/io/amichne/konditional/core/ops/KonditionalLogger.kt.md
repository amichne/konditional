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
  - claim_05f61eaae9db_an01
  - claim_05f61eaae9db_an02
  - claim_05f61eaae9db_an03
---

# KonditionalLogger entrypoint

## Inputs

This entrypoint exposes a `specialized entrypoint surface`. The signature-declared method family
is `debug`, `info`, `error`, `warn`, with parameter/shape contracts defined by:

- `fun debug(message: () -> String) {}`
- `fun info(message: () -> String) {}`
- `fun error(message: () -> String, throwable: Throwable? = null) {}`
- `fun warn(message: () -> String, throwable: Throwable? = null) {}`

## Outputs

Return projections declared in this surface include `(not-explicit-in-signature-snippet)`. When
multiple return projections are present, they define complementary
entrypoints within the same target-scoped API seam.

## Determinism

Determinism at this layer comes from explicit, typed callable signatures:
inputs and output types are declared up front, and there are no ambient
runtime parameters encoded in the symbol surface.

## Operational notes

Linked contract types visible from signatures: `(none)`.
Category mix for this target: `observe`.
This surface primarily enables: inline logging/metrics/tracing integration without a separate adapter API.
