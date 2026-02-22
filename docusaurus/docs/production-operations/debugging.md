# Operational debugging

Use this page as the incident-debugging control plane. It routes symptoms to one
canonical diagnostic procedure and then returns to mitigation and recovery.

## Read this page when

- A production rollout behaves unexpectedly.
- Feature evaluation results diverge from expectations.
- Refresh or parsing incidents require immediate containment.

## Symptom routing

| Symptom class | Canonical diagnostic procedure | Containment companion |
| --- | --- | --- |
| Non-deterministic assignment | [Debugging determinism](/how-to-guides/debugging-determinism) | [Thread safety](/production-operations/thread-safety) |
| Snapshot parse failures | [Safe remote config](/how-to-guides/safe-remote-config) | [Failure modes](/production-operations/failure-modes) |
| Post-load stale behavior | [Handling failures](/how-to-guides/handling-failures) | [Refresh patterns](/production-operations/refresh-patterns) |
| Unexpected evaluated value | [Testing features](/how-to-guides/testing-features) | [Troubleshooting](/troubleshooting/) |

## Deterministic steps

1. Capture an incident fingerprint.

Record namespace, feature key, stable ID, payload version/hash, and first
failure timestamp.

2. Freeze change velocity.

Pause rollout changes while triage runs. Do not change multiple variables at
once.

3. Execute the canonical diagnostic procedure from the routing table.

Follow exactly one primary procedure until you have a clear root cause.

4. Apply containment.

- `rollback(steps)` when a recent snapshot introduced regression.
- `disableAll()` only for declared emergency fallback.
- Keep baseline semantics authoritative during shadow analysis.

5. Verify recovery with fixed contexts.

Use deterministic contexts to confirm behavior is stable after mitigation.

## Incident checklist

- [ ] Incident fingerprint captured before remediation.
- [ ] One canonical diagnostic route selected and completed.
- [ ] Containment action recorded with timestamp and owner.
- [ ] Recovery verified with deterministic replay.
- [ ] Follow-up test added for the root-cause path.

## Next steps

- [Failure modes](/production-operations/failure-modes)
- [Refresh patterns](/production-operations/refresh-patterns)
- [Troubleshooting](/troubleshooting/)
