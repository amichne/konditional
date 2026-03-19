# Directory Scope

This file scopes `konditional-engine/src/main/kotlin/io/amichne/konditional/core/instance/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Configuration snapshot value objects.

## Local Files
- `Configuration.kt`

## Rules
- Treat these classes as immutable snapshot state passed across registry boundaries.
- Metadata shape changes affect JSON and contract code paths; audit downstream modules before changing them.
