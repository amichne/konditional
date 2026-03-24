# Directory Scope

This file scopes `konditional-engine/src/main/kotlin/io/amichne/konditional/rules/versions/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Immutable version-range variants and shared base semantics.

## Local Files
- `FullyBound.kt`
- `LeftBound.kt`
- `RightBound.kt`
- `Unbounded.kt`
- `VersionRange.kt`

## Rules
- Preserve documented bound inclusivity and comparison behavior.
- Any change here affects targeting, JSON codecs, and tests across modules.
