# Directory Scope

This file scopes `konditional-engine/src/main/kotlin/io/amichne/konditional/rules/targeting/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Pure targeting tree used by rule matching and explanation.

## Local Files
- `Targeting.kt`
- `TargetingProjections.kt`

## Rules
- Matching and specificity calculations are semantic core logic; keep them deterministic and side-effect free.
- Projection helpers should remain derivations of the targeting tree, not alternate sources of truth.
