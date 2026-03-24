# Directory Scope

This file scopes `konditional-engine/src/main/kotlin/io/amichne/konditional/core/registry/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Namespace registry contracts and the in-memory atomic snapshot implementation.

## Local Files
- `InMemoryNamespaceRegistry.kt`
- `NamespaceRegistry.kt`
- `NamespaceSnapshot.kt`

## Rules
- Readers must continue to observe either the old snapshot or the new snapshot, never partial state.
- Keep `current`, `history`, and disable or enable state transitions coherent under concurrent access.
- Rollback behavior is part of the public runtime contract; preserve whole-snapshot semantics.
