# Directory Scope

This file scopes `konditional-engine/src/main/kotlin/io/amichne/konditional/core/dsl/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Top-level DSL surface for declaring flags and pairing outcomes with conditions.

## Local Files
- `DslSugar.kt`
- `FlagScope.kt`
- `KonditionalDsl.kt`

## Go Deeper
- `rules/`: The receiver for deferred rule values.

## Rules
- Preserve compile-time guidance through generics; avoid APIs that erase context or fact-schema type information.
- Every rule setting is an argument. Do not add a rule form that takes a `Unit` block: a comparison written inside one is discarded without an error.
- A rule's criteria is one `Condition<S>`, bound once to the namespace's schema at declaration.
- Sugar helpers must delegate to `rule` or `always` on `FlagScope`.
