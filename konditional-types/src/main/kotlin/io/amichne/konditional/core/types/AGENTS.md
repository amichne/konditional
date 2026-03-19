# Directory Scope

This file scopes `konditional-types/src/main/kotlin/io/amichne/konditional/core/types/` and yields to deeper `AGENTS.md` files when present.

## Purpose
`Konstrained` marker hierarchy and adapter contracts for custom typed values.

## Local Files
- `Konstrained.kt`

## Rules
- JSON reflection and schema extraction assume the shapes expressed here; coordinate any contract change with codec code.
- Prefer explicit typed encoders and decoders over ad hoc reflection shortcuts.
