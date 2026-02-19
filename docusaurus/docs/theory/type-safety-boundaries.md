# Type Safety Boundaries

Konditional has two safety domains. Compile-time safety covers statically
declared Kotlin feature definitions. Runtime boundary safety covers external
configuration payloads.

## Core claim

Type safety is qualified by boundary location.

- In code-defined DSL: the compiler enforces feature typing and context typing.
- At JSON ingress: boundary parsing enforces schema and type integrity through
  `Result`.

## Compile-time domain

Statically defined features provide compile-time guarantees:

1. Feature access is symbol-based, not string-keyed.
2. Return types are inferred from feature declarations.
3. Rule value types must match feature value types.
4. Defaults are required for total evaluation.
5. Context type parameters constrain valid evaluation call sites.

These guarantees are driven by Kotlin generics and delegated feature
construction around `Feature<T, C, M>`.

## Runtime boundary domain

At the boundary, the compiler cannot validate payload syntax or payload schema.
Konditional handles this with explicit parse semantics.

```kotlin
val result: Result<MaterializedConfiguration> =
    ConfigurationSnapshotCodec.decode(json, AppFeatures.compiledSchema())

result
    .onSuccess { materialized -> AppFeatures.load(materialized.configuration) }
    .onFailure { failure -> logger.error(failure.parseErrorOrNull()?.message.orEmpty()) }
```

Boundary decode validates payload structure, feature identity scope, and typed
value compatibility against the compiled namespace schema.

## Cutover model

1. Untrusted JSON crosses into boundary decode.
2. Successful materialization creates trusted runtime data.
3. Evaluation re-enters typed semantics over trusted snapshots.

## Tradeoff

Konditional intentionally combines compile-time DSL guarantees with runtime
boundary parsing. It does not claim that raw external JSON is compile-time safe.

## Related

- [Parse don’t validate](/theory/parse-dont-validate)
- [Type safety in learn docs](/learn/type-safety)
- [Parse result API](/reference/api/parse-result)

## Claim ledger

| claim_id | claim_statement | claim_kind | status |
| --- | --- | --- | --- |
| TH-002-C1 | Feature definitions are namespace-bound typed symbols rather than stringly runtime lookups. | guarantee | supported |
| TH-002-C2 | Context capability constraints are enforced by type parameters in feature evaluation. | guarantee | supported |
| TH-002-C3 | Boundary parse failures preserve typed error semantics at module boundaries. | failure_mode | supported |
| TH-002-C4 | Schema compilation defines the trusted domain accepted by snapshot decode. | mechanism | supported |
| TH-002-C5 | Namespace snapshot loading returns typed materialized success and never an untyped payload. | boundary | supported |
| TH-002-C6 | Typed flag definitions preserve runtime evaluation type integrity across rule/default selection. | guarantee | supported |
