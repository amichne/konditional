# Shadow Evaluation

Practical patterns for comparing two Konditional configurations (baseline vs candidate) using shadow evaluation.

---

## Overview

Shadow evaluation evaluates the same feature against two registries:

- Baseline registry: returned value (production behavior)
- Candidate registry: comparison only

Konditional provides this via:

-

`Feature.evaluateWithShadow(context, candidateRegistry, baselineRegistry = namespace, options = ShadowOptions.defaults(), onMismatch): T`
-
`Feature.evaluateShadow(context, candidateRegistry, baselineRegistry = namespace, options = ShadowOptions.defaults(), onMismatch): Unit`

`onMismatch` receives a `ShadowMismatch<T>` containing both `EvaluationResult<T>` values (baseline + candidate) and a
`kinds` set (`VALUE`, and optionally `DECISION`).

### Prerequisites

- `konditional-observability` for `evaluateWithShadow` / `evaluateShadow`
- `konditional-runtime` for `InMemoryNamespaceRegistry`
- `konditional-serialization` if you load candidate snapshots from JSON

---

## Pattern 1: Canary a candidate snapshot

Compare a candidate JSON snapshot against production traffic before promoting it.

### Setup

```kotlin
val candidateJson = fetchCandidateConfig()
val candidateConfig = ConfigurationSnapshotCodec.decode(candidateJson).getOrThrow()

val candidateRegistry = InMemoryNamespaceRegistry(namespaceId = AppFeatures.namespaceId).apply {
    load(candidateConfig)
}

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

applyDarkMode(value)
```

### Analyze and promote

```kotlin
val mismatchRate = mismatches.size.toDouble() / totalEvaluations

if (mismatchRate < 0.01) {
    AppFeatures.load(candidateConfig)
}
```

---

## Pattern 2: Sampling for performance

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
        AppFeatures.darkMode.evaluate(context)
    }
```

---

## Pattern 3: Time-boxed shadow campaign

Run shadow evaluation for a limited window and then decide whether to promote.

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
            feature.evaluate(context)
        }
    }
}
```

---

## Pattern 4: Migration from another flag system

Konditional shadow APIs compare two Konditional registries. If you are migrating from a non-Konditional system, compare
manually at the call site:

```kotlin
val oldValue = oldFlagClient.getBool("dark_mode", default = false)
val candidateValue = AppFeatures.darkMode.evaluate(context)

if (oldValue != candidateValue) {
    logger.warn("migrationMismatch stableId=${context.stableId.id} old=$oldValue konditional=$candidateValue")
}

applyDarkMode(oldValue)
```

---

## Next steps

- [Theory: Migration and shadowing](/theory/migration-and-shadowing)
- [Observability reference](/observability/reference)
