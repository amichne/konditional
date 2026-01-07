# OpenTelemetry Reference

## Telemetry installation

```kotlin
val telemetry = KonditionalTelemetry(
    otel = otel,
    tracingConfig = TracingConfig(
        samplingStrategy = SamplingStrategy.PARENT_BASED,
    ),
)

KonditionalTelemetry.install(telemetry)
```

## Evaluation helpers

```kotlin
val enabled = MyFlags.darkMode.evaluateWithTelemetry(context)
```

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
AppFeatures.setHooks(hooks)
```
