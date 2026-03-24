# Directory Scope

This file scopes `konditional-engine/src/main/kotlin/io/amichne/konditional/core/dsl/rules/targeting/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Routing layer for targeting-specific DSL scopes.

## Go Deeper
- `scopes/`: Locale, platform, version, axis, stable-id, and custom targeting DSL fragments.

## Rules
- Keep targeting-specific authoring APIs in the deeper `scopes/` package.
- Do not duplicate targeting semantics here; this level exists to narrow where to read next.
