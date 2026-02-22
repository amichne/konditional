# Runtime lifecycle

This page is the runtime operator runbook for loading and recovering namespace
snapshots.

## Read this page when

- You are implementing production config refresh loops.
- You are defining failure handling for invalid payloads.
- You are documenting rollback and emergency procedures.

## Steps in scope

1. Parse untrusted payload JSON into a typed `Result`.
2. On success, load the materialized snapshot into the namespace.
3. On failure, log typed parse errors and keep last-known-good state.
4. Continue evaluation against the currently active snapshot.
5. Use rollback if a newly loaded snapshot causes issues.

## Reference flow

```kotlin
val result = NamespaceSnapshotLoader(AppFeatures).load(json)
result
    .onSuccess { logger.info("config loaded") }
    .onFailure { failure ->
        val parseError = result.parseErrorOrNull()
        logger.warn(parseError?.message ?: failure.message.orEmpty())
    }
```

## Related pages

- [Runtime operations](/runtime/operations)
- [Configuration lifecycle (learn)](/learn/configuration-lifecycle)
- [Serialization reference](/serialization/reference)
- [Atomicity guarantees](/theory/atomicity-guarantees)

## Next steps

1. Add rollback playbooks from [Runtime operations](/runtime/operations).
2. Add parse-failure alerts using [Parse don’t validate](/theory/parse-dont-validate).
3. Validate deterministic outcomes with [Evaluation model](/learn/evaluation-model).
