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
  - claim_72a20fcca082_an01
  - claim_72a20fcca082_an02
  - claim_72a20fcca082_an03
---

# OtelLogger entrypoint

## Inputs

This entrypoint exposes a `specialized entrypoint surface`. The signature-declared method family
is `info`, `error`, `warn`, `debug`, with parameter/shape contracts defined by:

- `override fun info(message: () -> String)`
- `override fun error( message: () -> String, throwable: Throwable?, )`
- `override fun warn( message: () -> String, throwable: Throwable?, )`
- `override fun debug(message: () -> String)`

## Outputs

Return projections declared in this surface include `(not-explicit-in-signature-snippet)`. When
multiple return projections are present, they define complementary
entrypoints within the same target-scoped API seam.

## Determinism

Determinism at this layer comes from explicit, typed callable signatures:
inputs and output types are declared up front, and there are no ambient
runtime parameters encoded in the symbol surface.

## Operational notes

Linked contract types visible from signatures: `KonditionalLogger`, `AttributeKey`, `Logger`, `Severity`.
Category mix for this target: `observe`.
This surface primarily enables: inline logging/metrics/tracing integration without a separate adapter API.
