# Thread safety

Use this page to validate atomic snapshot guarantees so concurrent readers never
observe partial configuration updates.

## Read this page when

- You are operating concurrent evaluation and refresh workloads.
- You need confidence in linearizable snapshot replacement.
- You are reviewing concurrency risk in rollout incidents.

## Core guarantee

Konditional snapshot updates are atomic: each evaluation observes either the
old snapshot or the new snapshot, never a partial merge.

## Deterministic steps

1. Treat loaded configurations as immutable snapshots.

Never mutate active configuration objects in place.

2. Activate updates only through the public load boundary.

```kotlin
val decoded = ConfigurationSnapshotCodec.decode(json, AppFeatures.compiledSchema())
if (decoded.isSuccess) {
    AppFeatures.load(decoded.getOrThrow())
}
```

3. Verify concurrent behavior with a smoke test.

```kotlin
// Pseudocode: one writer, many readers
repeat(1_000) {
    writerThread.load(nextSnapshot())
    readerThreads.forEach { read -> assertTrue(read() in allowedStates) }
}
```

4. Validate rollback uses the same atomic swap semantics.

Ensure `rollback(steps)` is tested under concurrent read load.

5. Keep observability side-effect free.

Hooks and metrics must record events without mutating evaluation state.

## Concurrency checklist

- [ ] No in-place mutation of active snapshot state.
- [ ] All updates go through parse-and-load boundary APIs.
- [ ] Concurrent readers observe only valid whole snapshots.
- [ ] Rollback behavior is verified under concurrent traffic.
- [ ] Observability hooks do not alter evaluation outputs.

## Next steps

- [Theory: atomicity guarantees](/theory/atomicity-guarantees)
- [Refresh patterns](/production-operations/refresh-patterns)
- [Operational debugging](/production-operations/debugging)
