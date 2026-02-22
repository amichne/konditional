# Configuration refresh patterns

Use this page to choose and operate a refresh strategy that preserves atomic
updates and deterministic evaluation.

## Read this page when

- You are selecting a refresh architecture for production.
- You need operational controls for update cadence and failure handling.
- You need a common checklist for polling, push, or manual refresh.

## Strategy selection matrix

| Pattern | Best for | Tradeoff |
| --- | --- | --- |
| Manual refresh | Controlled rollouts and admin-triggered updates | Slow propagation |
| Polling | Simple distributed deployment with eventual consistency | Extra fetch load |
| Webhook/push | Low-latency propagation | More auth and retry complexity |
| Local file watch | Developer and CI workflows | Not for production scale |

## Deterministic steps

1. Choose one refresh pattern per environment.

Do not mix patterns in the same environment unless you have explicit priority
rules.

2. Standardize the refresh contract.

```kotlin
fetch payload -> parse typed result -> atomic load on success -> keep last-known-good on failure
```

3. Implement resilience controls.

- Retry with exponential backoff.
- Add jitter for polling fleets.
- Enforce request timeout and payload size limits.

4. Add observability that does not change semantics.

- Metrics: fetch latency, load success rate, parse failure classes.
- Logs: namespace, payload version/hash, decision outcome.
- Alerts: sustained refresh failure or stale version age.

5. Rehearse rollback.

Run periodic drills for `rollback(...)` and emergency `disableAll()`.

## Operational checklist

- [ ] Refresh pattern selected and documented per environment.
- [ ] Fetch/parse/load contract implemented consistently.
- [ ] Backoff, jitter, and timeout controls configured.
- [ ] Metrics and alerts verified for refresh failure scenarios.
- [ ] Rollback drill completed and timestamped.

## Next steps

- [Safe remote config](/how-to-guides/safe-remote-config)
- [Thread safety](/production-operations/thread-safety)
- [Failure modes](/production-operations/failure-modes)
