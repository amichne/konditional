# Shadow Evaluation

Practical patterns for A/B testing configuration changes and migrating between flag systems using shadow evaluation.

---

## Overview

Shadow evaluation means: evaluate against two configurations (primary + shadow) simultaneously, compare results, but only use the primary value in production.

**Key insight:** Production behavior is unchanged, but you get advance warning of mismatches before making the shadow configuration primary.

---

## Pattern 1: Canary Testing Config Changes

Test a candidate configuration against production traffic before rolling it out.

### Setup

```kotlin
// Primary: current production config
val productionRegistry = AppFeatures.registry

// Shadow: candidate config to test
val candidateJson = fetchCandidateConfig()
val candidateConfig = SnapshotSerializer.fromJson(candidateJson).value
val candidateRegistry = InMemoryNamespaceRegistry.from(candidateConfig)

// Mismatch logger
val mismatchLog = mutableListOf<ShadowMismatch<*>>()
```

### Evaluation

```kotlin
val value = AppFeatures.darkMode.evaluateWithShadow(
    context = context,
    shadowOptions = ShadowEvaluationOptions(
        shadowRegistry = candidateRegistry,
        onMismatch = { mismatch ->
            mismatchLog.add(mismatch)
            logger.warn("""
                Shadow mismatch:
                Feature: ${mismatch.featureKey}
                Primary: ${mismatch.primary}
                Shadow: ${mismatch.shadow}
                User: ${mismatch.context.stableId}
            """.trimIndent())
        }
    )
)

// Production uses primary value
applyDarkMode(value)
```

### Analysis

```kotlin
// After collecting mismatches from production traffic
val mismatchRate = mismatchLog.size.toDouble() / totalEvaluations
val mismatchesByFeature = mismatchLog.groupBy { it.featureKey }

mismatchesByFeature.forEach { (featureKey, mismatches) ->
    val primaryTrue = mismatches.count { it.primary == true }
    val shadowTrue = mismatches.count { it.shadow == true }

    logger.info("""
        Feature: $featureKey
        Mismatches: ${mismatches.size}
        Primary=true: $primaryTrue
        Shadow=true: $shadowTrue
    """.trimIndent())
}

// Decision: If mismatch rate is acceptable, promote shadow to primary
if (mismatchRate < 0.01) {  // <1% mismatch
    AppFeatures.load(candidateConfig)
}
```

---

## Pattern 2: Sampling for Performance

Shadow evaluation doubles the work. Sample a percentage of requests:

```kotlin
val shouldShadow = Random.nextDouble() < 0.10  // 10% sampling

val value = if (shouldShadow) {
    AppFeatures.darkMode.evaluateWithShadow(context, shadowOptions)
} else {
    AppFeatures.darkMode.evaluate(context)
}
```

### Dynamic Sampling Based on Load

```kotlin
val currentLoad = getSystemLoad()
val samplingRate = when {
    currentLoad < 0.5 -> 0.50  // Low load: 50% sampling
    currentLoad < 0.8 -> 0.10  // Medium load: 10% sampling
    else -> 0.01              // High load: 1% sampling
}

val shouldShadow = Random.nextDouble() < samplingRate
```

---

## Pattern 3: Time-Boxed Shadow Evaluation

Run shadow evaluation for a limited time, then make a decision:

```kotlin
class ShadowEvaluationCampaign(
    val feature: Feature<Boolean, Context, *>,
    val shadowRegistry: NamespaceRegistry,
    val durationHours: Long = 24
) {
    private val startTime = Instant.now()
    private val mismatches = ConcurrentHashMap<StableId, ShadowMismatch<Boolean>>()

    fun evaluate(context: Context): Boolean {
        val elapsed = Duration.between(startTime, Instant.now())

        return if (elapsed.toHours() < durationHours) {
            // Still in shadow period
            feature.evaluateWithShadow(
                context = context,
                shadowOptions = ShadowEvaluationOptions(
                    shadowRegistry = shadowRegistry,
                    onMismatch = { mismatch ->
                        mismatches[context.stableId] = mismatch as ShadowMismatch<Boolean>
                    }
                )
            )
        } else {
            // Shadow period ended, use primary only
            feature.evaluate(context)
        }
    }

    fun getMismatchReport(): ShadowMismatchReport {
        return ShadowMismatchReport(
            totalMismatches = mismatches.size,
            mismatchRate = mismatches.size.toDouble() / totalEvaluations,
            mismatches = mismatches.values.toList()
        )
    }
}

// Usage
val campaign = ShadowEvaluationCampaign(
    feature = AppFeatures.darkMode,
    shadowRegistry = candidateRegistry,
    durationHours = 24
)

// Over 24 hours...
val value = campaign.evaluate(context)

// After 24 hours
val report = campaign.getMismatchReport()
println("Mismatch rate: ${report.mismatchRate * 100}%")
```

---

## Pattern 4: Migration from Another Flag System

Migrate from an old flag system to Konditional with confidence.

### Phase 1: Old System Primary, Konditional Shadow

```kotlin
// Old system (primary)
val oldSystemValue = oldFlagClient.getBool("dark_mode", false)

// Konditional (shadow)
val mismatch = AppFeatures.darkMode.evaluateShadow(
    context = context,
    shadowOptions = ShadowEvaluationOptions(
        shadowRegistry = konditionalRegistry,
        onMismatch = { m ->
            logger.error("""
                Migration mismatch:
                Old system: $oldSystemValue
                Konditional: ${m.shadow}
                User: ${m.context.stableId}
            """.trimIndent())
        }
    )
)

// Use old system value (production unchanged)
applyDarkMode(oldSystemValue)
```

