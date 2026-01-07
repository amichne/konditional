# kontracts

Type-safe JSON Schema DSL for Kotlin used by Konditional for structured value validation.

## Installation

```kotlin
dependencies {
    implementation(project(":kontracts"))
}
```

## Guarantees

**Guarantee**: Schemas are expressed as Kotlin types, not untyped JSON strings.

**Mechanism**: Type-inferred DSL (`schemaRoot { ::property of { ... } }`).

**Boundary**: Runtime JSON validation is explicit; schema correctness does not imply correct business logic.

## Next steps

- [Schema DSL](/kontracts/schema-dsl)
