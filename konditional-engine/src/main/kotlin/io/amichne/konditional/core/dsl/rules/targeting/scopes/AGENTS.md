# Directory Scope

This file scopes `konditional-engine/src/main/kotlin/io/amichne/konditional/core/dsl/rules/targeting/scopes/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Targeting DSL fragments for locales, platforms, versions, stable IDs, axes, and custom narrowing.

## Local Files
- `AnyOfScope.kt`
- `AxisTargetingScope.kt`
- `ExtensionTargetingScope.kt`
- `LocaleTargetingScope.kt`
- `PlatformTargetingScope.kt`
- `StableIdTargetingScope.kt`
- `VersionTargetingScope.kt`

## Rules
- Every new DSL method needs a corresponding pure targeting representation in `rules/targeting/` and builder support in `internal/builders/`.
- Custom narrowing must preserve context soundness; avoid unchecked casts or runtime-only contracts.
- These scopes describe targeting only; keep evaluation and mutation logic out of this package.
