# konditional-observability

`konditional-observability` provides the public observability API surface for
shadow evaluation, logging hooks, and metrics hooks.

## Read this page when

- You need module-level observability entrypoints.
- You are wiring mismatch reporting without changing baseline behavior.
- You need hook contracts before integrating logging or metrics backends.

## API and contract surface

- Artifact: `io.amichne:konditional-observability:VERSION`
- Shadow APIs:
  - `Feature.evaluateWithShadow(...)`
  - `Feature.evaluateShadow(...)`
- Shadow config type: `ShadowOptions`
- Mismatch payload: `ShadowMismatch<T>`
- Hook surface: `RegistryHooks`, `KonditionalLogger`, `MetricsCollector`

## Deterministic API and contract notes

- Baseline evaluation value is always returned by `evaluateWithShadow(...)`.
- Candidate evaluation has no behavior side effects unless your callback does
  side effects.
- Hook callbacks run inline on the evaluation path; keep implementations small
  and deterministic.

## Canonical conceptual pages

- [Theory: Migration and shadowing](/theory/migration-and-shadowing)
- [Theory: Atomicity guarantees](/theory/atomicity-guarantees)
- [How-to: Handling failures](/how-to-guides/handling-failures)

## Next steps

- [Observability reference](/observability/reference)
- [Shadow evaluation reference](/observability/shadow-evaluation)
- [OpenTelemetry integration reference](/opentelemetry)
