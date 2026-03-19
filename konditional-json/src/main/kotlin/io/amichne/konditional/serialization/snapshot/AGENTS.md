# Directory Scope

This file scopes `konditional-json/src/main/kotlin/io/amichne/konditional/serialization/snapshot/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Top-level strict JSON codec for exported and imported namespace snapshots.

## Local Files
- `ConfigurationCodec.kt`

## Rules
- Moshi adapter registration order is part of decode behavior; change it carefully.
- Decode returns typed failures and must not mutate namespaces on error.
