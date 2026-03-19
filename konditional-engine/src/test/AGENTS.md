# Directory Scope

This file scopes `konditional-engine/src/test/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Engine test source set.

## Go Deeper
- `kotlin/`: Kotlin-based engine regression tests.

## Rules
- Tests here should prove determinism, namespace isolation, and atomic registry behavior.
- Prefer narrow package-local tests over broad integration fixtures when a single invariant is under change.
