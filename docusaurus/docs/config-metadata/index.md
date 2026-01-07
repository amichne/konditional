# config-metadata

Contract types for describing how configuration payloads can be interpreted and safely mutated.
This module only defines data structures and a small DSL. It does not generate schemas, compute metadata, or depend on
`konditional-core`.

## Guarantees

**Guarantee**: Metadata is represented as plain Kotlin data structures.

**Mechanism**: `ConfigMetadata` and `ConfigMetadataResponse` are data classes.

**Boundary**: This module does not validate pointer syntax, enforce constraints, or interpret metadata.

## Quick start

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

## Key types

- `BindingType`: identifiers for the kinds of fields you bind in JSON payloads.
- `ValueDescriptor`: sealed interface describing constraints and UI hints.
- `UiHints`: presentation hints (control type, label, help text, ordering).
- `ConfigMetadata`: bindings + descriptors, no validation or generation behavior.
