# Directory Scope

This file scopes `konditional-types/src/main/kotlin/io/amichne/konditional/values/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Public identifier value types and validation helpers for features, namespaces, and rules.

## Local Files
- `FeatureId.kt`
- `IdentifierEncoding.kt`
- `NamespaceId.kt`
- `RuleId.kt`
- `Validateable.kt`

## Rules
- Preserve separator, prefix, and seed compatibility because these identifiers are part of the wire format.
- Namespace and rule identifiers should stay deterministic and composable; avoid ad hoc string construction elsewhere.
