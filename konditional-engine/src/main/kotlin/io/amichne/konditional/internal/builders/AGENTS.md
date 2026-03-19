# Directory Scope

This file scopes `konditional-engine/src/main/kotlin/io/amichne/konditional/internal/builders/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Mutable builder implementations that assemble immutable rules and flag definitions from the DSL.

## Local Files
- `AnyOfBuilder.kt`
- `FlagBuilder.kt`
- `RuleBuilder.kt`
- `RuleVariantScope.kt`

## Go Deeper
- `versions/`: Version-range builder state.

## Rules
- Builder mutation is an implementation detail; emitted rule and flag models must remain deterministic and immutable.
- Pending yields, rule ordinals, and variant collection are ordering-sensitive.
