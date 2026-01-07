# opentelemetry

OpenTelemetry integration for Konditional: tracing, metrics, and structured logging.

## When to Use This Module

You should use `opentelemetry` when you need to:

- Integrate feature flag evaluation into your existing OpenTelemetry instrumentation
- Trace evaluation decisions with distributed tracing context
- Collect metrics on feature flag performance with standard OpenTelemetry conventions
- Control sampling rates to balance observability with performance overhead

## What You Get

- **Automatic tracing**: Feature evaluations create spans with rich semantic attributes
- **Configurable sampling**: Fine-grained control over which evaluations are traced
- **Standard conventions**: Spans follow OpenTelemetry semantic conventions for consistency
- **Zero core dependency**: OpenTelemetry is completely optional and lives in a separate module

## Alternatives

Without this module, you would need to:

- Manually instrument every feature flag evaluation with custom tracing code
- Define your own span attribute conventions instead of following standards
- Integrate logging and metrics hooks individually using the observability module

## Installation

```kotlin
dependencies {
  implementation("io.amichne:opentelemetry:VERSION")
}
```

## Guarantees

- **Guarantee**: Core functionality does not depend on OpenTelemetry.

- **Mechanism**: Telemetry lives in a separate module with optional hooks and extensions.

- **Boundary**: Instrumentation overhead depends on sampling and your OpenTelemetry configuration.

## Quick start

```kotlin
val otel = OpenTelemetrySdk.builder()
    .setTracerProvider(tracerProvider)
    .setMeterProvider(meterProvider)
    .setLoggerProvider(loggerProvider)
    .build()

val telemetry = KonditionalTelemetry(
    otel = otel,
    tracingConfig = TracingConfig(
        samplingStrategy = SamplingStrategy.RATIO(10) // 10% sampling
    ),
)

KonditionalTelemetry.install(telemetry)

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

## Semantic conventions

Spans include attributes such as:

- `feature.namespace`
- `feature.key`
- `feature.type`
- `evaluation.result.value`
- `evaluation.result.decision`
- `evaluation.duration_ns`
- `evaluation.rule.specificity`
- `evaluation.bucket`
- `evaluation.ramp_up`
- `context.platform`
- `context.locale`
- `context.version`
- `context.stable_id.sha256_prefix`

## Performance notes

- Disabled or not sampled: near-zero overhead
- Sampled: additional overhead for span creation and attribute population

## Next steps

- [Observability module](/observability/)
- [Core API reference](/core/reference)
