# Directory Scope

This file scopes `konditional-engine/src/main/kotlin/io/amichne/konditional/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Top-level engine package split by public API, DSL/core runtime, rule model, and internal helpers.

## Go Deeper
- `api/`: Evaluation entrypoints and metrics/explain integration.
- `context/`: Engine-only contextual primitives such as rollout percentages.
- `core/`: Namespace model, DSL surface, runtime registry, and schema compilation.
- `internal/`: Builder and serialization support internals.
- `rules/`: Immutable rule/value/targeting model used by evaluation.

## Rules
- Prefer the narrowest subtree that owns the behavior before editing files here.
- Public-facing changes usually start in `api/`, `core/`, or `rules/`; `internal/` is implementation detail only.
