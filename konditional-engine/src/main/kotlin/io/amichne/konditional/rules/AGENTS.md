# Directory Scope

This file scopes `konditional-engine/src/main/kotlin/io/amichne/konditional/rules/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Immutable rule and conditional-value domain model used by namespace evaluation.

## Local Files
- `ConditionalValue.kt`
- `Rule.kt`

## Go Deeper
- `targeting/`: Pure targeting tree and projection helpers.
- `versions/`: Immutable version-range variants.

## Rules
- Keep rule matching and value resolution pure with respect to the provided context and registry snapshot.
- Static and contextual resolvers must preserve identical precedence semantics.
