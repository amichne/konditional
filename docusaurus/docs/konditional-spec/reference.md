# Konditional spec reference

This page lists the concrete schema contract APIs that back the
`konditional-spec` documentation surface in this repository.

## Read this page when

- You need symbol-level schema contract definitions.
- You are integrating schema providers with serialization boundaries.
- You want to verify current contract names after module refactors.

## API and contract reference

### OpenAPI base contract (`:openapi`)

```kotlin
interface OpenApi<out T : Any> {
    val type: OpenApi.Type
    val title: String?
    val description: String?
    val default: T?
    val nullable: Boolean
    val example: T?
    val deprecated: Boolean

    enum class Type(val serialized: String) {
        STRING,
        INTEGER,
        NUMBER,
        BOOLEAN,
        ARRAY,
        OBJECT,
        NULL,
    }
}
```

### Schema provider contract (`:kontracts`)

```kotlin
interface SchemaProvider<out S : JsonSchema<*>> {
    val schema: S
}
```

### Primary schema families (`:kontracts`)

- Primitive schemas: `StringSchema`, `IntSchema`, `DoubleSchema`,
  `BooleanSchema`, `NullSchema`.
- Composite schemas: `ObjectSchema`, `ArraySchema`, `MapSchema`, `OneOfSchema`,
  `RefSchema`.
- Validation channel: `ValidationResult.Valid` and `ValidationResult.Invalid`.

## Deterministic API and contract notes

- Schema types are immutable values, so the same schema definition yields the
  same contract shape.
- The schema plane is decoupled from runtime mutation; loading remains a
  separate boundary step.
- Contract evolution is explicit in source and testable via schema fixtures.

## Canonical conceptual pages

- [Theory: Type safety boundaries](/theory/type-safety-boundaries)
- [Theory: Parse don't validate](/theory/parse-dont-validate)
- [Theory: Determinism proofs](/theory/determinism-proofs)

## Next steps

- [Kontracts schema DSL reference](/kontracts/schema-dsl)
- [Namespace snapshot loader API](/reference/api/snapshot-loader)
- [Boundary result API](/reference/api/parse-result)
