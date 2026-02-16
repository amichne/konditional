# server:rest-spec

Contract-first REST surface specification module for Konditional.
The module is data-only and transport-agnostic.

## When to use this module

Use `server:rest-spec` when you need to:

- define canonical surface routes and operation IDs
- generate deterministic OpenAPI JSON from Kotlin contracts
- describe mutation outcomes with typed codec result variants

## What this module includes

- route catalog (`GET`, `POST`, `PATCH`)
- DTO contracts for snapshot reads and mutations
- error and codec-outcome envelopes
- OpenAPI schema registry and JSON renderer

## What it explicitly excludes

- UI metadata descriptors and UI hint models
- server routing/runtime
- persistence/auth implementation

## Quick start

```bash
./gradlew :server:rest-spec:generateOpenApiSpec
```

Generated output:

- `server/rest-spec/build/generated/openapi/konditional-surface-openapi.json`
