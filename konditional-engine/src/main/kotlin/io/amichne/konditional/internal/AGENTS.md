# Directory Scope

This file scopes `konditional-engine/src/main/kotlin/io/amichne/konditional/internal/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Engine implementation details shared across DSL, serialization, and diagnostics.

## Local Files
- `FlagDefinitionInternal.kt`

## Go Deeper
- `builders/`: DSL-to-domain builders and variant assembly.
- `evaluation/`: Evaluation diagnostics data model.

## Rules
- This package is internal plumbing; do not widen it into an accidental public API surface.
- When behavior changes here, verify the corresponding public surface in `core/`, `rules/`, or `api/` still matches.
