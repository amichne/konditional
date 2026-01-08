# Migration and Shadowing

How `evaluateWithShadow` enables safe comparisons between two Konditional configurations.

---

## The Migration Problem

When updating configuration or migrating between flag systems, you need confidence that the new system produces the same
results as the old system.

**Traditional approach:**

1. Deploy new config
2. Hope it works
3. Monitor for issues
4. Rollback if problems detected

**Issues:**

- No advance warning of mismatches
- Production is the first test
- Rollback is reactive (damage may already be done)

---

## Shadow Evaluation

**Shadow evaluation** means: evaluate against two configurations (baseline + candidate) and compare results without
affecting production.

```kotlin
val baselineValue = feature.evaluate(context)  // Returned to caller

val candidateValue = evaluateAgainstCandidateConfig(feature, context)  // For comparison only

if (baselineValue != candidateValue) {
    logMismatch(feature, context, baselineValue, candidateValue)
}

return baselineValue  // Production uses baseline
```

**Key insight:** Production behavior is unchanged (baseline value is returned), but mismatches are logged for analysis.

---

## Konditional's Shadow API

### `evaluateWithShadow(context, candidateRegistry, ...): T`

Evaluate against baseline (returned) and candidate (comparison only):

```kotlin
val _ = AppFeatures // ensure features are registered before parsing
val candidateConfig = ConfigurationSnapshotCodec.decode(candidateJson).getOrThrow()
val candidateRegistry = InMemoryNamespaceRegistry(namespaceId = AppFeatures.namespaceId).apply {
    load(candidateConfig)
}

val value = AppFeatures.darkMode.evaluateWithShadow(
    context = context,
    candidateRegistry = candidateRegistry,
    onMismatch = { mismatch ->
        logger.warn(
            "shadowMismatch key=${mismatch.featureKey} kinds=${mismatch.kinds} baseline=${mismatch.baseline.value} candidate=${mismatch.candidate.value} stableId=${context.stableId.id}",
        )
    },
)

// value is from the baseline registry (production unchanged)
applyDarkMode(value)
```

**Behavior:**

1. Evaluate against baseline registry -> `baselineValue` (returned)
2. Evaluate against candidate registry -> `candidateValue` (comparison only)
3. If they differ, invoke the `onMismatch` callback
4. Return `baselineValue` (production unaffected)

---

## Use Case 1: Configuration Changes

You want to update ramp-up percentages or targeting criteria. Before rolling out, validate that the new config produces
expected results.

### Example: Increasing Ramp-Up

**Current config:**

```kotlin
val newFeature by boolean<Context>(default = false) {
    rule(true) { rampUp { 10.0 } }  // 10% rollout
}
```

**Candidate config (JSON):**

```json
{
  "flags": [
    {
      "key": "feature::app::newFeature",
      "defaultValue": { "type": "BOOLEAN", "value": false },
      "rules": [
        { "value": { "type": "BOOLEAN", "value": true }, "rampUp": 25.0 }
      ]
    }
  ]
}
```

**Shadow evaluation:**

```kotlin
val _ = AppFeatures // ensure features are registered before parsing
val candidateConfig = ConfigurationSnapshotCodec.decode(candidateJson).getOrThrow()
val candidateRegistry = InMemoryNamespaceRegistry(namespaceId = AppFeatures.namespaceId).apply {
    load(candidateConfig)
}

// Evaluate sample of users
users.forEach { user ->
    val ctx = buildContext(user)

    AppFeatures.newFeature.evaluateShadow(
        context = ctx,
        candidateRegistry = candidateRegistry,
        onMismatch = { mismatch ->
            logger.info(
                "User ${user.id}: baseline=${mismatch.baseline.value} candidate=${mismatch.candidate.value} kinds=${mismatch.kinds}",
            )
        },
    )
}
```

**Analysis:**

- Users with `baseline=false, candidate=true` -> will be newly enabled by the candidate config
- Verify this matches the expected 15% increase (10% -> 25%)

---

## Use Case 2: Migration Between Flag Systems

You're migrating from another flag system to Konditional. You want to verify that Konditional produces the same results
as the old system.

### Migration Flow

1. **Define flags in Konditional** (statically)
2. **Translate "old system" state into a baseline snapshot** (via JSON)
3. **Build a candidate snapshot** (the new desired behavior)
4. **Log mismatches**, investigate differences
5. **Once confident**, promote the candidate snapshot
6. **Monitor for regressions**
7. **Decommission old system**

### Example

