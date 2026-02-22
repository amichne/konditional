# kontracts

`kontracts` is Konditional's API-first JSON Schema DSL module. It gives you
compile-time schema construction and explicit runtime validation contracts.

## Read this page when

- You need typed schema contracts for structured values.
- You are replacing ad hoc JSON schema strings with Kotlin DSL definitions.
- You want the module-level API map before using specific DSL symbols.

## API and contract surface

- Artifact: `io.amichne:konditional-kontracts:VERSION`
- Entry DSL: `schema { ... }` for root object contracts.
- Type-inferred fields: `::property of { ... }`
- Custom mappings: `::property asString { ... }`, `asInt`, `asDouble`,
  `asBoolean`.
- Runtime value validation: `JsonValue.validate(schema): ValidationResult`

## Deterministic API and contract notes

- Schema construction is source-driven and deterministic for fixed inputs.
- Validation returns typed `ValidationResult` values instead of ambient
  side-channel state.
- Schema and value models are immutable, so validation has no partial-update
  surface.

## Canonical conceptual pages

- [Theory: Type safety boundaries](/theory/type-safety-boundaries)
- [Theory: Parse don't validate](/theory/parse-dont-validate)
- [How-to: Custom business logic](/how-to-guides/custom-business-logic)

## Next steps

- [Kontracts schema DSL reference](/kontracts/schema-dsl)
- [konditional-spec reference](/konditional-spec/reference)
- [Namespace snapshot loader API](/reference/api/snapshot-loader)
