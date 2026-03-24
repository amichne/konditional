# Directory Scope

This file scopes `konditional-json/src/main/kotlin/io/amichne/konditional/internal/serialization/models/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Strict wire models that rebuild typed engine configuration from JSON.

## Local Files
- `FlagValue.kt`
- `SerializableFlag.kt`
- `SerializableRule.kt`
- `SerializableSnapshot.kt`
- `SerializableSnapshotMetadata.kt`

## Rules
- Materialization must remain schema-aware and deterministic; unknown or missing features fail loudly.
- Failed reconstruction must not partially apply values or weaken typed boundary failures.
