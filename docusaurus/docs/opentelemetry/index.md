# OpenTelemetry integration

Konditional OpenTelemetry integration adds tracing, metrics, and structured
logging without introducing OpenTelemetry dependencies into core modules.

## What this module gives you

The `konditional-otel` module is designed for production instrumentation with
clear control over overhead.

- **Distributed tracing**: create spans for feature evaluations with semantic
  attributes.
- **Metrics collection**: record evaluation metrics, including exemplar links
  to traces.
- **Structured logging**: emit logs with active trace context.
- **Configurable sampling**: choose `ALWAYS`, `NEVER`, `PARENT_BASED`,
  `RATIO`, or `FEATURE_FILTER`.
- **Low overhead defaults**: disabled or unsampled paths keep overhead minimal.
- **Thread-safe behavior**: supports concurrent evaluation flows.

## Installation

Add the dependency to your `build.gradle.kts` file:

```kotlin
dependencies {
    implementation("io.amichne:opentelemetry:VERSION")
}
```

## Quick start

Use this flow to connect Konditional evaluations to your OpenTelemetry SDK.

1. Build or obtain an `OpenTelemetry` SDK instance.
2. Create `KonditionalTelemetry` with a tracing strategy.
3. Pass telemetry explicitly at evaluation call sites.

```kotlin
val otel = OpenTelemetrySdk.builder()
    .setTracerProvider(tracerProvider)
    .setMeterProvider(meterProvider)
    .setLoggerProvider(loggerProvider)
    .build()

val telemetry = KonditionalTelemetry(
    otel = otel,
    tracingConfig = TracingConfig(
        samplingStrategy = SamplingStrategy.RATIO(10),
    ),
)

val enabled = MyFlags.darkMode.evaluateWithTelemetry(
    context = context,
    telemetry = telemetry,
)
```

If you are migrating from older call sites, global install remains available:

```kotlin
KonditionalTelemetry.install(telemetry)
```

## Sampling strategies

Sampling controls trace volume and instrumentation cost.

```kotlin
SamplingStrategy.ALWAYS
SamplingStrategy.NEVER
SamplingStrategy.PARENT_BASED
SamplingStrategy.RATIO(10)
SamplingStrategy.FEATURE_FILTER { feature ->
    feature.namespace.id == "critical-features"
}
```

- `ALWAYS`: sample every evaluation.
- `NEVER`: disable sampling.
- `PARENT_BASED`: inherit parent span sampling.
- `RATIO(percentage)`: deterministic percentage-based sampling.
- `FEATURE_FILTER`: sample only for matching feature predicates.

## Semantic conventions

Evaluation spans include stable attributes that you can query in traces,
dashboards, and logs.

| Attribute                         | Description                                                        |
|-----------------------------------|--------------------------------------------------------------------|
| `feature.namespace`               | Feature namespace ID                                               |
| `feature.key`                     | Feature key                                                        |
| `feature.type`                    | Value type (boolean, string, enum, and so on)                     |
| `evaluation.result.value`         | Evaluated value (sanitized)                                        |
| `evaluation.result.decision`      | Decision type (default, rule_matched, inactive, registry_disabled) |
| `evaluation.duration_ns`          | Evaluation duration in nanoseconds                                 |
| `evaluation.rule.note`            | Rule note when a rule matches                                      |
| `evaluation.rule.specificity`     | Rule specificity score                                             |
| `evaluation.bucket`               | Rollout bucket                                                     |
| `evaluation.ramp_up`              | Rollout percentage                                                 |
| `context.platform`                | Platform identifier                                                |
| `context.locale`                  | Locale identifier                                                  |
| `context.version`                 | App version                                                        |
| `context.stable_id.sha256_prefix` | Stable ID hash prefix (PII-safe)                                   |

## Advanced usage

Use these patterns when you need tighter integration with existing trace or
registry wiring.

### Propagate a parent span

```kotlin
val parentSpan = tracer.spanBuilder("checkout.process").startSpan()

parentSpan.makeCurrent().use {
    val enabled = MyFlags.feature.evaluateWithAutoSpan(
        context = context,
        telemetry = telemetry,
    )
}

parentSpan.end()
```

### Build registry hooks

```kotlin
val hooks = telemetry.toRegistryHooks()
```

### Disable tracing for tests

```kotlin
val telemetry = KonditionalTelemetry(
    otel = OpenTelemetry.noop(),
    tracingConfig = TracingConfig(enabled = false),
)
```

## Performance notes

Use sampling to keep cost predictable in production.

- Disabled or not sampled: less than 1 percent latency overhead.
- Sampled: typically around 5 to 10 percent overhead for span creation and
  attribute population.
- Metrics recording: usually around 1 to 2 microseconds per evaluation.

## Next steps

- [OpenTelemetry reference](/opentelemetry/reference)
- [Observability module](/observability/)
- [Core API reference](/core/reference)
