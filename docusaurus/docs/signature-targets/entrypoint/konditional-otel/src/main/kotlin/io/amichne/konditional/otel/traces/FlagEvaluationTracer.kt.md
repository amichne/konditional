---
target_id: entrypoint:konditional-otel/src/main/kotlin/io/amichne/konditional/otel/traces/FlagEvaluationTracer.kt
scope_sig_paths:
  - konditional-otel/src/main/kotlin/io/amichne/konditional/otel/traces/FlagEvaluationTracer.kt.sig
symbol_ids:
  - method:1b5b330f67bd6dd3
claims:
  - claim_11df056d19bd_01
  - claim_11df056d19bd_02
  - claim_11df056d19bd_03
---

# FlagEvaluationTracer entrypoint

## Inputs

Input contracts are defined by the signature declarations in this target:

- `fun <T : Any, C : Context> traceEvaluation( feature: Feature<T, C, *>, context: C, parentSpan: Span? = null, block: () -> EvaluationDiagnostics<T>, ): EvaluationDiagnostics<T>`

## Outputs

Output contracts are the return types encoded directly in these method
declarations.

## Determinism

This documentation is constrained to signature-level API contracts, where
callable behavior is represented by explicit typed declarations.

## Operational notes

Symbol IDs in this target scope: `method:1b5b330f67bd6dd3`.
