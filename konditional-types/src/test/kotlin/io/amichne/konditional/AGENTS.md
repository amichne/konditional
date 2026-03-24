# Directory Scope

This file scopes `konditional-types/src/test/kotlin/io/amichne/konditional/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Top-level foundational-type tests grouped by context, parse-result behavior, and leaf-type semantics.

## Go Deeper
- `context/`: Context composition, stable-id, version, and axis tests.
- `core/`: Parse-result and typed-failure tests.
- `types/`: Leaf-type and context-factory regression tests.

## Rules
- Prefer narrow executable specifications tied to one invariant over broad smoke coverage.
