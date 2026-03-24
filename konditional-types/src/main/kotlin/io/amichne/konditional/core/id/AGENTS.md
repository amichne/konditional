# Directory Scope

This file scopes `konditional-types/src/main/kotlin/io/amichne/konditional/core/id/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Stable identifier primitives used for bucketing and deterministic rollout behavior.

## Local Files
- `HexId.kt`
- `StableId.kt`
- `StaticStableId.kt`

## Rules
- `StableId` normalization and `HexId` encoding are compatibility-sensitive because bucketing hashes the canonical hex form.
- Do not change lowercase normalization or canonical encoding without coordinating downstream rollout semantics.
