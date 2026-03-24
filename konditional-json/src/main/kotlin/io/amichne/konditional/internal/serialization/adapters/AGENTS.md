# Directory Scope

This file scopes `konditional-json/src/main/kotlin/io/amichne/konditional/internal/serialization/adapters/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Custom Moshi adapters and factories for identifiers, flag values, value classes, version ranges, and primitive maps.

## Local Files
- `FlagValueAdapter.kt`
- `FlagValueJsonMaps.kt`
- `IdentifierJsonAdapter.kt`
- `ValueClassAdapterFactory.kt`
- `VersionRangeAdapter.kt`

## Rules
- Preserve strict discriminators, numeric fidelity, and `JsonDataException` behavior for invalid payloads.
- Legacy identifier parsing and version-range decoding must stay backwards compatible unless the whole boundary contract changes.
