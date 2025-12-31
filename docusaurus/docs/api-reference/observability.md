# Observability

API reference for logging, metrics, and explainability: hooks, evaluation results, and bucketing utilities.

---

## `RegistryHooks`

Dependency-free hooks for logging and metrics adapters.

```kotlin
object RegistryHooks {
    var onEvaluationComplete: ((EvaluationEvent) -> Unit)? = null
    var onConfigurationLoad: ((ConfigurationLoadEvent) -> Unit)? = null
    var onParseFailure: ((ParseFailureEvent) -> Unit)? = null
}
```

### Hook Types

#### `onEvaluationComplete`

Invoked after every feature evaluation.

```kotlin
data class EvaluationEvent(
    val featureKey: FeatureKey,
    val context: Context,
    val value: Any,
    val decision: EvaluationDecision,
    val durationMs: Long
)
```

**Example:**

```kotlin
RegistryHooks.onEvaluationComplete = { event ->
    logger.debug("Evaluated ${event.featureKey}: ${event.value} (${event.decision})")
    metrics.histogram("feature.evaluation.duration", event.durationMs)
}
```

#### `onConfigurationLoad`

Invoked when configuration is successfully loaded.

```kotlin
data class ConfigurationLoadEvent(
    val namespaceId: String,
    val metadata: ConfigurationMetadata?,
    val flagCount: Int
)
```

**Example:**

```kotlin
RegistryHooks.onConfigurationLoad = { event ->
    logger.info("Loaded ${event.flagCount} flags into namespace ${event.namespaceId}")
    metrics.gauge("feature.flags.loaded", event.flagCount.toDouble())
}
```

#### `onParseFailure`

Invoked when JSON parsing fails.

```kotlin
data class ParseFailureEvent(
    val namespaceId: String?,
    val error: ParseError,
    val json: String?  // Truncated if large
)
```

**Example:**

```kotlin
RegistryHooks.onParseFailure = { event ->
    logger.error("Parse failed for ${event.namespaceId}: ${event.error}")
    metrics.increment("feature.config.parse.failure")
}
```

---

## `EvaluationResult<T>`

Explainable evaluation result with decision metadata.

```kotlin
data class EvaluationResult<T : Any>(
    val value: T,
    val decision: EvaluationDecision,
    val matchedRule: RuleInfo? = null,
    val bucketInfo: BucketInfo? = null
)
```

### Fields

- `value` — The evaluated value
- `decision` — Why this value was chosen
- `matchedRule` — Details of matched rule (if `RULE_MATCHED`)
- `bucketInfo` — Ramp-up bucket details (if rule has ramp-up)

### `EvaluationDecision` (Enum)

```kotlin
enum class EvaluationDecision {
    RULE_MATCHED,   // A rule matched and returned a value
    DEFAULT,        // No rules matched, using default
    INACTIVE,       // Flag is inactive
    DISABLED        // Namespace disabled via kill-switch
}
```

### Example: Logging Decisions

```kotlin
val result = AppFeatures.darkMode.explain(context)

when (result.decision) {
    EvaluationDecision.RULE_MATCHED -> {
        logger.info("Rule matched: ${result.matchedRule?.note}")
        logger.debug("Bucket: ${result.bucketInfo?.bucket}")
    }
    EvaluationDecision.DEFAULT -> {
        logger.info("No rules matched, using default: ${result.value}")
    }
    EvaluationDecision.INACTIVE -> {
        logger.warn("Flag ${AppFeatures.darkMode.key} is inactive")
    }
    EvaluationDecision.DISABLED -> {
        logger.error("Namespace disabled, returning default")
    }
}
```

---

## `RuleInfo`

Metadata about a matched rule.

```kotlin
data class RuleInfo(
    val note: String?,
    val specificity: Int,
    val platforms: Set<String>,
    val locales: Set<String>,
    val versionRange: VersionRange?,
    val axes: Map<String, Set<String>>,
    val hasExtension: Boolean
)
```

### Example: Audit Trail

```kotlin
val result = AppFeatures.apiEndpoint.explain(context)

result.matchedRule?.let { rule ->
    logger.info("""
        Matched rule: ${rule.note}
        Specificity: ${rule.specificity}
        Platforms: ${rule.platforms}
        Locales: ${rule.locales}
    """.trimIndent())
}
```

