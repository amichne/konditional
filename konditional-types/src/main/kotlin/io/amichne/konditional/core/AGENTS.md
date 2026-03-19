# Directory Scope

This file scopes `konditional-types/src/main/kotlin/io/amichne/konditional/core/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Foundational core contracts grouped by identity, parse results, and `Konstrained` typing.

## Go Deeper
- `features/`: Shared identifiable interfaces.
- `id/`: Stable and hex identifier primitives.
- `result/`: Typed parse-failure ADTs and wrappers.
- `types/`: `Konstrained` marker hierarchy.

## Rules
- Keep shared semantics here dependency-light and type-safe; engine and JSON both rely on them.