```kotlin
// If you can translate the "old system" state into a Konditional snapshot, you can compare
// two registries side-by-side without changing production behavior:
val _ = AppFeatures // ensure features are registered before parsing
val baselineConfig = ConfigurationSnapshotCodec.decode(baselineJson).getOrThrow()
val candidateConfig = ConfigurationSnapshotCodec.decode(candidateJson).getOrThrow()

val baselineRegistry = InMemoryNamespaceRegistry(namespaceId = AppFeatures.namespaceId).apply {
    load(baselineConfig)
}
val candidateRegistry = InMemoryNamespaceRegistry(namespaceId = AppFeatures.namespaceId).apply {
    load(candidateConfig)
}

val value = AppFeatures.darkMode.evaluateWithShadow(
    context = context,
    candidateRegistry = candidateRegistry,
    baselineRegistry = baselineRegistry,
    onMismatch = { m ->
        logger.error("Migration mismatch baseline=${m.baseline.value} candidate=${m.candidate.value} kinds=${m.kinds}")
    },
)

applyDarkMode(value)
```

**Progression:**

- **Phase 1:** Baseline vs candidate comparison (log mismatches)
- **Phase 2:** Candidate becomes baseline (optional continued shadowing)
- **Phase 3:** Decommission old system

---

## Mechanism: Dual Evaluation

### Implementation (Simplified)

```kotlin
fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateWithShadow(
    context: C,
    candidateRegistry: NamespaceRegistry,
    baselineRegistry: NamespaceRegistry = namespace,
    options: ShadowOptions = ShadowOptions.defaults(),
    onMismatch: (ShadowMismatch<T>) -> Unit,
): T {
    val baseline = explain(context, baselineRegistry) // EvaluationResult<T>

    if (baselineRegistry.isAllDisabled && !options.evaluateCandidateWhenBaselineDisabled) {
        return baseline.value
    }

    val candidate = explain(context, candidateRegistry) // EvaluationResult<T>
    if (baseline.value != candidate.value) {
        onMismatch(
            ShadowMismatch(
                featureKey = key,
                baseline = baseline,
                candidate = candidate,
                kinds = setOf(ShadowMismatch.Kind.VALUE),
            ),
        )
    }

    return baseline.value
}
```

**Guarantees:**

1. Baseline value is returned (production behavior unchanged)
2. Candidate evaluation does not affect the returned result
3. Mismatch callback runs inline; keep it lightweight

---

## Performance Considerations

### Overhead

Shadow evaluation doubles the evaluation work:

- Baseline evaluation: ~O(n) where n = rules per flag
- Candidate evaluation: ~O(n) where n = rules per flag
- Total: ~O(2n)

**Mitigations:**

1. **Sampling** - Only shadow-evaluate a percentage of requests
2. **Async logging** - `onMismatch` callback should be non-blocking
3. **Time-boxing** - Run shadow evaluation for limited time period (e.g., 24 hours)

### Example: Sampled Shadow Evaluation

```kotlin
val shouldShadow = Random.nextDouble() < 0.10  // 10% sampling

val value = if (shouldShadow) {
    AppFeatures.darkMode.evaluateWithShadow(
        context = context,
        candidateRegistry = candidateRegistry,
        onMismatch = { mismatch ->
            logger.warn(
                "shadowMismatch key=${mismatch.featureKey} kinds=${mismatch.kinds} baseline=${mismatch.baseline.value} candidate=${mismatch.candidate.value}",
            )
        },
    )
} else {
    AppFeatures.darkMode.evaluate(context)
}
```

---

## Mismatch Analysis

### Common Causes of Mismatches

1. **Ramp-up percentage changed** - Users move in/out of ramp-up
2. **Targeting criteria changed** - Rules match different users
3. **Rule ordering changed** - Different rule wins due to specificity
4. **Salt changed** - Bucket assignment redistributed
5. **Configuration drift** - Candidate config is stale

### Debugging Mismatches

```kotlin
AppFeatures.darkMode.evaluateWithShadow(
    context = context,
    candidateRegistry = candidateRegistry,
    onMismatch = { m ->
        logger.error(
            "Mismatch detected baseline=${m.baseline.value} candidate=${m.candidate.value} baselineDecision=${m.baseline.decision::class.simpleName} candidateDecision=${m.candidate.decision::class.simpleName} stableId=${context.stableId.id}",
        )
    },
)
```

---

## Next Steps

- [Shadow Evaluation](/observability/shadow-evaluation) - Practical migration patterns
- [Observability Reference](/observability/reference) - `evaluateWithShadow` API
- [Core API Reference](/core/reference) - Evaluation baseline
