# Konditional Spec

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
  - `./gradlew :konditional-spec:generateOpenApiSpec`
- Output:
  - `konditional-spec/build/generated/openapi/konditional-surface-openapi.json`
- Build wiring:
  - `assemble` depends on `generateOpenApiSpec`
- Artifact exposure:
  - outgoing configuration: `openapiSpecElements`
  - JAR embedding: `META-INF/openapi/konditional-surface-openapi.json`
  - Maven publication: classifier `openapi` + extension `json`

## Extending the contract

1. Edit routes in `io.amichne.konditional.configmetadata.contract.openapi.SurfaceRouteCatalog`.
2. Edit DTOs in `io.amichne.konditional.configmetadata.contract.openapi.SurfaceDtos`.
3. Edit component schemas in `io.amichne.konditional.configmetadata.contract.openapi.SurfaceSchemaRegistry`.
4. Keep `operationId` stable for existing operations.
5. Re-run:
   - `./gradlew :konditional-spec:generateOpenApiSpec :konditional-spec:test`

## Package layout

- `io.amichne.konditional.configmetadata.contract.openapi` - route catalog, DTOs, OpenAPI model + renderer
