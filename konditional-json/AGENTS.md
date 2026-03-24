# Directory Scope

This file scopes `konditional-json/` and yields to deeper `AGENTS.md` files when present.

## Purpose
JSON module: strict Moshi codecs and schema-aware boundary parsing layered on top of engine types.

## Local Files
- `build.gradle.kts`

## Go Deeper
- `src/`: Production codecs and regression tests.

## Rules
- Failed decode must never mutate live namespace state.
- Keep this module at the parse boundary: turn untrusted JSON into typed engine models or typed failures.
