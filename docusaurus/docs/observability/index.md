# konditional-observability

`konditional-observability` adds shadow evaluation helpers and exposes observability hooks.

## When to Use This Module

You should use `konditional-observability` when you need to:

- Compare two configurations side-by-side with shadow evaluation to detect mismatches
- Add instrumentation without coupling to specific logging or metrics vendors
- Get explainable evaluation results for debugging and auditing
- Track evaluation performance and decision paths in production

## What You Get

- **Shadow evaluation**: Compare baseline and candidate configurations safely
- **Dependency-free hooks**: `KonditionalLogger` and `MetricsCollector` interfaces
- **Evaluation explanations**: `EvaluationResult` with decision traces and bucketing info
- **Mismatch detection**: Callbacks when shadow evaluation reveals differences

## Alternatives

Without this module, you would need to:

- Build custom shadow evaluation logic with careful isolation to avoid affecting production
- Tightly couple feature flag evaluation to specific logging/metrics libraries (vendor lock-in)
- Implement your own evaluation tracing and debugging utilities from scratch

## Installation

```kotlin
dependencies {
  implementation("io.amichne:konditional-observability:VERSION")
}
```

## Guarantees

- **Guarantee**: Shadow evaluation does not alter production behavior.

- **Mechanism**: The baseline registry is returned; the candidate registry is evaluated for comparison only.

- **Boundary**: Mismatch callbacks run inline on the evaluation thread and must stay lightweight.

## Next steps

- [Observability reference](/observability/reference)
- [Shadow evaluation patterns](/observability/shadow-evaluation)
- [OpenTelemetry integration](/opentelemetry/)
