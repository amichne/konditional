# Directory Scope

This file scopes `konditional-types/src/main/kotlin/io/amichne/konditional/api/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Opt-in annotation surface for internal APIs shared across modules.

## Local Files
- `KonditionalInternalApi.kt`

## Rules
- Keep this package minimal and stable; broadening internal opt-ins weakens API boundaries.
