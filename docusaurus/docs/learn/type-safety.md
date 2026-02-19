# Type Safety in Konditional

Konditional provides compile-time type safety for statically declared features
and explicit runtime boundary safety for external configuration.

## Compile-time guarantees

1. Feature access is property/symbol based.
2. Feature return type is known at call sites.
3. Rule values must match feature value type.
4. Defaults enforce total evaluation.
5. Context type parameters constrain valid evaluation calls.

## Runtime boundary guarantees

External JSON is not compile-time checked. It is validated at the snapshot
boundary and either materialized safely or rejected.

```kotlin
val result = NamespaceSnapshotLoader(AppFeatures).load(json)
if (result.isFailure) {
    logger.error(result.parseErrorOrNull()?.message.orEmpty())
}
```

Invalid payloads remain boundary failures and do not replace active runtime
state.

## Practical posture

Use compiler guarantees inside feature DSL and app call sites. Use explicit
`Result` handling at ingestion boundaries.

## Related

- [Type safety boundaries](/theory/type-safety-boundaries)
- [Configuration lifecycle](/learn/configuration-lifecycle)
- [Parse result API](/reference/api/parse-result)

## Claim ledger

| claim_id | claim_statement | claim_kind | status |
| --- | --- | --- | --- |
| LRN-003-C1 | Feature access is compile-time checked through typed namespace properties. | guarantee | supported |
| LRN-003-C2 | Defaults keep evaluation total and prevent nullable propagation in flag call sites. | guarantee | supported |
| LRN-003-C3 | Rule and context type constraints are enforced through generic feature signatures. | guarantee | supported |
| LRN-003-C4 | Invalid JSON is rejected at the boundary while last-known-good state remains active. | failure_mode | supported |
