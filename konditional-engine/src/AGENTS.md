# Directory Scope

This file scopes `konditional-engine/src/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Source set boundary for the engine module.

## Go Deeper
- `main/`: Production engine APIs and internals.
- `test/`: Behavioral and concurrency regression coverage.
- `testFixtures/`: Reusable fixtures for sibling modules and tests.

## Rules
- Keep production behavior in `main/`, executable specifications in `test/`, and reusable helpers in `testFixtures/`.
- Do not add generated or ephemeral content under this tree.
