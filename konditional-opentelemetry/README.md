# Konditional OpenTelemetry Integration

Production-ready OpenTelemetry integration for the Konditional feature flag library.

## Features

- **Distributed Tracing**: Create spans for feature flag evaluations with semantic attributes
- **Metrics Collection**: Record evaluation metrics with exemplar support linking metrics to traces
- **Structured Logging**: Emit structured log records with trace context propagation
- **Configurable Sampling**: Multiple sampling strategies to control overhead (ALWAYS, NEVER, PARENT_BASED, RATIO, FEATURE_FILTER)
- **Low Overhead**: <1% latency impact when disabled or not sampled; ~5-10% when sampled
- **Thread-Safe**: Concurrent evaluation support with proper context propagation

## Installation

Add the dependency to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.amichne:konditional-opentelemetry:$version")
}
```

## Quick Start

### 1. Setup OpenTelemetry SDK

```kotlin
val otel = OpenTelemetrySdk.builder()
    .setTracerProvider(tracerProvider)
    .setMeterProvider(meterProvider)
    .setLoggerProvider(loggerProvider)
    .build()
```

### 2. Configure Konditional Telemetry

```kotlin
val telemetry = KonditionalTelemetry(
    otel = otel,
    tracingConfig = TracingConfig(
        samplingStrategy = SamplingStrategy.RATIO(10) // 10% sampling
    )
)

// Install globally
KonditionalTelemetry.install(telemetry)
```

### 3. Use in Evaluations

```kotlin
// Before: standard evaluation
val enabled = MyFlags.darkMode.evaluate(context)

// After: evaluation with telemetry
val enabled = MyFlags.darkMode.evaluateWithTelemetry(context)
```

## Sampling Strategies

Control trace volume with flexible sampling:

```kotlin
// Sample all evaluations (high overhead, use for testing)
SamplingStrategy.ALWAYS

// Disable tracing entirely
SamplingStrategy.NEVER

// Sample only if parent span is sampled (recommended for production)
SamplingStrategy.PARENT_BASED

// Sample 10% of evaluations deterministically
SamplingStrategy.RATIO(10)

// Sample based on feature predicate
SamplingStrategy.FEATURE_FILTER { feature ->
    feature.namespace.id == "critical-features"
}
```

## Semantic Conventions

Spans include standardized attributes:

| Attribute | Description |
|-----------|-------------|
| `feature.namespace` | Feature namespace ID |
| `feature.key` | Feature key |
| `feature.type` | Value type (boolean, string, etc.) |
| `evaluation.result.value` | Evaluated value (sanitized) |
| `evaluation.result.decision` | Decision type (default, rule_matched, inactive, registry_disabled) |
| `evaluation.duration_ns` | Evaluation duration in nanoseconds |
| `evaluation.rule.note` | Rule note (if matched) |
| `evaluation.rule.specificity` | Rule specificity score |
| `evaluation.bucket` | Rollout bucket |
| `evaluation.ramp_up` | Rollout percentage |
| `context.platform` | Platform ID |
| `context.locale` | Locale ID |
| `context.version` | App version |
| `context.stable_id.sha256_prefix` | Stable ID hash prefix (PII-safe) |

## Advanced Usage

### Parent Span Propagation

```kotlin
val parentSpan = tracer.spanBuilder("checkout.process").startSpan()

parentSpan.makeCurrent().use {
    val enabled = MyFlags.feature.evaluateWithAutoSpan(context, telemetry)
}

parentSpan.end()
```

### Registry Hooks Integration

```kotlin
val telemetry = KonditionalTelemetry(otel)
val hooks = telemetry.toRegistryHooks()

// Use hooks when creating namespace registries
// to automatically instrument all evaluations
```

### Disabled Tracing for Testing

```kotlin
val telemetry = KonditionalTelemetry(
    otel = OpenTelemetry.noop(),
    tracingConfig = TracingConfig(enabled = false)
)
```

## Performance

- **Disabled/Not Sampled**: <1% latency overhead
- **Sampled**: ~5-10% overhead for span creation and attribute population
- **Metrics**: Minimal overhead (~1-2μs per evaluation)
- **Recommended**: Use `PARENT_BASED` or `RATIO(10)` sampling in production

## Architecture

### Module Structure

```
konditional-opentelemetry/
├── traces/
│   ├── FlagEvaluationTracer.kt       # Span creation and population
│   ├── SpanAttributes.kt             # Semantic conventions
│   └── TracingConfig.kt              # Sampling strategies
├── metrics/
│   ├── OtelMetricsCollector.kt       # MetricsCollector implementation
│   └── MetricsConfig.kt              # Cardinality controls
├── logging/
│   └── OtelLogger.kt                 # KonditionalLogger implementation
└── KonditionalTelemetry.kt           # Main facade

```

### Design Principles

1. **Opt-In**: Telemetry is entirely optional; core library has zero OpenTelemetry dependencies
2. **Low Overhead**: Sampling and conditional instrumentation minimize performance impact
3. **Type-Safe**: Leverages Kotlin's type system for compile-time safety
4. **Composable**: Separate concerns (tracing, metrics, logging) with clean interfaces

## License

Same as parent Konditional project.
