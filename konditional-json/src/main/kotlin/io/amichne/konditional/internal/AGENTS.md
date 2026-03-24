# Directory Scope

This file scopes `konditional-json/src/main/kotlin/io/amichne/konditional/internal/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Private JSON implementation packages.

## Go Deeper
- `serialization/`: Wire adapters and serializable snapshot models.

## Rules
- Everything here is internal-only plumbing; keep public API concerns in `serialization/`.
