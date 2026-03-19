# Directory Scope

This file scopes `konditional-engine/src/test/kotlin/io/amichne/konditional/engine/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Runtime-focused engine tests for registry loading, rollback, rollout determinism, and concurrent readers.

## Local Files
- `NamespaceRuntimeTest.kt`

## Rules
- Use this package when a change spans declared definitions plus live registry state.
