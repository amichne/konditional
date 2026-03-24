# Directory Scope

This file scopes `konditional-engine/src/test/kotlin/io/amichne/konditional/runtime/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Explicit atomicity and whole-snapshot runtime regression tests.

## Local Files
- `NamespaceAtomicityTest.kt`

## Rules
- Changes in `core/registry/` should add or update coverage here for reader and writer interleavings.
