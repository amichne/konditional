# Directory Scope

This file scopes `konditional-json/src/main/kotlin/io/amichne/konditional/serialization/internal/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Low-level JSON value and primitive conversion helpers.

## Local Files
- `JsonValueConversions.kt`
- `KonstrainedPrimitiveMap.kt`

## Rules
- Preserve primitive fidelity, especially `Int` versus `Double`, and keep map keys string-safe.
- These helpers support schema-aware codecs; do not bypass higher-level validation from here.
