# How-to: handle configuration failures

Use this page to implement explicit failure policy at the configuration boundary
without changing evaluation semantics.

## Read this page when

- Remote configuration fetch or parsing can fail in your environment.
- You need deterministic fallback behavior during incidents.
- You want typed failure handling with clear operator actions.

## Failure policy matrix

| Failure type | Immediate behavior | Operator action |
| --- | --- | --- |
| Fetch timeout or transport error | Keep last-known-good snapshot active | Retry with backoff and alert |
| `ParseError.InvalidJson` | Reject payload | Fix producer payload, redeploy config |
| `ParseError.FeatureNotFound` | Reject payload | Align config keys with registered features |
| `ParseError.InvalidSnapshot` | Reject payload | Fix schema/type mismatch in payload |

## Deterministic steps

1. Treat snapshot loading as a typed boundary.

```kotlin
val result = NamespaceSnapshotLoader(AppFeatures).load(json)
```

2. Handle parse failures explicitly by error type.

```kotlin
if (result.isFailure) {
    when (val error = result.parseErrorOrNull()) {
        is ParseError.InvalidJson -> alert("invalid_json", error.reason)
        is ParseError.FeatureNotFound -> alert("feature_not_found", error.key)
        is ParseError.InvalidSnapshot -> alert("invalid_snapshot", error.reason)
        else -> alert("unknown_load_failure", result.exceptionOrNull()?.message ?: "n/a")
    }
}
```

3. Apply one mitigation path per failure class.

- Continue on last-known-good for parse or transport failures.
- Use `rollback(steps)` when a previously accepted snapshot causes incidents.
- Use `disableAll()` only as an explicit emergency control.

4. Emit observability signals that do not alter semantics.

- Structured logs: namespace, feature key, error type, timestamp.
- Metrics: load attempts, load failures by class, rollback count.
- Alerts: sustained failure rate and rollback usage.

5. Prove behavior with failure-injection tests.

- Invalid JSON fixture returns `ParseError.InvalidJson`.
- Unknown feature fixture returns `ParseError.FeatureNotFound`.
- Invalid schema fixture returns `ParseError.InvalidSnapshot`.
- Last-known-good behavior remains active after each failure.

## Verification checklist

- [ ] Each parse error type maps to one documented mitigation action.
- [ ] Failure handling never throws across module boundaries.
- [ ] Last-known-good behavior is preserved on rejected payloads.
- [ ] Rollback and kill-switch are tested before production use.
- [ ] Alerting captures sustained failure trends.

## Next steps

- [Safe remote config](/how-to-guides/safe-remote-config)
- [Failure modes](/production-operations/failure-modes)
- [Refresh patterns](/production-operations/refresh-patterns)
