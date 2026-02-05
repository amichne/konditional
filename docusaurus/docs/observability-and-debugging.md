---
title: Observability & Debugging
---

# Observability & Debugging

Operationally, feature flags are only as safe as your ability to answer:

- “Why did user X see behavior Y?”
- “Which rule matched?”
- “Are we in/out of rollout?”
- “What did the registry configuration look like at the time?”

Konditional’s core supports this via:

- `Feature.explain(context)` returning `EvaluationResult<T>`
- `RegistryHooks` for lightweight logging and metrics

## EvaluationResult anatomy

An `EvaluationResult<T>` includes:

- `value`: the evaluated value (`T`)
- `decision`: one of:
  - registry disabled (kill-switch)
  - inactive definition
  - matched rule (with optional “skipped by rollout” rule)
  - default (with optional “skipped by rollout” rule)
- `configVersion`: the version attached to the active configuration view
- `durationNanos`: measured duration for the evaluation

## Hot-path hooks

`NamespaceRegistry.hooks` exposes:

- `logger`: receives `explain` debug signals
- `metrics`: receives evaluation events (`NORMAL`, `EXPLAIN`, and internal modes like `SHADOW`)

**Rule of thumb:** keep hooks non-blocking and lightweight; they execute on the evaluation hot path.

Next:

- [Recipes](recipes) for shadow evaluation and config lifecycle patterns

