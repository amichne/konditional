# Directory Scope

This file scopes `konditional-engine/src/main/kotlin/io/amichne/konditional/internal/evaluation/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Diagnostics model returned by `explain` and internal evaluation paths.

## Local Files
- `EvaluationDiagnostics.kt`

## Rules
- Diagnostics may describe evaluation in more detail, but they must stay a pure projection of actual decisions.
- If you add structure here, update `api/FeatureEvaluation.kt` and any metrics mappings that project from it.
