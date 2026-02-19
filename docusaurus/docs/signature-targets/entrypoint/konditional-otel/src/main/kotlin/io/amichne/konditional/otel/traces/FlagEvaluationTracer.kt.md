---
target_id: entrypoint:konditional-otel/src/main/kotlin/io/amichne/konditional/otel/traces/FlagEvaluationTracer.kt
scope_sig_paths:
  - konditional-otel/src/main/kotlin/io/amichne/konditional/otel/traces/FlagEvaluationTracer.kt.sig
symbol_ids:
  - method:1b5b330f67bd6dd3
claims:
  - claim_11df056d19bd_an01
  - claim_11df056d19bd_an02
  - claim_11df056d19bd_an03
---

# FlagEvaluationTracer entrypoint

## Inputs

This entrypoint exposes a `specialized entrypoint surface`. The signature-declared method family
is `traceEvaluation`, with parameter/shape contracts defined by:

- `fun <T : Any, C : Context> traceEvaluation( feature: Feature<T, C, *>, context: C, parentSpan: Span? = null, block: () -> EvaluationDiagnostics<T>, ): EvaluationDiagnostics<T>`

## Outputs

Return projections declared in this surface include `EvaluationDiagnostics<T>`. When
multiple return projections are present, they define complementary
entrypoints within the same target-scoped API seam.

## Determinism

Determinism at this layer comes from explicit, typed callable signatures:
inputs and output types are declared up front, and there are no ambient
runtime parameters encoded in the symbol surface.

## Operational notes

Linked contract types visible from signatures: `LocaleContext`, `PlatformContext`, `StableIdContext`, `VersionContext`, `AxisValues`.
Category mix for this target: `observe`.
This surface primarily enables: inline logging/metrics/tracing integration without a separate adapter API.
