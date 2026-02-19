---
target_id: entrypoint:konditional-otel/src/main/kotlin/io/amichne/konditional/otel/metrics/OtelMetricsCollector.kt
scope_sig_paths:
  - konditional-otel/src/main/kotlin/io/amichne/konditional/otel/metrics/OtelMetricsCollector.kt.sig
symbol_ids:
  - method:86a6a7fc2fa434c3
  - method:c8e1b7cb9b10831b
  - method:dd14fc2dcfe9487b
claims:
  - claim_9ed4f7bfe812_an01
  - claim_9ed4f7bfe812_an02
  - claim_9ed4f7bfe812_an03
---

# OtelMetricsCollector entrypoint

## Inputs

This entrypoint exposes a `instrumentation-enabled runtime surface`. The signature-declared method family
is `recordConfigRollback`, `recordConfigLoad`, `recordEvaluation`, with parameter/shape contracts defined by:

- `override fun recordConfigRollback(event: Metrics.ConfigRollbackMetric)`
- `override fun recordConfigLoad(event: Metrics.ConfigLoadMetric)`
- `override fun recordEvaluation(event: Metrics.Evaluation)`

## Outputs

Return projections declared in this surface include `(not-explicit-in-signature-snippet)`. When
multiple return projections are present, they define complementary
entrypoints within the same target-scoped API seam.

## Determinism

Determinism at this layer comes from explicit, typed callable signatures:
inputs and output types are declared up front, and there are no ambient
runtime parameters encoded in the symbol surface.

## Operational notes

Linked contract types visible from signatures: `Metrics`, `MetricsCollector`, `AttributeKey`, `Attributes`, `LongCounter`.
Category mix for this target: `observe, write`.
This surface primarily enables: inline logging/metrics/tracing integration without a separate adapter API.
