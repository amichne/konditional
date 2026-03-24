# Directory Scope

This file scopes `konditional-json/src/main/kotlin/io/amichne/konditional/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Top-level JSON package split between private wire adapters and public serialization helpers.

## Go Deeper
- `internal/`: Private wire-model and Moshi adapter implementation.
- `serialization/`: Public JSON entrypoints and schema-aware codecs.

## Rules
- Keep strict wire-model handling in internal packages and the narrow public surface in `serialization/`.
- Do not relax parse failures into nulls or silent coercions at this level.
