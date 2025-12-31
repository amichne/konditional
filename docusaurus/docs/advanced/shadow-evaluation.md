# Shadow Evaluation

Practical patterns for comparing two Konditional configurations (baseline vs candidate) using shadow evaluation.

---

## Overview

Shadow evaluation evaluates the same feature against two registries:

- **Baseline registry**: returned value (production behavior)
- **Candidate registry**: comparison only

Konditional provides this via:

- `Feature.evaluateWithShadow(context, candidateRegistry, baselineRegistry = namespace, options = ShadowOptions.defaults(), onMismatch): T`
- `Feature.evaluateShadow(context, candidateRegistry, baselineRegistry = namespace, options = ShadowOptions.defaults(), onMismatch): Unit`

`onMismatch` receives a `ShadowMismatch<T>` containing both `EvaluationResult<T>` values (`baseline` + `candidate`) and a
`kinds` set (`VALUE`, and optionally `DECISION`).

---

## Pattern 1: Canary a Candidate Snapshot

Compare a candidate JSON snapshot against production traffic before promoting it.

### Setup

```kotlin
// Candidate: parse JSON into a Configuration
val candidateJson = fetchCandidateConfig()
val _ = AppFeatures // ensure features are registered before parsing
val candidateConfig = SnapshotSerializer.fromJson(candidateJson).getOrThrow()

// Candidate registry: isolate evaluation without touching production namespace state
val candidateRegistry = NamespaceRegistry(
    configuration = candidateConfig,
    namespaceId = AppFeatures.namespaceId,
)

// Collect mismatches for offline analysis
val mismatches = mutableListOf<ShadowMismatch<Boolean>>()
```

### Evaluate

```kotlin
val value = AppFeatures.darkMode.evaluateWithShadow(
    context = context,
    candidateRegistry = candidateRegistry,
    onMismatch = { mismatch ->
        mismatches.add(mismatch)
        logger.warn(
            "shadowMismatch key=${mismatch.featureKey} kinds=${mismatch.kinds} baseline=${mismatch.baseline.value} candidate=${mismatch.candidate.value} stableId=${context.stableId.id}",
        )
    },
)

// Production uses baseline value
applyDarkMode(value)
```

### Analyze + Promote

```kotlin
val mismatchRate = mismatches.size.toDouble() / totalEvaluations
val mismatchesByFeature = mismatches.groupBy { it.featureKey }

mismatchesByFeature.forEach { (featureKey, featureMismatches) ->
    val baselineTrue = featureMismatches.count { it.baseline.value }
    val candidateTrue = featureMismatches.count { it.candidate.value }
    logger.info("Feature=$featureKey mismatches=${featureMismatches.size} baselineTrue=$baselineTrue candidateTrue=$candidateTrue")
}

if (mismatchRate < 0.01) { // <1% mismatch
    AppFeatures.load(candidateConfig)
}
```

---

## Pattern 2: Sampling for Performance

Shadow evaluation adds an extra evaluation on the hot path. Sample a percentage of requests:

```kotlin
val shouldShadow = Random.nextDouble() < 0.10 // 10% sampling

val value =
    if (shouldShadow) {
        AppFeatures.darkMode.evaluateWithShadow(
            context = context,
            candidateRegistry = candidateRegistry,
            onMismatch = { mismatch ->
                logger.warn("shadowMismatch key=${mismatch.featureKey} kinds=${mismatch.kinds}")
            },
        )
    } else {
        AppFeatures.darkMode(context)
    }
```

---

## Pattern 3: Time-Boxed Shadow Campaign

Run shadow evaluation for a limited time window and then make a promotion decision.

```kotlin
class ShadowEvaluationCampaign(
    private val feature: Feature<Boolean, Context, *>,
    private val candidateRegistry: NamespaceRegistry,
    private val durationHours: Long = 24,
) {
    private val startTime = Instant.now()
    private val mismatchesByStableId = ConcurrentHashMap<StableId, ShadowMismatch<Boolean>>()

    fun evaluate(context: Context): Boolean {
        val elapsed = Duration.between(startTime, Instant.now())
        return if (elapsed.toHours() < durationHours) {
            feature.evaluateWithShadow(
                context = context,
                candidateRegistry = candidateRegistry,
                onMismatch = { mismatch -> mismatchesByStableId[context.stableId] = mismatch },
            )
        } else {
            feature(context)
        }
    }

    fun mismatchRate(totalEvaluations: Long): Double =
        mismatchesByStableId.size.toDouble() / totalEvaluations.toDouble()
}
```

---

## Pattern 4: Migration from Another Flag System

Konditional’s shadow APIs compare **two Konditional registries**. If you’re migrating from a non-Konditional system,
compare manually at the call site:

```kotlin
// Old system (baseline)
val oldValue = oldFlagClient.getBool("dark_mode", default = false)

// Konditional (candidate)
val candidateValue = AppFeatures.darkMode(context)

if (oldValue != candidateValue) {
    logger.warn("migrationMismatch stableId=${context.stableId.id} old=$oldValue konditional=$candidateValue")
}

// Keep production behavior unchanged
applyDarkMode(oldValue)
```

If you can translate the old system state into a Konditional snapshot, you can treat it as a `baselineRegistry` and use
`evaluateWithShadow` to compare registries directly (see `/theory/migration-and-shadowing`).

---

## Debugging Mismatches

`ShadowMismatch<T>` already carries both `EvaluationResult<T>` objects, so you can log decisions without re-evaluating:

```kotlin
AppFeatures.darkMode.evaluateWithShadow(
    context = context,
    candidateRegistry = candidateRegistry,
    onMismatch = { m ->
        val baseline = m.baseline
        val candidate = m.candidate
        logger.error(
            "Mismatch key=${m.featureKey} kinds=${m.kinds} baseline=${baseline.value} candidate=${candidate.value} baselineDecision=${baseline.decision::class.simpleName} candidateDecision=${candidate.decision::class.simpleName} stableId=${context.stableId.id}",
        )
    },
)
```

---

## Performance Notes

- Shadow evaluation runs inline; keep `onMismatch` fast (offload logging/metrics if needed).
- Candidate evaluation is skipped when the baseline registry kill-switch is enabled (by default).
- Hook both registries with `NamespaceRegistry.setHooks(...)` if you want separate baseline/candidate telemetry.

---

## Next Steps

- [Theory: Migration and Shadowing](/theory/migration-and-shadowing) — Concepts + guarantees
- [API Reference: Feature Operations](/api-reference/feature-operations) — Signatures + semantics
- [API Reference: Observability](/api-reference/observability) — `RegistryHooks` / metrics payloads
