# Directory Scope

This file scopes `konditional-types/src/main/kotlin/io/amichne/konditional/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Top-level shared types package used by engine and JSON.

## Go Deeper
- `api/`: Internal opt-in annotation surface.
- `context/`: Context mixins and contextual value objects.
- `core/`: Identity, parse-result, and `Konstrained` foundations.
- `values/`: Public identifier value types.

## Rules
- Favor compile-time guarantees over convenience; this package fans out into the rest of the repo.
- Keep the small `api/` opt-in surface separate from actual domain value types and parse ADTs.
