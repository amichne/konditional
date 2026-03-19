# Directory Scope

This file scopes `konditional-json/src/main/kotlin/io/amichne/konditional/internal/serialization/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Private wire-layer boundary for JSON serialization.

## Go Deeper
- `adapters/`: Custom Moshi adapters and factories.
- `models/`: Serializable flag, rule, snapshot, and value models.

## Rules
- Keep the wire layer strict, typed, and deterministic.
- Do not let helper conveniences here leak into a broader public surface.
