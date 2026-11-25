# Directory Scope

This file scopes `konditional-engine/src/main/kotlin/io/amichne/konditional/internal/builders/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Mutable builder implementation that assembles immutable rules and flag definitions from the DSL.

## Local Files
- `FlagBuilder.kt`

## Rules
- Builder mutation is an implementation detail; emitted rule and flag models must remain deterministic and immutable.
- Bind each condition exactly once, in declaration order, and reject an expression that uses another schema's fact before the definition is built.
- Rule ordinals are ordering-sensitive: they seed rule identifiers.
