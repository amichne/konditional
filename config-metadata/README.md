# Config Metadata

Contract types for describing how configuration payloads can be interpreted and safely mutated.
This module only defines data structures and a small DSL. It does not generate schemas, compute
metadata, or depend on `core`.

## What this module does

- **Bindings** map JSON Pointer templates (RFC 6901 + `*` wildcards) to `BindingType` identifiers.
- **Descriptors** declare constraints and UI hints for each `BindingType`.
- **Responses** provide an envelope for returning a state payload plus its metadata.

## Guarantees

- **Guarantee**: Metadata is represented as plain Kotlin data structures.
- **Mechanism**: `ConfigMetadata` and `ConfigMetadataResponse` are `data class` values.
- **Boundary**: This module does not validate pointer syntax, enforce constraints, or interpret metadata.

## Validation Boundary

- **Guarantee**: No constraint validation is performed by this module.
- **Mechanism**: Descriptors are data-only; there is no evaluator.
- **Boundary**: Validate JSON pointers and value constraints in your own boundary layer.

## Non-Goals

- OpenAPI/JSON schema generation
- Snapshot serialization or conversion
- Catalogs or pre-filled bindings

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
- `ConfigMetadata`: bindings + descriptors, no validation or generation behavior.

## Package Layout

- `io.amichne.konditional.configmetadata.contract` — response + metadata contracts
- `io.amichne.konditional.configmetadata.descriptor` — descriptor types and enums
- `io.amichne.konditional.configmetadata.ui` — UI hint models
- `io.amichne.konditional.configmetadata.dsl` — builder + DSL entrypoint
