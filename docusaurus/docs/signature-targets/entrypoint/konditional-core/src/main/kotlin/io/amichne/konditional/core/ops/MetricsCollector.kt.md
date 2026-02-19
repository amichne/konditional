---
target_id: entrypoint:konditional-core/src/main/kotlin/io/amichne/konditional/core/ops/MetricsCollector.kt
scope_sig_paths:
  - konditional-core/src/main/kotlin/io/amichne/konditional/core/ops/MetricsCollector.kt.sig
symbol_ids:
  - method:0d391987815328f5
  - method:99553bd3f24c6b8b
  - method:fca4dd3ffdaebbba
claims:
  - claim_3e7225273e70_an01
  - claim_3e7225273e70_an02
  - claim_3e7225273e70_an03
---

# MetricsCollector entrypoint

## Inputs

This entrypoint exposes a `instrumentation-enabled runtime surface`. The signature-declared method family
is `recordConfigLoad`, `recordEvaluation`, `recordConfigRollback`, with parameter/shape contracts defined by:

- `fun recordConfigLoad(event: Metrics.ConfigLoadMetric) {}`
- `fun recordEvaluation(event: Metrics.Evaluation) {}`
- `fun recordConfigRollback(event: Metrics.ConfigRollbackMetric) {}`

## Outputs

Return projections declared in this surface include `(not-explicit-in-signature-snippet)`. When
multiple return projections are present, they define complementary
entrypoints within the same target-scoped API seam.

## Determinism

Determinism at this layer comes from explicit, typed callable signatures:
inputs and output types are declared up front, and there are no ambient
runtime parameters encoded in the symbol surface.

## Operational notes

Linked contract types visible from signatures: `Metrics`, `ConfigLoadMetric`, `Evaluation`, `ConfigRollbackMetric`.
Category mix for this target: `observe, write`.
This surface primarily enables: inline logging/metrics/tracing integration without a separate adapter API.
