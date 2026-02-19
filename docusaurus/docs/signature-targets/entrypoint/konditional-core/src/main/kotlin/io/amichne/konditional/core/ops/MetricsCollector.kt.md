---
target_id: entrypoint:konditional-core/src/main/kotlin/io/amichne/konditional/core/ops/MetricsCollector.kt
scope_sig_paths:
  - konditional-core/src/main/kotlin/io/amichne/konditional/core/ops/MetricsCollector.kt.sig
symbol_ids:
  - method:0d391987815328f5
  - method:99553bd3f24c6b8b
  - method:fca4dd3ffdaebbba
claims:
  - claim_3e7225273e70_01
  - claim_3e7225273e70_02
  - claim_3e7225273e70_03
---

# MetricsCollector entrypoint

## Inputs

This entrypoint target exposes 3 signature symbol(s). Input parameter shapes
are defined by the declarations below:

- `fun recordConfigLoad(event: Metrics.ConfigLoadMetric) {}`
- `fun recordEvaluation(event: Metrics.Evaluation) {}`
- `fun recordConfigRollback(event: Metrics.ConfigRollbackMetric) {}`

## Outputs

Return shapes are defined directly in the signature declarations for the
symbols in this target scope.

## Determinism

The documented API surface is signature-scoped: callable inputs are explicit
in method declarations, with no ambient parameters encoded at this layer.

## Operational notes

Symbol IDs in this target scope: `method:0d391987815328f5`, `method:99553bd3f24c6b8b`, `method:fca4dd3ffdaebbba`.
