# Kontracts schema DSL reference

This page is the symbol-level reference for Kontracts schema construction and
runtime validation APIs.

## Read this page when

- You need exact DSL entrypoints and signatures.
- You are reviewing schema authoring patterns for custom types.
- You are validating JSON values against schema contracts.

## API and contract reference

### Root schema entrypoint

```kotlin
fun schema(builder: RootObjectSchemaBuilder.() -> Unit): ObjectSchema
```

### Type-inferred field builders

```kotlin
context(root: RootObjectSchemaBuilder)
infix fun KProperty0<String>.of(builder: StringSchemaBuilder.() -> Unit = {})

context(root: RootObjectSchemaBuilder)
infix fun KProperty0<Int>.of(builder: IntSchemaBuilder.() -> Unit = {})

context(root: RootObjectSchemaBuilder)
infix fun KProperty0<Double>.of(builder: DoubleSchemaBuilder.() -> Unit = {})

context(root: RootObjectSchemaBuilder)
infix fun KProperty0<Boolean>.of(builder: BooleanSchemaBuilder.() -> Unit = {})
```

### Custom type mappings

```kotlin
context(root: RootObjectSchemaBuilder)
infix fun <reified V : Any> KProperty0<V>.asString(
    builder: CustomStringSchemaBuilder<V>.() -> Unit,
)

context(root: RootObjectSchemaBuilder)
infix fun <reified V : Any> KProperty0<V>.asInt(
    builder: CustomIntSchemaBuilder<V>.() -> Unit,
)
```

`asDouble` and `asBoolean` follow the same contract shape.

### Additional schema factories

- `objectSchema { ... }`
- `mapSchema { ... }`
- `oneOfSchema { ... }` with optional discriminator mapping
- `schemaRef("#/components/schemas/...")`

### Runtime validation channel

```kotlin
sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid internal constructor(val message: String) : ValidationResult()
}

sealed interface JsonValue {
    fun validate(schema: JsonSchema<*>): ValidationResult
}
```

## Deterministic API and contract notes

- `schema { ... }` builds immutable object schemas from explicit builder state.
- Field inference derives from Kotlin property types, so type drift is caught at
  compile time.
- Validation is pure for fixed `JsonValue` and `JsonSchema` inputs.

## Canonical conceptual pages

- [Theory: Type safety boundaries](/theory/type-safety-boundaries)
- [Theory: Parse don't validate](/theory/parse-dont-validate)
- [How-to: Custom business logic](/how-to-guides/custom-business-logic)

## Next steps

- [Kontracts module reference](/kontracts)
- [konditional-spec reference](/konditional-spec/reference)
- [Boundary result API](/reference/api/parse-result)
