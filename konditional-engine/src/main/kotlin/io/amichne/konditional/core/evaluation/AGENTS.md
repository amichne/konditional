# Directory Scope

This file scopes `konditional-engine/src/main/kotlin/io/amichne/konditional/core/evaluation/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Deterministic bucketing primitives used by rollout evaluation.

## Local Files
- `Bucketing.kt`

## Rules
- Bucketing must stay stable for the same namespace, feature, salt, and stable-id input.
- Changes here can invalidate rollout behavior globally; verify determinism and rollout threshold semantics.
