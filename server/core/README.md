# Konditional Server Core

Framework-agnostic REST surface contracts and deterministic OpenAPI generation for Konditional.
This module is data-only: no HTTP routing, no auth middleware, no server runtime.

## What this module provides

- Route catalog (`GET`/`POST`/`PATCH`) with explicit `operationId` values.
- Contract DTOs for snapshot reads and mutations.
- Typed mutation codec outcomes (`SUCCESS` / `FAILURE`) including lifecycle phase.
- OpenAPI generation using Kontract DSL with explicit schema registration.

## Non-goals

- UI metadata models
- UI control descriptors/hints
- Runtime endpoint implementation

## OpenAPI generation

- Generate:
  - `./gradlew :server:core:generateOpenApiSpec`
- Output:
  - `server/core/build/generated/openapi/konditional-surface-openapi.json`
- Build wiring:
  - `assemble` depends on `generateOpenApiSpec`
- Artifact exposure:
  - outgoing configuration: `openapiSpecElements`
  - JAR embedding: `META-INF/openapi/konditional-surface-openapi.json`
  - Maven publication: classifier `openapi` + extension `json`

## Extending the contract

1. Edit routes in `io.amichne.konditional.server.core.surface.route.SurfaceRouteCatalog`.
2. Edit DTOs in `io.amichne.konditional.server.core.surface.dto`.
3. Edit component schemas in `io.amichne.konditional.server.core.surface.schema.SurfaceSchemaRegistry`.
4. Keep `operationId` stable for existing operations.
5. Re-run:
   - `./gradlew :server:core:generateOpenApiSpec :server:core:test`

## Package layout

- `io.amichne.konditional.server.core.surface.dto` - exposed surface DTO contracts
- `io.amichne.konditional.server.core.surface.route` - route catalog + route model primitives
- `io.amichne.konditional.server.core.surface.selector` - target selector ADTs
- `io.amichne.konditional.server.core.surface.profile` - profile/capability model + gating
- `io.amichne.konditional.server.core.surface.spi` - SPI contracts for reads/mutations/codec boundaries
- `io.amichne.konditional.server.core.surface.schema` - component schema registry
- `io.amichne.konditional.server.core.openapi` - OpenAPI model, mapper, renderer, and generator
