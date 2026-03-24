# Directory Scope

This file scopes `konditional-engine/src/main/kotlin/io/amichne/konditional/core/schema/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Compiled namespace schema extraction from declared features.

## Local Files
- `CompiledNamespaceSchema.kt`

## Rules
- Schema extraction should reflect declared namespace state exactly; missing definitions are programmer errors, not soft failures.
- Downstream JSON codecs depend on stable schema shape and feature ordering.
