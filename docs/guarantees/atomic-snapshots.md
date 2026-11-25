# Atomic snapshots

An evaluation uses one snapshot for the flag definition, kill switch, and diagnostic version.
Feature calls inside a deferred rule reuse that snapshot when they belong to the same namespace.
Calls into a different namespace use that namespace's registry.

The default registry serializes writes and stores the current snapshot in an `AtomicReference`. A concurrent load, rollback, or kill-switch change affects subsequent snapshot reads.

`Configuration` copies the supplied map of flag entries. A later change to the caller's map leaves both the active configuration and rollback history intact.

Custom `NamespaceRegistry` implementations must supply `currentSnapshot`. Return the configuration and kill-switch state from one atomic read or one critical section.

See [the concurrent reload test](https://github.com/amichne/konditional/blob/main/konditional-engine/src/test/kotlin/io/amichne/konditional/engine/EvaluationSnapshotTest.kt) for a reload during a deferred rule.
