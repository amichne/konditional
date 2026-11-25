# Directory Scope

This file scopes `konditional-json/src/main/kotlin/io/amichne/konditional/internal/serialization/adapters/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Custom Moshi adapters and factories for identifiers, flag values, fact expressions, and primitive maps.

## Local Files
- `FlagValueAdapter.kt`
- `FlagValueJsonMaps.kt`
- `IdentifierJsonAdapter.kt`
- `FactSpecAdapter.kt`
- `SnapshotFieldAdapter.kt`

## Rules
- Preserve strict discriminators, numeric fidelity, and `JsonDataException` behavior for invalid payloads.
- Resolve fact names and operands against the destination namespace before installation.
