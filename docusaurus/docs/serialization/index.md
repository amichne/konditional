# konditional-serialization

`konditional-serialization` handles JSON snapshot/patch formats and parsing at the configuration boundary.

## When to Use This Module

You should use `konditional-serialization` when you need to:
- Store feature flag configuration in files, databases, or remote services as JSON
- Load validated configuration snapshots from external sources
- Apply incremental patches to update configuration without full replacement
- Ensure invalid JSON never becomes active configuration

## What You Get

- **JSON snapshot encoding/decoding**: Serialize and deserialize complete configuration snapshots
- **Incremental patch support**: Apply partial updates to existing configurations
- **Parse-don't-validate boundary**: Invalid JSON returns `ParseResult.Failure`, never throws
- **Type-safe configuration**: Parsed JSON becomes strongly-typed `Configuration` objects

## Alternatives

Without this module, you would need to:
- Manually serialize and deserialize feature flag state using custom JSON logic
- Build your own validation boundary to prevent invalid configuration from activating
- Handle JSON parsing errors with try-catch blocks instead of explicit result types

## Installation

```kotlin
dependencies {
    implementation("io.amichne:konditional-serialization:VERSION")
}
```

## Guarantees

**Guarantee**: Invalid JSON payloads never become `Configuration` values.

**Mechanism**: Parsing returns `ParseResult.Success` or `ParseResult.Failure` and never throws for validation errors.

**Boundary**: Semantic correctness (for example, whether a rollout should be 10% or 20%) is not validated.

## Next steps

- [Serialization API reference](/serialization/reference)
- [Persistence format](/serialization/persistence-format)
- [Runtime lifecycle](/runtime/lifecycle)
