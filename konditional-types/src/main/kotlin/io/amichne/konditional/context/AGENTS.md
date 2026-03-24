# Directory Scope

This file scopes `konditional-types/src/main/kotlin/io/amichne/konditional/context/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Execution-context mixins and shared contextual leaf types such as locale, platform, version, and keys.

## Local Files
- `AppLocale.kt`
- `Context.kt`
- `ContextKey.kt`
- `LocaleTag.kt`
- `Platform.kt`
- `PlatformTag.kt`
- `Version.kt`

## Go Deeper
- `axis/`: Typed custom-axis identifiers and grouped values.

## Rules
- Context capability absence should remain a normal non-match boundary rather than an exception path.
- Locale, platform, and version identities are consumed by targeting and serialization; preserve their canonical forms.