---

## `BucketInfo`

Ramp-up bucket assignment details.

```kotlin
data class BucketInfo(
    val bucket: Int,                    // Bucket value [0, 10_000)
    val thresholdBasisPoints: Int,      // Ramp-up threshold
    val inRampUp: Boolean,              // Whether user is in ramp-up
    val stableIdHex: String,            // Normalized stableId (hex)
    val bucketingInput: String          // Full input to SHA-256
)
```

### Example: Debug Ramp-Up

```kotlin
val result = AppFeatures.newFeature.explain(context)

result.bucketInfo?.let { info ->
    logger.debug("""
        Bucket: ${info.bucket} / 10,000
        Threshold: ${info.thresholdBasisPoints}
        In ramp-up: ${info.inRampUp}
        Bucketing input: ${info.bucketingInput}
    """.trimIndent())
}
```

---

## `RampUpBucketing.explain(...)`

Utility to compute bucket assignment for a specific user.

```kotlin
object RampUpBucketing {
    fun explain(
        stableId: StableId,
        featureKey: FeatureKey,
        salt: String,
        rampUp: RampUp
    ): BucketInfo
}
```

### Parameters

- `stableId` — User's stable identifier
- `featureKey` — Feature key
- `salt` — Feature salt
- `rampUp` — Ramp-up configuration

### Returns

`BucketInfo` with bucket assignment details.

### Example

```kotlin
val flag = AppFeatures.flag(AppFeatures.darkMode)

val info = RampUpBucketing.explain(
    stableId = context.stableId,
    featureKey = AppFeatures.darkMode.key,
    salt = flag.salt,
    rampUp = RampUp.of(25.0)
)

println("""
    User ${context.stableId.id} → bucket ${info.bucket}
    Threshold: ${info.thresholdBasisPoints} (25%)
    In ramp-up: ${info.inRampUp}
""".trimIndent())
```

---

## `ShadowMismatch<T>`

Result type for shadow evaluation mismatches.

```kotlin
data class ShadowMismatch<T : Any>(
    val primary: T,
    val shadow: T,
    val context: Context
)
```

### Fields

- `primary` — Value from primary registry
- `shadow` — Value from shadow registry
- `context` — Context used for evaluation

### Example

```kotlin
val mismatch = AppFeatures.darkMode.evaluateShadow(context, shadowOptions)

mismatch?.let {
    logger.error("""
        Shadow mismatch detected!
        Primary: ${it.primary}
        Shadow: ${it.shadow}
        User: ${it.context.stableId}
    """.trimIndent())
}
```

---

## Integration Example: Full Observability Stack

```kotlin
// Setup hooks at application startup
fun setupObservability() {
    RegistryHooks.onEvaluationComplete = { event ->
        // Log evaluation
        logger.debug("Eval: ${event.featureKey} → ${event.value}")

        // Metrics
        metrics.histogram("feature.evaluation.duration", event.durationMs)
        metrics.increment("feature.evaluation.count", mapOf(
            "feature" to event.featureKey.toString(),
            "decision" to event.decision.name
        ))
    }

    RegistryHooks.onConfigurationLoad = { event ->
        logger.info("Config loaded: ${event.metadata?.version}")
        metrics.gauge("feature.flags.count", event.flagCount.toDouble())
    }

    RegistryHooks.onParseFailure = { event ->
        logger.error("Parse failed: ${event.error}")
        metrics.increment("feature.config.parse.failure")
        alerting.notify("Config parse failure in ${event.namespaceId}")
    }
}

// Use explain for debugging
fun debugFeature(context: Context) {
    val result = AppFeatures.darkMode.explain(context)

    logger.info("Decision: ${result.decision}")
    result.matchedRule?.let { rule ->
        logger.info("Matched rule: ${rule.note} (specificity: ${rule.specificity})")
    }
    result.bucketInfo?.let { bucket ->
        logger.info("Bucket: ${bucket.bucket} (in ramp-up: ${bucket.inRampUp})")
    }
}
```

---

## Next Steps

- [Feature Operations](/api-reference/feature-operations) — Evaluation API
- [Namespace Operations](/api-reference/namespace-operations) — Lifecycle operations
- [Advanced: Shadow Evaluation](/advanced/shadow-evaluation) — Migration patterns
