# Directory Scope

This file scopes `konditional-types/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Foundational shared types module: contexts, identifiers, parse-result ADTs, and `Konstrained` contracts.

## Local Files
- `build.gradle.kts`

## Go Deeper
- `src/`: Production types and regression tests.

## Rules
- This module is downstream of nothing inside the main Konditional stack; avoid dependency creep.
- Wire-format and identifier behavior here propagate directly into engine and JSON semantics.
