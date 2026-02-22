# Shadow evaluation reference

This page defines the operational contract for shadow evaluation APIs without
repeating conceptual migration guidance.

## Read this page when

- You need exact behavior of baseline vs. candidate evaluation.
- You are deciding which mismatch kinds to report.
- You are integrating mismatch callbacks into an existing telemetry pipeline.

## API and contract reference

### Baseline-returning API

```kotlin
fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateWithShadow(
    context: C,
    candidateRegistry: NamespaceRegistry,
    baselineRegistry: NamespaceRegistry = namespace,
    options: ShadowOptions = ShadowOptions.defaults(),
    onMismatch: (ShadowMismatch<T>) -> Unit = {},
): T
```

Contract:

- Evaluates baseline with `EvaluationMode.NORMAL`.
- Returns baseline value to caller.
- Optionally evaluates candidate with `EvaluationMode.SHADOW`.
- Emits callback and warning log only when mismatch kinds are non-empty.

### Callback-only API

```kotlin
fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateShadow(
    context: C,
    candidateRegistry: NamespaceRegistry,
    baselineRegistry: NamespaceRegistry = namespace,
    options: ShadowOptions = ShadowOptions.defaults(),
    onMismatch: (ShadowMismatch<T>) -> Unit = {},
)
```

Contract:

- Executes the same mismatch pipeline as `evaluateWithShadow(...)`.
- Discards the baseline value from the call site.

### Options and mismatch kinds

- `ShadowOptions.defaults()`:
  - `reportDecisionMismatches = false`
  - `evaluateCandidateWhenBaselineDisabled = false`
- `ShadowMismatch.Kind.VALUE` is emitted when baseline and candidate values
  differ.
- `ShadowMismatch.Kind.DECISION` is emitted only when decision mismatch
  reporting is enabled and decision classes differ.

## Deterministic API and contract notes

- For fixed `(context, baselineRegistry, candidateRegistry, options)`, mismatch
  detection is deterministic.
- No mutation is performed on either registry by these APIs.
- Observability callbacks do not change evaluation semantics unless callback code
  introduces side effects externally.

## Canonical conceptual pages

- [Theory: Migration and shadowing](/theory/migration-and-shadowing)
- [Theory: Determinism proofs](/theory/determinism-proofs)
- [How-to: Handling failures](/how-to-guides/handling-failures)

## Next steps

- [Observability reference](/observability/reference)
- [OpenTelemetry reference](/opentelemetry/reference)
- [Feature evaluation API](/reference/api/feature-evaluation)
