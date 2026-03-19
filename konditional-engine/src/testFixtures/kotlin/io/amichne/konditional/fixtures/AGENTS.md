# Directory Scope

This file scopes `konditional-engine/src/testFixtures/kotlin/io/amichne/konditional/fixtures/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Reusable test contexts, axis enums, and helper value types for engine and JSON tests.

## Local Files
- `TestFixtures.kt`

## Rules
- Fixtures should model representative behavior without baking in incidental implementation details.
- Prefer extending fixtures here over duplicating domain-like test data across modules.
