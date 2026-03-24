# Directory Scope

This file scopes `konditional-engine/src/main/kotlin/io/amichne/konditional/context/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Engine-specific contextual leaf types.

## Local Files
- `RampUp.kt`

## Rules
- `RampUp` is a trusted percentage type; preserve strict 0..100 parsing and normalization semantics.
- Keep this directory small and engine-specific; shared context primitives belong in `:konditional-types`.
