# Directory Scope

This file scopes `konditional-engine/src/main/kotlin/io/amichne/konditional/core/dsl/rules/` and yields to deeper `AGENTS.md` files when present.

## Purpose
The receiver that deferred rule values run against during evaluation.

## Local Files
- `RuleValueScope.kt`

## Rules
- A resolver runs only after its rule matched its condition and rollout.
- Same-namespace feature reads must use the evaluation's registry snapshot.
