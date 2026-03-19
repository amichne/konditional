# Directory Scope

This file scopes `konditional-types/src/main/kotlin/io/amichne/konditional/core/result/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Typed parse-failure taxonomy and result wrappers shared by JSON and other boundaries.

## Local Files
- `KonditionalBoundaryFailure.kt`
- `ParseError.kt`
- `ParseException.kt`
- `ParseResult.kt`

## Rules
- Extend failures by adding structured cases rather than stringly exceptions or ambiguous nulls.
- Equality and exhaustive matching over these ADTs are part of the contract.
