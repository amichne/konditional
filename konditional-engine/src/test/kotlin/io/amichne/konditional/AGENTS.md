# Directory Scope

This file scopes `konditional-engine/src/test/kotlin/io/amichne/konditional/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Top-level engine test packages grouped by invariant.

## Go Deeper
- `core/`: Namespace behavior and deterministic evaluation tests.
- `engine/`: Runtime snapshot behavior tests.
- `rules/`: Rule and version semantics tests.
- `runtime/`: Explicit atomicity regression tests.

## Rules
- Keep each package focused on the behavior it proves; avoid duplicating the same concurrency or isolation assertions in multiple places.