### Phase 2: Konditional Primary, Old System Shadow

Once mismatch rate is acceptable, flip:

```kotlin
// Konditional (primary)
val konditionalValue = AppFeatures.darkMode.evaluate(context)

// Old system (shadow, for verification)
val oldSystemValue = oldFlagClient.getBool("dark_mode", false)

if (konditionalValue != oldSystemValue) {
    logger.warn("Regression detected: konditional=$konditionalValue, old=$oldSystemValue")
}

// Use Konditional value
applyDarkMode(konditionalValue)
```

### Phase 3: Decommission Old System

After confidence is established, remove old system integration.

---

## Pattern 5: Feature-Level Shadow Evaluation

Shadow-evaluate specific features (not entire registry):

```kotlin
class FeatureShadowEvaluator<T : Any>(
    val feature: Feature<T, Context, *>,
    val shadowDefinition: FlagDefinition<T, Context, *>
) {
    fun evaluate(context: Context): T {
        val primaryValue = feature.evaluate(context)

        val shadowValue = evaluateDefinition(shadowDefinition, context)

        if (primaryValue != shadowValue) {
            logger.warn("Mismatch for ${feature.key}: primary=$primaryValue, shadow=$shadowValue")
        }

        return primaryValue
    }
}

// Usage: Test a new rule definition without affecting production
val newDefinition = FlagDefinition(
    key = AppFeatures.darkMode.key,
    default = false,
    rules = listOf(
        Rule(value = true, criteria = RuleCriteria(platforms = setOf(Platform.IOS)), rampUp = RampUp.of(50.0))
    ),
    salt = "v2",
    isActive = true
)

val evaluator = FeatureShadowEvaluator(AppFeatures.darkMode, newDefinition)
val value = evaluator.evaluate(context)
```

---

## Debugging Mismatches

### Detailed Mismatch Logging

```kotlin
shadowOptions = ShadowEvaluationOptions(
    shadowRegistry = candidateRegistry,
    onMismatch = { m ->
        val primaryReason = feature.evaluateWithReason(context)
        val shadowFeature = candidateRegistry.getFlag(feature.key)
        val shadowReason = shadowFeature?.evaluateWithReason(context)

        logger.error("""
            ┌─ Shadow Mismatch Detected ─────────────────────
            │ Feature: ${m.featureKey}
            │ User: ${m.context.stableId}
            ├─ Primary: ${m.primary} ─────────────────────────
            │   Decision: ${primaryReason.decision}
            │   Matched Rule: ${primaryReason.matchedRule?.note}
            │   Bucket: ${primaryReason.bucketInfo?.bucket}
            ├─ Shadow: ${m.shadow} ──────────────────────────
            │   Decision: ${shadowReason?.decision}
            │   Matched Rule: ${shadowReason?.matchedRule?.note}
            │   Bucket: ${shadowReason?.bucketInfo?.bucket}
            └─────────────────────────────────────────────────
        """.trimIndent())
    }
)
```

### Mismatch Categories

```kotlin
data class MismatchAnalysis(
    val totalMismatches: Int,
    val primaryTrueShadowFalse: Int,
    val primaryFalseShadowTrue: Int,
    val affectedUsers: Set<StableId>
)

fun analyzeMismatches(mismatches: List<ShadowMismatch<Boolean>>): MismatchAnalysis {
    return MismatchAnalysis(
        totalMismatches = mismatches.size,
        primaryTrueShadowFalse = mismatches.count { it.primary == true && it.shadow == false },
        primaryFalseShadowTrue = mismatches.count { it.primary == false && it.shadow == true },
        affectedUsers = mismatches.map { it.context.stableId }.toSet()
    )
}
```

---

## Performance Considerations

### Async Shadow Evaluation

```kotlin
class AsyncShadowEvaluator<T : Any>(
    private val feature: Feature<T, Context, *>,
    private val shadowOptions: ShadowEvaluationOptions,
    private val scope: CoroutineScope
) {
    fun evaluate(context: Context): T {
        val primaryValue = feature.evaluate(context)

        // Shadow evaluation in background (non-blocking)
        scope.launch {
            val shadowValue = shadowOptions.shadowRegistry
                .getFlag(feature.key)
                ?.evaluate(context)

            if (shadowValue != null && primaryValue != shadowValue) {
                shadowOptions.onMismatch(ShadowMismatch(
                    featureKey = feature.key,
                    primary = primaryValue,
                    shadow = shadowValue,
                    context = context
                ))
            }
        }

        return primaryValue  // Return immediately
    }
}
```

### Latency Budget

```kotlin
fun evaluateWithShadowAndTimeout(
    feature: Feature<Boolean, Context, *>,
    context: Context,
    shadowOptions: ShadowEvaluationOptions,
    timeoutMs: Long = 100
): Boolean {
    val primaryValue = feature.evaluate(context)

    // Shadow evaluation with timeout
    runCatching {
        withTimeout(timeoutMs) {
            val shadowValue = shadowOptions.shadowRegistry
                .getFlag(feature.key)
                ?.evaluate(context)

            if (shadowValue != null && primaryValue != shadowValue) {
                shadowOptions.onMismatch(ShadowMismatch(...))
            }
        }
    }.onFailure {
        logger.warn("Shadow evaluation timed out after ${timeoutMs}ms")
    }

    return primaryValue
}
```

---

## Next Steps

- [Theory: Migration and Shadowing](/theory/migration-and-shadowing) — Formal guarantees
- [API Reference: Feature Operations](/api-reference/feature-operations) — `evaluateWithShadow` API
- [Migration Guide](/migration) — Step-by-step migration patterns
