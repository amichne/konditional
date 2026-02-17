# OpenTelemetry Reference

This page lists the main `konditional-otel` entry points and the current
preferred call patterns.

## Telemetry installation

```kotlin
val telemetry = KonditionalTelemetry(
    otel = otel,
    tracingConfig = TracingConfig(
        samplingStrategy = SamplingStrategy.PARENT_BASED,
    ),
)

// Preferred usage: pass telemetry explicitly to evaluation calls.
val enabled = MyFlags.darkMode.evaluateWithTelemetry(
    context = context,
    telemetry = telemetry,
)

// Compatibility path for deprecated global-shim overloads.
KonditionalTelemetry.install(telemetry)
```

## Evaluation helpers

```kotlin
val enabled = MyFlags.darkMode.evaluateWithTelemetry(
    context = context,
    telemetry = telemetry,
)

val diagnostics = MyFlags.darkMode.evaluateWithTelemetryAndReason(
    context = context,
    telemetry = telemetry,
)

val enabledWithCurrentSpan = MyFlags.darkMode.evaluateWithAutoSpan(
    context = context,
    telemetry = telemetry,
)
```

---

<details>
<summary>Advanced Options</summary>

## Sampling strategies

```kotlin
SamplingStrategy.ALWAYS
SamplingStrategy.NEVER
SamplingStrategy.PARENT_BASED
SamplingStrategy.RATIO(10)
SamplingStrategy.FEATURE_FILTER { feature -> feature.namespace.id == "critical" }
```

## Registry hooks

```kotlin
val hooks = telemetry.toRegistryHooks()
```

## No-op testing

```kotlin
val telemetry = KonditionalTelemetry.noop()
```

</details>
