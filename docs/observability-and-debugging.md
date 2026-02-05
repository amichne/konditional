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

:::note Debugging toolkit
Use `explain` for one-off diagnostics, `RampUpBucketing` for deterministic bucketing checks, and registry configuration
inspection for “what is actually loaded?”
:::

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

## Explain in practice

```kotlin
val result = AppFeatures.checkoutVariant.explain(context)
when (val decision = result.decision) {
    is EvaluationResult.Decision.Rule -> decision.matched
    is EvaluationResult.Decision.Default -> decision.skippedByRollout
    else -> null
}
```

:::tip When to use explain
Use it when a user reports unexpected behavior, when you need to verify rule precedence, or when you want to capture
bucket metadata in logs for a debugging session.
:::

## Bucketing checks

```kotlin
val info = RampUpBucketing.explain(
    stableId = StableId.of("user-123"),
    featureKey = AppFeatures.newCheckout.key,
    salt = "v1",
    rampUp = RampUp.of(10.0),
)
```

## Configuration inspection

```kotlin
val snapshot = AppFeatures.configuration
val definition = AppFeatures.flag(AppFeatures.newCheckout)
```

## Hot-path hooks

`NamespaceRegistry.hooks` exposes:

- `logger`: receives `explain` debug signals
- `metrics`: receives evaluation events (`NORMAL`, `EXPLAIN`, and internal modes like `SHADOW`)

:::caution Hot-path warning
Hooks execute on the evaluation hot path. Keep them non‑blocking and lightweight.
:::

## Shadow evaluation (observability module)

If you need to compare a candidate configuration against production behavior, use shadow evaluation from
`:konditional-observability`. It evaluates both registries but returns the baseline value.

```kotlin
val value = AppFeatures.darkMode.evaluateWithShadow(
    context = context,
    candidateRegistry = candidateRegistry,
    onMismatch = { mismatch ->
        logger.warn("shadowMismatch key=${mismatch.featureKey} kinds=${mismatch.kinds}")
    },
)
```

:::caution Sampling recommended
Shadow evaluation adds extra work on the hot path. Sample a percentage of requests for large traffic.
:::

Next:

- [Recipes](/recipes) for shadow evaluation and config lifecycle patterns
