# Directory Scope

This file scopes `konditional-json/src/test/kotlin/io/amichne/konditional/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Top-level JSON tests grouped by entrypoint shape.

## Go Deeper
- `json/`: Public `Namespace.toJson` and `fromJson` tests.
- `serialization/`: Configuration codec and namespace-isolation tests.

## Rules
- Keep tests narrow and explicit about whether they target public namespace extensions or lower-level serialization APIs.
