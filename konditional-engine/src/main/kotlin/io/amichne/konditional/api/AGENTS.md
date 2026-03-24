# Directory Scope

This file scopes `konditional-engine/src/main/kotlin/io/amichne/konditional/api/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Public evaluation surface for engine consumers.

## Local Files
- `BucketInfo.kt`
- `FeatureEvaluation.kt`
- `FeatureEvaluationMetrics.kt`

## Rules
- Keep `evaluate` and `explain` aligned: diagnostics may add detail but must not change decision semantics.
- Metrics and logging remain observational; they must not affect returned values or rule ordering.
- If rule identity or explanation structure changes, review sibling diagnostics and tests.
