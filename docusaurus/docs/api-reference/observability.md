# Observability

API reference for logging, metrics, and explainability: registry hooks, evaluation results, and deterministic bucketing utilities.

---

## `RegistryHooks`

Dependency-free hooks for logging and metrics adapters.

```kotlin
data class RegistryHooks(
    val logger: KonditionalLogger = KonditionalLogger.NoOp,
    val metrics: MetricsCollector = MetricsCollector.NoOp,
) {
    companion object {
        val None: RegistryHooks
        fun of(
            logger: KonditionalLogger = KonditionalLogger.NoOp,
            metrics: MetricsCollector = MetricsCollector.NoOp,
        ): RegistryHooks
    }
}
```

Attach hooks to a namespace (or any `NamespaceRegistry`) via `setHooks(...)`:

```kotlin
AppFeatures.setHooks(
    RegistryHooks.of(
        logger = myLogger,
        metrics = myMetrics,
    ),
)
```

Notes:

- Hooks run on the hot path; keep them lightweight and avoid blocking I/O.
- The core runtime does not expose a global “parse failure hook”; JSON parsing returns `ParseResult`, and you decide how to log/metric failures.

---

## `KonditionalLogger`

Minimal logging interface used by the core runtime.

```kotlin
interface KonditionalLogger {
    fun debug(message: () -> String) {}
    fun info(message: () -> String) {}
    fun warn(message: () -> String, throwable: Throwable? = null) {}
    fun error(message: () -> String, throwable: Throwable? = null) {}
}
```

Used by the runtime for:

- `Feature.explain(...)` debug lines (`konditional.explain ...`)
- shadow mismatch warnings emitted by `Feature.evaluateWithShadow(...)` (`konditional.shadowMismatch ...`)

---

## `MetricsCollector`

Minimal metrics interface used by the core runtime.

```kotlin
interface MetricsCollector {
    fun recordEvaluation(event: Metrics.Evaluation) {}
    fun recordConfigLoad(event: Metrics.ConfigLoadMetric) {}
    fun recordConfigRollback(event: Metrics.ConfigRollbackMetric) {}
}
```

### Metric payloads

```kotlin
object Metrics {
    data class Evaluation(
        val namespaceId: String,
        val featureKey: String,
        val mode: EvaluationMode,               // NORMAL | EXPLAIN | SHADOW
        val durationNanos: Long,
        val decision: DecisionKind,             // DEFAULT | RULE | INACTIVE | REGISTRY_DISABLED
        val configVersion: String? = null,
        val bucket: Int? = null,
        val matchedRuleSpecificity: Int? = null,
    )

    data class ConfigLoadMetric(
        val namespaceId: String,
        val featureCount: Int,
        val version: String? = null,
    )

    data class ConfigRollbackMetric(
        val namespaceId: String,
        val steps: Int,
        val success: Boolean,
        val version: String? = null,
    )
}
```

---

## `EvaluationResult<T>`

Returned by `Feature.explain(...)`.

```kotlin
data class EvaluationResult<T : Any>(
    val namespaceId: String,
    val featureKey: String,
    val configVersion: String?,
    val mode: Metrics.Evaluation.EvaluationMode,
    val durationNanos: Long,
    val value: T,
    val decision: Decision,
)
```

### Decision model

`decision` is a sealed hierarchy:

- `RegistryDisabled` — registry kill-switch enabled (`disableAll()`)
- `Inactive` — flag inactive (`isActive=false` in config)
- `Rule(matched, skippedByRollout?)` — a rule produced the value
- `Default(skippedByRollout?)` — no rule produced a value; returned the declared default

### Example: inspect a rule match

```kotlin
val result = AppFeatures.darkMode.explain(context)

when (val decision = result.decision) {
    EvaluationResult.Decision.RegistryDisabled -> logger.warn { "Registry disabled for ${result.featureKey}" }
    EvaluationResult.Decision.Inactive -> logger.warn { "Inactive flag ${result.featureKey}" }
    is EvaluationResult.Decision.Rule -> {
        val matchedRule = decision.matched.rule
        val bucket = decision.matched.bucket

        logger.info {
            "Matched rule key=${result.featureKey} note=${matchedRule.note} specificity=${matchedRule.totalSpecificity} bucket=${bucket.bucket}"
        }
    }
    is EvaluationResult.Decision.Default -> logger.info { "Default returned for ${result.featureKey}" }
}
```

---

## `BucketInfo`

Bucket assignment details (used in `EvaluationResult` and `RampUpBucketing`).

```kotlin
data class BucketInfo(
    val featureKey: String,
    val salt: String,
    val bucket: Int,                    // [0, 10_000)
    val rollout: RampUp,
    val thresholdBasisPoints: Int,
    val inRollout: Boolean,
)
```

---

## `RampUpBucketing`

Deterministic ramp-up bucketing utilities (guaranteed to match runtime evaluation behavior).

```kotlin
object RampUpBucketing {
    fun bucket(
        stableId: StableId,
        featureKey: String,
        salt: String,
    ): Int

    fun explain(
        stableId: StableId,
        featureKey: String,
        salt: String,
        rampUp: RampUp,
    ): BucketInfo
}
```

`RolloutBucketing` exists as a deprecated alias.

---

## `ShadowMismatch<T>`

Result type passed to shadow mismatch callbacks.

```kotlin
data class ShadowMismatch<T : Any>(
    val featureKey: String,
    val baseline: EvaluationResult<T>,
    val candidate: EvaluationResult<T>,
    val kinds: Set<Kind>,
) {
    enum class Kind { VALUE, DECISION }
}
```

Notes:

- The callback runs inline on the evaluation thread; keep it cheap or offload.
- If you need to associate mismatches with a user, capture `context.stableId` from the call site (it’s not embedded in the mismatch object).

---

## Next Steps

- [Feature Operations](/api-reference/feature-operations) — Evaluation and shadow APIs
- [Namespace Operations](/api-reference/namespace-operations) — Registry lifecycle operations
- [Advanced: Shadow Evaluation](/advanced/shadow-evaluation) — Migration patterns
