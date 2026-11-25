# Directory Scope

This file scopes `konditional-types/src/main/kotlin/io/amichne/konditional/context/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Evaluation inputs and the optional stable identity for rollout.

## Local Files
- `Context.kt`
- `ContextKey.kt`

## Go Deeper
- `fact/` owns fact declarations, immutable assignments, portable expressions, and schema resolution.

## Rules
- Missing assignments cause positive comparisons to miss.
- Compare assignments by descriptor identity. Resolve wire identifiers against the namespace schema.
