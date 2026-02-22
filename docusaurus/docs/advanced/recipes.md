---
title: Advanced recipes
---

# Advanced recipes

Use this page as the advanced routing map. It keeps recipe-level intent in one
place and sends you to the single canonical procedure for each operation.

## Read this page when

- You know the outcome you need, but not the exact implementation guide.
- You want to avoid duplicate or conflicting operational instructions.
- You are designing an advanced rollout, migration, or isolation plan.

## Recipe routing matrix

| Goal | Canonical implementation procedure | Production companion |
| --- | --- | --- |
| Gradual boolean rollout | [Roll out gradually](/how-to-guides/rolling-out-gradually) | [Refresh patterns](/production-operations/refresh-patterns) |
| Multi-variant experiment | [A/B testing](/how-to-guides/ab-testing) | [Operational debugging](/production-operations/debugging) |
| Remote configuration boundary | [Safe remote config](/how-to-guides/safe-remote-config) | [Failure modes](/production-operations/failure-modes) |
| Domain-specific targeting | [Custom business logic](/how-to-guides/custom-business-logic) | [Thread safety](/production-operations/thread-safety) |
| Team and domain blast-radius control | [Namespace isolation](/how-to-guides/namespace-isolation) | [Failure modes](/production-operations/failure-modes) |
| Determinism investigation | [Debugging determinism](/how-to-guides/debugging-determinism) | [Operational debugging](/production-operations/debugging) |
| Local integration harness | [Local HTTP server container](/how-to-guides/local-http-server-container) | [Refresh patterns](/production-operations/refresh-patterns) |

## Deterministic steps

1. Choose one row in the routing matrix as the primary objective.
2. Complete the linked implementation procedure in full.
3. Complete the linked production companion procedure before rollout.
4. Record evidence for both procedures in your release notes.

## Completion checklist

- [ ] One canonical implementation guide has been completed.
- [ ] One production companion runbook has been completed.
- [ ] No local copy of the procedure was created in another page.
- [ ] Links in your team docs point to canonical pages, not duplicated text.

## Next steps

- [Golden path example](/examples/golden-path)
- [Troubleshooting index](/troubleshooting/)
- [Shadow evaluation](/observability/shadow-evaluation)
