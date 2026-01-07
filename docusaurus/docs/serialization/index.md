# konditional-serialization

`konditional-serialization` handles JSON snapshot/patch formats and parsing at the configuration boundary.

Use this module when you want to load or persist configuration outside the process.

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
