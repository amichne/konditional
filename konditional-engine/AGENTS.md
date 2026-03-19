# Directory Scope

This file scopes `konditional-engine/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Engine module: typed namespace definition, deterministic evaluation, atomic registry state, and explainability hooks.

## Local Files
- `build.gradle.kts`

## Go Deeper
- `src/`: Production code, tests, and shared fixtures.

## Rules
- Keep runtime behavior deterministic; evaluation paths must stay free of ambient time or randomness.
- Preserve namespace isolation and whole-snapshot atomicity when touching registry-facing code.
- This module depends on `:konditional-types` and `:kontracts`; do not back-reference JSON-specific parsing concerns here.
