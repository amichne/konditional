# Directory Scope

This file scopes `konditional-engine/src/main/kotlin/io/amichne/konditional/core/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Core engine runtime surface: namespaces, flag definitions, and typed runtime value classification.

## Local Files
- `FlagDefinition.kt`
- `Namespace.kt`
- `ValueType.kt`

## Go Deeper
- `dsl/`: Feature-definition DSL and rule-authoring APIs.
- `evaluation/`: Deterministic bucketing helpers.
- `features/`: Typed feature interfaces and implementations.
- `instance/`: Configuration snapshot value objects.
- `ops/`: Metrics, logging, and registry hooks.
- `registry/`: Atomic namespace snapshot lifecycle.
- `schema/`: Compiled namespace schema extraction.
- `spi/`: Feature-registration extension hooks.

## Rules
- Namespace registration must stay type-safe and deterministic.
- Keep declared defaults and definitions coherent with registry fallbacks; no partial namespace state.
- Push specialized behavior into the focused child packages below instead of expanding this directory indiscriminately.
