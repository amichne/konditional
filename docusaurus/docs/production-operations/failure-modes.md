# Failure modes

Use this page to classify runtime failure modes, apply safe containment, and
route teams to canonical remediation procedures.

## Read this page when

- A configuration operation fails in production.
- You need a predictable containment decision under time pressure.
- You need a post-incident checklist that preserves determinism.

## Failure mode matrix

| Failure mode | Primary signal | Safe immediate behavior | Canonical remediation |
| --- | --- | --- | --- |
| Remote fetch failure | Transport timeout, non-2xx, network error | Keep last-known-good snapshot | [Handling failures](/how-to-guides/handling-failures) |
| Invalid JSON payload | `ParseError.InvalidJson` | Reject payload, keep current snapshot | [Safe remote config](/how-to-guides/safe-remote-config) |
| Unknown feature key | `ParseError.FeatureNotFound` | Reject payload, keep current snapshot | [Namespace isolation](/how-to-guides/namespace-isolation) |
| Type/schema mismatch | `ParseError.InvalidSnapshot` | Reject payload, keep current snapshot | [Safe remote config](/how-to-guides/safe-remote-config) |
| Behavior regression after accepted load | Guardrail metrics degrade | Roll back to prior snapshot | [Operational debugging](/production-operations/debugging) |
| Cross-namespace blast radius | Unrelated namespace behavior changes | Halt rollout and isolate owner namespace | [Namespace isolation](/how-to-guides/namespace-isolation) |

## Deterministic steps

1. Classify the incident into one failure mode from the matrix.
2. Apply the documented safe immediate behavior.
3. Execute the linked canonical remediation procedure.
4. Verify stable output with deterministic replay contexts.
5. Record mitigation, root cause, and permanent test coverage.

## Operations checklist

- [ ] Failure mode classified with exact parse/load signal.
- [ ] Immediate behavior preserved last-known-good semantics.
- [ ] Canonical remediation procedure completed.
- [ ] Rollback or kill-switch decisions recorded.
- [ ] Regression tests added before closing incident.

## Next steps

- [Operational debugging](/production-operations/debugging)
- [Handling failures](/how-to-guides/handling-failures)
- [Troubleshooting](/troubleshooting/)
