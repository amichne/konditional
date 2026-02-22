---
title: Troubleshooting
---

# Troubleshooting

Use this page to triage production symptoms quickly, then jump to one canonical
procedure page. This page does not duplicate implementation runbooks.

## Read this page when

- Runtime behavior differs from expected results.
- Snapshot loads fail or configuration looks stale.
- Rollout distribution or determinism looks wrong.

## Triage steps

1. Capture the symptom, namespace, feature key, stable ID, and timestamp.
2. Identify the symptom class from the routing table.
3. Follow the linked canonical procedure exactly.
4. Return here only if the symptom class changes.

## Symptom routing table

| Symptom | Primary canonical procedure | Secondary operational runbook |
| --- | --- | --- |
| Same user flips between rollout states | [Debugging determinism](/how-to-guides/debugging-determinism) | [Operational debugging](/production-operations/debugging) |
| `NamespaceSnapshotLoader.load(...)` fails | [Safe remote config](/how-to-guides/safe-remote-config) | [Failure modes](/production-operations/failure-modes) |
| Load succeeds but behavior appears stale | [Handling failures](/how-to-guides/handling-failures) | [Refresh patterns](/production-operations/refresh-patterns) |
| Returned value is unexpected | [Testing features](/how-to-guides/testing-features) | [Operational debugging](/production-operations/debugging) |
| Cross-team/config blast-radius concerns | [Namespace isolation](/how-to-guides/namespace-isolation) | [Thread safety](/production-operations/thread-safety) |

## Symptom shortcuts

### Bucketing issues

Go to [Bucketing issues (legacy bridge)](/troubleshooting/bucketing-issues).

### Parsing issues

Go to [Parsing issues (legacy bridge)](/troubleshooting/parsing-issues).

### Integration issues

Go to [Integration issues (legacy bridge)](/troubleshooting/integration-issues).

## Incident checklist

- [ ] Incident artifacts captured before making changes.
- [ ] Canonical procedure selected from the routing table.
- [ ] Mitigation applied (`rollback` or kill-switch) when required.
- [ ] Follow-up test case added for the observed failure path.

## Next steps

- [Operational debugging](/production-operations/debugging)
- [Failure modes](/production-operations/failure-modes)
- [Safe remote config](/how-to-guides/safe-remote-config)
