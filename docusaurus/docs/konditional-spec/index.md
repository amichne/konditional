# konditional-spec

`konditional-spec` is the reference entry for schema contracts used across
Konditional modules. In this repository, that contract surface is implemented
by `:openapi` and `:kontracts`.

## Read this page when

- You are defining or reviewing API schema contracts.
- You need a module-level map before going into symbol-level references.
- You are replacing older `konditional-spec` assumptions with current modules.

## API and contract surface

- `:openapi` exposes `OpenApi<T>` and canonical OpenAPI primitive type enums.
- `:kontracts` exposes typed schema builders (`schema`, `of`, `asString`, and
  related APIs).
- `:konditional-serialization` consumes compiled schema contracts at the JSON
  boundary through schema-aware codecs.

## Deterministic API and contract notes

- Schema definitions are explicit value models; runtime behavior does not depend
  on reflection-based discovery.
- Contract construction is deterministic for a fixed Kotlin source definition.
- Boundary ingestion remains explicit: untrusted JSON is parsed into trusted
  typed models before load.

## Canonical conceptual pages

- [Theory: Type safety boundaries](/theory/type-safety-boundaries)
- [Theory: Parse don't validate](/theory/parse-dont-validate)
- [How-to: Safe remote config loading](/how-to-guides/safe-remote-config)

## Next steps

- [konditional-spec reference](/konditional-spec/reference)
- [Kontracts module reference](/kontracts)
- [Kontracts schema DSL reference](/kontracts/schema-dsl)
