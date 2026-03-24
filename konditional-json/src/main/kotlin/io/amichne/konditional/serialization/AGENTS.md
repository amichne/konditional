# Directory Scope

This file scopes `konditional-json/src/main/kotlin/io/amichne/konditional/serialization/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Public JSON surface: namespace extensions, schema extraction, schema-value codecs, and snapshot codec routing.

## Local Files
- `NamespaceExtensions.kt`
- `SchemaExtraction.kt`
- `SchemaValueCodec.kt`

## Go Deeper
- `internal/`: Low-level primitive and `Konstrained` conversions.
- `snapshot/`: Top-level configuration codec wiring.

## Rules
- Keep the public API strict: return typed parse failures instead of best-effort recovery.
- Schema extraction and `Konstrained` codec behavior must stay aligned with `:konditional-types` and engine schema compilation.
