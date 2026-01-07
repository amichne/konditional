# konditional-observability

`konditional-observability` adds shadow evaluation helpers and exposes observability hooks.

Use this module when you need mismatch detection, explainable evaluation at scale, or dependency-free hooks for logging
and metrics.

## Installation

```kotlin
dependencies {
    implementation("io.amichne:konditional-observability:VERSION")
}
```

## Guarantees

**Guarantee**: Shadow evaluation does not alter production behavior.

**Mechanism**: The baseline registry is returned; the candidate registry is evaluated for comparison only.

**Boundary**: Mismatch callbacks run inline on the evaluation thread and must stay lightweight.

## Next steps

- [Observability reference](/observability/reference)
- [Shadow evaluation patterns](/observability/shadow-evaluation)
- [OpenTelemetry integration](/opentelemetry/index)
