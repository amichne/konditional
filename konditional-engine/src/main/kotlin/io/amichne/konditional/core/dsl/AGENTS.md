# Directory Scope

This file scopes `konditional-engine/src/main/kotlin/io/amichne/konditional/core/dsl/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Top-level DSL surface for defining flags and composing rule sets.

## Local Files
- `DslSugar.kt`
- `FlagScope.kt`
- `KonditionalDsl.kt`
- `VariantDispatchHost.kt`
- `VersionRangeScope.kt`

## Go Deeper
- `rules/`: Rule-building scopes and deferred-yield mechanics.

## Rules
- Preserve compile-time guidance through generics; avoid APIs that erase context or namespace type information.
- Sugar helpers should compile down to the same rule semantics as the explicit builders.
