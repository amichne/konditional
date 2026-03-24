# Directory Scope

This file scopes `konditional-engine/src/main/kotlin/io/amichne/konditional/internal/builders/versions/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Builder for version-range bounds before they become immutable domain values.

## Local Files
- `VersionRangeBuilder.kt`

## Rules
- Preserve the inclusive bound semantics implemented by `rules/versions/`.
- Default and unbounded behavior must stay explicit; avoid ambiguous partially initialized state.
