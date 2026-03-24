# Directory Scope

This file scopes `konditional-engine/src/testFixtures/kotlin/io/amichne/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Routing scope for this subtree; go deeper before making behavior changes.

## Go Deeper
- `konditional/`: Module package root.

## Rules
- Do not add behavior at this level unless you are intentionally creating a new package boundary.
- When changing a child package, read the deeper `AGENTS.md` first; it owns the concrete rules.
