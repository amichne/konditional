# Context: Kontracts JSON Schema DSL

You are documenting Kontracts, a standalone submodule providing a type-safe JSON Schema DSL. Your audience is developers who want to define, generate, or validate JSON schemas in Kotlin.

## Scope

Focus on:

- **Schema DSL syntax**: How to define schemas using the Kotlin DSL
- **Type inference**: How schema types are automatically detected
- **Custom type mapping**: Mapping domain types to JSON Schema types
- **Validation**: Runtime validation with detailed error messages
- **OpenAPI compatibility**: Generating OpenAPI 3.1 compatible schemas
- **Standalone usage**: Using Kontracts independently of Konditional

## Relationship to Konditional

Kontracts is extracted as a submodule that can be used independently:

- **Within Konditional**: Used for configuration schema validation
- **Standalone**: Can be published and used as an independent library
- **Zero dependencies**: Only Kotlin stdlib required

## Key Features to Document

### Type-Inferred DSL

```kotlin
// Schema type is inferred from the DSL structure
val userSchema = schema {
    obj {
        "name" to string()
        "age" to integer { minimum(0) }
        "email" to string { format("email") }
    }
}
```

### Custom Type Mapping

```kotlin
// Map domain types to schema representations
val schema = schema {
    obj {
        "status" to enum<OrderStatus>()
        "amount" to decimal { minimum(0.0) }
    }
}
```

### Validation with Detailed Errors

```kotlin
val result = userSchema.validate(jsonObject)
when (result) {
    is ValidationResult.Valid -> // proceed
    is ValidationResult.Invalid -> {
        result.errors.forEach { error ->
            println("${error.path}: ${error.message}")
        }
    }
}
```

### OpenAPI 3.1 Generation

```kotlin
val openApiSchema = userSchema.toOpenApi()
// Produces OpenAPI 3.1 compatible JSON Schema
```

## DSL Reference

Document the available schema builders:

| Builder | JSON Schema Type | Example |
|---------|-----------------|---------|
| `string()` | `{ "type": "string" }` | `"name" to string()` |
| `integer()` | `{ "type": "integer" }` | `"count" to integer()` |
| `number()` | `{ "type": "number" }` | `"price" to number()` |
| `boolean()` | `{ "type": "boolean" }` | `"active" to boolean()` |
| `array { }` | `{ "type": "array" }` | `"items" to array { string() }` |
| `obj { }` | `{ "type": "object" }` | `"user" to obj { ... }` |
| `enum<T>()` | `{ "enum": [...] }` | `"status" to enum<Status>()` |
| `nullable()` | Adds `null` to type | `"optional" to string().nullable()` |
| `oneOf { }` | `{ "oneOf": [...] }` | Union types |
| `ref()` | `{ "$ref": "..." }` | Schema references |

## Constraint Builders

Document available constraints:

### String Constraints
```kotlin
string {
    minLength(1)
    maxLength(100)
    pattern("[a-z]+")
    format("email")  // or "uri", "date-time", etc.
}
```

### Numeric Constraints
```kotlin
integer {
    minimum(0)
    maximum(100)
    exclusiveMinimum(0)
    multipleOf(5)
}
```

### Array Constraints
```kotlin
array {
    items { string() }
    minItems(1)
    maxItems(10)
    uniqueItems(true)
}
```

### Object Constraints
```kotlin
obj {
    "required_field" to string()
    "optional_field" to string().optional()
    additionalProperties(false)
}
```

## Out of Scope (defer to other domains)

- Konditional feature flag usage → See `01-public-api.md`
- How Konditional uses Kontracts internally → See `05-configuration-integrity.md`

## Constraints

- Examples should be self-contained and runnable
- Show the DSL approach, not manual JSON construction
- Document OpenAPI compatibility explicitly
- Note any deviations from JSON Schema draft versions

## Error Message Quality

Document how validation errors are structured:

```kotlin
data class ValidationError(
    val path: JsonPath,      // e.g., "$.user.email"
    val message: String,     // Human-readable description
    val keyword: String,     // JSON Schema keyword that failed
    val expected: Any?,      // What was expected
    val actual: Any?         // What was found
)
```

## Output Format

For Kontracts documentation, produce:
1. Feature being documented
2. DSL syntax with example
3. Generated JSON Schema output
4. Validation behavior
5. Edge cases or limitations

## Context Injection Point

When documenting specific DSL features, inject source here:

```
[INSERT: Schema builder implementations, validation logic, type mapping code]
```

## Standalone Publication Notes

If documenting Kontracts for independent publication:

- Repository structure for extraction
- Gradle module configuration
- Version compatibility with Konditional
- Migration guide for users upgrading from bundled to standalone
