# Directory Scope

This file scopes `konditional-json/src/test/kotlin/io/amichne/konditional/serialization/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Lower-level codec tests for configuration round trips, invalid payload rejection, and namespace isolation.

## Local Files
- `ConfigurationCodecTest.kt`

## Rules
- Changes in snapshot materialization or schema-aware decoding should extend this package first.
