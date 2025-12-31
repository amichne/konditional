# Feature Operations

API reference for evaluating features and retrieving evaluation metadata.

---

## `Feature(context, registry): T`

Preferred evaluation API (operator overload).

```kotlin
operator fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.invoke(
    context: C,
    registry: NamespaceRegistry = namespace,
): T
```

### Parameters

- `context` — Runtime context providing locale, platform, version, stableId
- `registry` — Registry to evaluate against (defaults to the feature’s namespace)

### Returns

The feature's typed value (rule value or default). Never returns `null`.

### Throws

- `IllegalStateException` if the feature is not present in the provided registry (typically caused by evaluating
  before a namespace is initialized/registered, or evaluating against the wrong registry instance).

### Example

```kotlin
val enabled: Boolean = AppFeatures.darkMode(context)
val endpoint: String = AppFeatures.apiEndpoint(context)
```

### Behavior

1. Lookup the feature’s effective `FlagDefinition` in the registry.
2. If the registry kill-switch is enabled (`disableAll()`), return the declared default.
3. If the flag is inactive (`isActive=false` in configuration), return the declared default.
4. Iterate configured rules by descending specificity (stable sort).
5. For the first rule whose criteria match:
   - Compute a deterministic rollout bucket from `(stableId, feature.key, salt)`.
   - If allowlisted or in ramp-up, return the rule’s value.
   - Otherwise, continue to the next rule.
6. If no rule produces a value, return the declared default.

---

## `Feature.explain(context, registry): EvaluationResult<T>`

Explainable evaluation for debugging and observability.

```kotlin
fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.explain(
    context: C,
    registry: NamespaceRegistry = namespace,
): EvaluationResult<T>
```

### Parameters

- `context` — Runtime context
- `registry` — Registry to explain against (defaults to the feature’s namespace)

### Returns

`EvaluationResult<T>` capturing the returned value plus an explainable decision trace.

### Example

```kotlin
val result = AppFeatures.darkMode.explain(context)

when (val decision = result.decision) {
    EvaluationResult.Decision.RegistryDisabled -> println("Registry disabled via kill-switch")
    EvaluationResult.Decision.Inactive -> println("Flag is inactive")
    is EvaluationResult.Decision.Rule -> {
        println("Rule matched: ${decision.matched.rule.note}")
        println("Bucket: ${decision.matched.bucket.bucket}")
        decision.skippedByRollout?.let { skipped ->
            println("More specific rule matched by criteria but skipped by ramp-up: ${skipped.rule.note}")
        }
    }
    is EvaluationResult.Decision.Default -> {
        println("No rule produced a value, using declared default")
        decision.skippedByRollout?.let { skipped ->
            println("Most specific matching-by-criteria rule skipped by ramp-up: ${skipped.rule.note}")
        }
    }
}
```

### Use Cases

- Debugging user-specific outcomes
- Logging/metrics for observability
- Testing rule matching logic

---

## `Feature.evaluate(context, registry): T` (discouraged)

Verbose evaluation API.

This function is annotated with `@VerboseApi` (a Kotlin `@RequiresOptIn(level = ERROR)` marker) to discourage usage in
client code. Prefer the operator overload: `feature(context)`.

```kotlin
@VerboseApi
fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluate(
    context: C,
    registry: NamespaceRegistry = namespace,
): T
```

### Opt-in (if you really want it)

```kotlin
@OptIn(VerboseApi::class)
fun evaluateExplicitly() {
    val enabled = AppFeatures.darkMode.evaluate(context)
    // Prefer: val enabled = AppFeatures.darkMode(context)
}
```

### Why this exists

- Sometimes teams prefer explicit method calls for grep/searchability.
- The operator overload remains the supported default.

---

## Shadow Evaluation

Shadow evaluation compares a baseline registry (returned value) and a candidate registry (comparison only).

---

## `Feature.evaluateWithShadow(context, candidateRegistry, ...): T`

A/B test two configurations (baseline + candidate) and log mismatches.

```kotlin
fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateWithShadow(
    context: C,
    candidateRegistry: NamespaceRegistry,
    baselineRegistry: NamespaceRegistry = namespace,
    options: ShadowOptions = ShadowOptions.defaults(),
    onMismatch: (ShadowMismatch<T>) -> Unit = {}
): T
```

### Parameters

- `context` — Runtime context
- `candidateRegistry` — Registry evaluated for comparison only
- `baselineRegistry` — Registry whose value is returned (defaults to the feature’s namespace)
- `options` — Shadow evaluation options (see `ShadowOptions`)
- `onMismatch` — Callback invoked when baseline and candidate differ

### Returns

The **baseline** value (candidate is only evaluated for comparison).

### Behavior

1. Evaluate baseline registry (returned value)
2. Optionally skip candidate evaluation if baseline registry kill-switch is enabled (default behavior)
3. Evaluate candidate registry for comparison
4. If baseline and candidate differ, invoke `onMismatch` and emit a warning via `baselineRegistry` hooks
5. Return the baseline value

### Example

```kotlin
val _ = AppFeatures // ensure features are registered before parsing
val candidateConfig = SnapshotSerializer.fromJson(candidateJson).getOrThrow()
val candidateRegistry = NamespaceRegistry(configuration = candidateConfig, namespaceId = AppFeatures.id)

val value = AppFeatures.darkMode.evaluateWithShadow(
    context = context,
    candidateRegistry = candidateRegistry,
    onMismatch = { mismatch ->
        logger.warn(
            "Shadow mismatch key=${mismatch.featureKey} kinds=${mismatch.kinds} baseline=${mismatch.baseline.value} candidate=${mismatch.candidate.value}",
        )
    },
)
```

### Use Cases

- Validating configuration changes before rollout
- Canary testing new config logic
- Migration from one flag system to another

See [Advanced: Shadow Evaluation](/advanced/shadow-evaluation) for details.

---

## `Feature.evaluateShadow(context, candidateRegistry, ...): Unit`

Detect config drift between baseline and candidate without affecting production.

```kotlin
fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateShadow(
    context: C,
    candidateRegistry: NamespaceRegistry,
    baselineRegistry: NamespaceRegistry = namespace,
    options: ShadowOptions = ShadowOptions.defaults(),
    onMismatch: (ShadowMismatch<T>) -> Unit = {}
)
```

### Parameters

- `context` — Runtime context
- `candidateRegistry` — Registry evaluated for comparison only
- `baselineRegistry` — Registry treated as baseline (defaults to the feature’s namespace)
- `options` — Shadow evaluation options
- `onMismatch` — Callback invoked when baseline and candidate differ

### Example

```kotlin
AppFeatures.darkMode.evaluateShadow(
    context = context,
    candidateRegistry = candidateRegistry,
    onMismatch = { mismatch ->
        logger.error(
            "Config drift detected key=${mismatch.featureKey} kinds=${mismatch.kinds} baseline=${mismatch.baseline.value} candidate=${mismatch.candidate.value}",
        )
    },
)
```

---

## Next Steps

- [Namespace Operations](/api-reference/namespace-operations) — Load, rollback, kill-switch
- [Serialization](/api-reference/serialization) — JSON snapshot/patch operations
- [Observability](/api-reference/observability) — Hooks and explainability
