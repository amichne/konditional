# OpenTelemetry integration

`konditional-opentelemetry` is the instrumentation module for trace, metric,
and log signals around feature evaluation.

## Read this page when

- You need module-level OpenTelemetry integration entrypoints.
- You want explicit telemetry injection without global state.
- You are choosing sampling policy before production rollout.

## API and contract surface

- Artifact: `io.amichne:konditional-opentelemetry:VERSION`
- Telemetry facade: `KonditionalTelemetry`
- Evaluation extensions:
  - `evaluateWithTelemetry(...)`
  - `evaluateWithTelemetryAndReason(...)`
  - `evaluateWithAutoSpan(...)`
- Tracing controls: `TracingConfig`, `SamplingStrategy`
- Hook bridge: `KonditionalTelemetry.toRegistryHooks()`

## Deterministic API and contract notes

- Explicit telemetry injection keeps evaluation deterministic for a fixed
  telemetry configuration.
- `SamplingStrategy.RATIO(percentage)` is deterministic for a fixed feature and
  context identity.
- Global singleton installation (`KonditionalTelemetry.install`) exists for
  compatibility, but explicit telemetry parameters are the stable contract.

## Canonical conceptual pages

- [Theory: Determinism proofs](/theory/determinism-proofs)
- [Theory: Atomicity guarantees](/theory/atomicity-guarantees)
- [How-to: Debugging determinism](/how-to-guides/debugging-determinism)

## Next steps

- [OpenTelemetry reference](/opentelemetry/reference)
- [Observability reference](/observability/reference)
- [Feature evaluation API](/reference/api/feature-evaluation)
