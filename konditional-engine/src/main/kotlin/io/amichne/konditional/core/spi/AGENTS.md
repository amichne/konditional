# Directory Scope

This file scopes `konditional-engine/src/main/kotlin/io/amichne/konditional/core/spi/` and yields to deeper `AGENTS.md` files when present.

## Purpose
SPI hooks fired when features are declared.

## Local Files
- `FeatureRegistrationHook.kt`
- `FeatureRegistrationHooks.kt`

## Rules
- SPI hooks are extension points, not control flow; keep registration notifications simple and deterministic.
- Avoid introducing dependencies from SPI hooks back into runtime state management.
