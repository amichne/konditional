# Feature Operations

API reference for evaluating features and retrieving evaluation metadata.

---

## `Feature.evaluate(context): T`

Simple evaluation returning the typed value.

```kotlin
fun <T : Any, C : Context> Feature<T, C, *>.evaluate(context: C): T
```

### Parameters

- `context` — Runtime context providing locale, platform, version, stableId

### Returns

The feature's typed value (rule value or default). Never returns `null`.

### Example

```kotlin
val enabled: Boolean = AppFeatures.darkMode.evaluate(context)
val endpoint: String = AppFeatures.apiEndpoint.evaluate(context)
```

### Behavior

1. Registry lookup (O(1))
2. Check if namespace is disabled via `disableAll()` → return default
3. Check if flag is inactive → return default
4. Iterate rules by descending specificity
5. For each rule:
   - Check if all criteria match (AND semantics)
   - If match, check ramp-up bucket (or allowlist bypass)
   - If in ramp-up, return rule value
6. If no rules match, return default

---

## `Feature.explain(context): EvaluationResult<T>`

Explainable evaluation for debugging and observability.

```kotlin
fun <T : Any, C : Context> Feature<T, C, *>.explain(
    context: C
): EvaluationResult<T>
```

### Parameters

- `context` — Runtime context

### Returns

`EvaluationResult<T>` containing:
- `value: T` — The evaluated value
- `decision: EvaluationDecision` — Why this value was chosen (RULE_MATCHED, DEFAULT, INACTIVE, DISABLED)
- `matchedRule: RuleInfo?` — Details of the matched rule (if applicable)
- `bucketInfo: BucketInfo?` — Ramp-up bucket details (if applicable)

### Example

```kotlin
val result = AppFeatures.darkMode.explain(context)

when (result.decision) {
    EvaluationDecision.RULE_MATCHED -> {
        println("Rule matched: ${result.matchedRule?.note}")
        println("Bucket: ${result.bucketInfo?.bucket}")
    }
    EvaluationDecision.DEFAULT -> {
        println("No rules matched, using default")
    }
    EvaluationDecision.INACTIVE -> {
        println("Flag is inactive")
    }
    EvaluationDecision.DISABLED -> {
        println("Namespace disabled via kill-switch")
    }
}
```

### Use Cases

- Debugging user-specific outcomes
- Logging/metrics for observability
- Testing rule matching logic

### Alternative: Invoke Operator

For concise evaluation syntax:

```kotlin
// Using invoke operator
val enabled = AppFeatures.darkMode(context)

// Equivalent to
val enabled = AppFeatures.darkMode.evaluate(context)
```

---

## `Feature.evaluateWithShadow(context, shadowOptions): T`

A/B test two configurations (primary + shadow) and log mismatches.

```kotlin
fun <T : Any, C : Context> Feature<T, C, *>.evaluateWithShadow(
    context: C,
    shadowOptions: ShadowEvaluationOptions
): T
```

### Parameters

- `context` — Runtime context
- `shadowOptions` — Shadow configuration (registry or config snapshot)

### Returns

The **primary** value (shadow is only evaluated for comparison).

### Behavior

1. Evaluate against primary registry (returned value)
2. Evaluate against shadow registry
3. If values differ, invoke `shadowOptions.onMismatch` callback
4. Return primary value

### Example

```kotlin
val shadowConfig = SnapshotSerializer.fromJson(candidateJson).value

val value = AppFeatures.darkMode.evaluateWithShadow(
    context = context,
    shadowOptions = ShadowEvaluationOptions(
        shadowRegistry = shadowRegistry,
        onMismatch = { mismatch ->
            logger.warn("Shadow mismatch: primary=${mismatch.primary}, shadow=${mismatch.shadow}")
        }
    )
)
```

### Use Cases

- Validating configuration changes before rollout
- Canary testing new config logic
- Migration from one flag system to another

See [Advanced: Shadow Evaluation](/advanced/shadow-evaluation) for details.

---

## `Feature.evaluateShadow(context, shadowOptions): ShadowMismatch<T>?`

Detect config drift between primary and shadow without affecting production.

```kotlin
fun <T : Any, C : Context> Feature<T, C, *>.evaluateShadow(
    context: C,
    shadowOptions: ShadowEvaluationOptions
): ShadowMismatch<T>?
```

### Parameters

- `context` — Runtime context
- `shadowOptions` — Shadow configuration

### Returns

- `null` if primary and shadow agree
- `ShadowMismatch<T>` if values differ (contains `primary`, `shadow`, `context`)

### Example

```kotlin
val mismatch = AppFeatures.darkMode.evaluateShadow(context, shadowOptions)

if (mismatch != null) {
    logger.error("Config drift detected: primary=${mismatch.primary}, shadow=${mismatch.shadow}")
}
```

---

## `Feature.key: FeatureKey`

Get the feature's stable key.

```kotlin
val Feature<*, *, *>.key: FeatureKey
```

### Returns

`FeatureKey` (typically `"feature::{namespaceId}::{propertyName}"`)

### Example

```kotlin
val key = AppFeatures.darkMode.key
println(key)  // "feature::app::darkMode"
```

---

## Next Steps

- [Namespace Operations](/api-reference/namespace-operations) — Load, rollback, kill-switch
- [Serialization](/api-reference/serialization) — JSON snapshot/patch operations
- [Observability](/api-reference/observability) — Hooks and explainability
