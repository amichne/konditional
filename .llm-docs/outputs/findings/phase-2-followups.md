# Phase 2 Followups (Non-Urgent)

## 0. Kill switch bypass via `FlagDefinition.evaluate`

`NamespaceRegistry.disableAll()` is enforced on the primary public evaluation path (`Feature.evaluate` / `feature {}`).
If a consumer retrieves a `FlagDefinition` and calls `FlagDefinition.evaluate(context)` directly, that bypasses the kill switch.

Potential fix (breaking but safer): add `NamespaceRegistry.evaluate(feature, context)` as the only supported evaluation API and/or make `FlagDefinition.evaluate` internal.

## 1. Test runner ergonomics in sandboxed environments

In environments where writing to `~/.gradle` is restricted, Gradle invocations that trigger wrapper locking/downloading can fail.
Workarounds used during this phase:
- Prefer `./gradlew test` (no `--tests` filtering) when wrapper artifacts already exist.
- If a failing test needs identification, inspect `build/test-results/test/TEST-*.xml` for `<failure>` entries.

## 2. Rollback metrics on failure

`InMemoryNamespaceRegistry.rollback(steps)` currently emits `ConfigRollbackMetric` only for successful rollbacks.
If you want failure visibility, emit a failure metric before returning `false`.

