---
title: Evaluation Diagnostics
sidebar_position: 6
---

# Evaluation Diagnostics

Reference model for evaluation diagnostics emitted through observability surfaces and shadow-evaluation mismatch callbacks.

## Diagnostic Core Fields

| Field | Meaning |
| --- | --- |
| `namespaceId` | Namespace where evaluation occurred. |
| `featureKey` | Evaluated feature identifier. |
| `configVersion` | Active configuration version metadata (if provided). |
| `mode` | Evaluation mode (`NORMAL`, `SHADOW`, explain/diagnostic modes). |
| `durationNanos` | Evaluation latency in nanoseconds. |
| `value` | Typed evaluated value returned by the engine. |
| `decision` | Decision category and associated match details. |

## Decision Categories

| Decision category | Meaning |
| --- | --- |
| Registry disabled | Namespace kill-switch forced default return. |
| Inactive | Feature-level inactive state forced default return. |
| Rule | One rule matched and produced value. |
| Default | No rule matched; declared default returned. |

## Shadow Mismatch Model

When using shadow evaluation, mismatch callbacks include:

- `featureKey`
- `baseline` diagnostics snapshot
- `candidate` diagnostics snapshot
- mismatch `kinds` (`VALUE`, `DECISION`)

## Operational Use

- Alert on mismatch rates during migrations.
- Correlate decision classes with rollout incidents.
- Track evaluation latency by namespace and feature.

## Next Steps

- [Migration from Legacy](/guides/migration-from-legacy)
- [Migration and Shadowing Theory](/theory/migration-and-shadowing)
