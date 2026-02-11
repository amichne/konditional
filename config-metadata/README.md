# Config Metadata

Contract types for describing how configuration payloads can be interpreted and safely mutated.
This module provides:
- metadata contracts + DSL
- contract-first OpenAPI generation for the surface API

## What this module does

- **Bindings** map JSON Pointer templates (RFC 6901 + `*` wildcards) to `BindingType` identifiers.
- **Descriptors** declare constraints and UI hints for each `BindingType`.
- **Responses** provide an envelope for returning a state payload plus its metadata.
- **OpenAPI generation** builds a deterministic spec from the route catalog + explicit DTO schemas.

## Guarantees

- **Guarantee**: Metadata is represented as plain Kotlin data structures.
- **Mechanism**: `ConfigMetadata` and `ConfigMetadataResponse` are `data class` values.
- **Boundary**: This module does not validate pointer syntax, enforce constraints, or interpret metadata.

## Validation Boundary

- **Guarantee**: No constraint validation is performed by this module.
- **Mechanism**: Descriptors are data-only; there is no evaluator.
- **Boundary**: Validate JSON pointers and value constraints in your own boundary layer.

## Non-Goals

- Snapshot serialization or conversion
- Runtime endpoint implementation
- Catalog pre-filling with environment-specific values

## OpenAPI Generation

- Generate command:
  - `./gradlew :config-metadata:generateOpenApiSpec`
- Canonical output:
  - `config-metadata/build/generated/openapi/konditional-surface-openapi.json`
- Build lifecycle wiring:
  - `assemble` depends on `generateOpenApiSpec`
- Artifact exposure:
  - outgoing configuration: `openapiSpecElements`
  - JAR embedding: `META-INF/openapi/konditional-surface-openapi.json`
  - Maven publication: classifier `openapi`, extension `json`

## Extending the Spec

1. Add/modify route contracts in `io.amichne.konditional.configmetadata.contract.openapi.SurfaceRouteCatalog`.
2. Add/modify DTO contract classes in `io.amichne.konditional.configmetadata.contract.openapi.SurfaceDtos`.
3. Register/adjust component schemas in `io.amichne.konditional.configmetadata.contract.openapi.SurfaceSchemaRegistry`.
4. Keep `operationId` values stable and explicit when adding operations.
5. Regenerate and verify:
   - `./gradlew :config-metadata:generateOpenApiSpec :config-metadata:test`

## Quick Start

```kotlin
import io.amichne.konditional.configmetadata.contract.BindingType
import io.amichne.konditional.configmetadata.contract.ConfigMetadataResponse
import io.amichne.konditional.configmetadata.descriptor.BooleanDescriptor
import io.amichne.konditional.configmetadata.dsl.configMetadata
import io.amichne.konditional.configmetadata.ui.UiControlType
import io.amichne.konditional.configmetadata.ui.UiHints

val metadata = configMetadata {
    bind("/flags/*/isActive", BindingType.FLAG_ACTIVE)
    describe(
        BindingType.FLAG_ACTIVE,
        BooleanDescriptor(
            uiHints = UiHints(
                control = UiControlType.TOGGLE,
                label = "Active",
                helpText = "Disables the flag when false.",
            ),
        ),
    )
}

val response = ConfigMetadataResponse(
    state = /* your state payload */,
    metadata = metadata,
)
```

## Key Types

- `BindingType`: identifiers for the kinds of fields you bind in JSON payloads.
- `ValueDescriptor`: sealed interface describing constraints and UI hints.
- `UiHints`: presentation hints (control type, label, help text, ordering).
- `ConfigMetadata`: bindings + descriptors, no validation behavior.

## Package Layout

- `io.amichne.konditional.configmetadata.contract` — response + metadata contracts
- `io.amichne.konditional.configmetadata.descriptor` — descriptor types and enums
- `io.amichne.konditional.configmetadata.ui` — UI hint models
- `io.amichne.konditional.configmetadata.dsl` — builder + DSL entrypoint
- `io.amichne.konditional.configmetadata.contract.openapi` — route catalog + DTO contracts + OpenAPI generator
