# Directory Scope

This file scopes `konditional-types/src/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Source set boundary for the foundational types module.

## Go Deeper
- `main/`: Production foundational types.
- `test/`: Determinism and parse-boundary regression tests.

## Rules
- Keep shared type definitions in `main/` and executable semantic coverage in `test/`.
- Do not add generated content under this tree.
