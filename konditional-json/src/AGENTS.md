# Directory Scope

This file scopes `konditional-json/src/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Source set boundary for the JSON module.

## Go Deeper
- `main/`: Production codecs and schema helpers.
- `test/`: Round-trip and failure-path tests.

## Rules
- Keep production codecs in `main/` and executable boundary specifications in `test/`.
- Do not add generated artifacts under this tree.
