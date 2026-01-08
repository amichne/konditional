# How-To: Debug Determinism Issues

## Problem

You're experiencing:

- Users report inconsistent feature behavior ("I had the feature yesterday, but not today")
- A/B test results show users switching between variants
- Metrics show unexpected bucket distribution
- Ramp-up percentages don't match actual traffic split

## Solution

### Step 1: Verify StableId Consistency

The most common cause of non-determinism is inconsistent `stableId`:

```kotlin
// Add logging to capture stableId
fun evaluateWithLogging(userId: String): Boolean {
  val stableId = StableId(userId)
  logger.debug("Evaluating for user=$userId, stableId=${stableId.hexId}")

  val ctx = Context(stableId = stableId)
  val result = AppFeatures.newFeature.evaluate(ctx)

  logger.debug("User=$userId, result=$result")
  return result
}
```

**Check logs for the same user across multiple requests:**

```
2024-01-01 10:00:00 Evaluating for user=12345, stableId=abc123def456
2024-01-01 10:05:00 Evaluating for user=12345, stableId=abc123def456  // ✓ Same
2024-01-01 10:10:00 Evaluating for user=12345, stableId=789ghi012jkl  // ✗ CHANGED!
```

If `stableId` changes, bucketing is non-deterministic.

### Step 2: Verify Bucket Calculation

Calculate the bucket directly to understand assignment:

```kotlin
import io.amichne.konditional.rules.RampUpBucketing

fun debugBucketAssignment(
    userId: String,
    featureKey: String
) {
  val stableId = StableId(userId)

  val bucket = RampUpBucketing.calculateBucket(
      stableId = stableId,
      featureKey = featureKey,
      salt = "default"  // Or your custom salt
  )

  logger.info("""
        Bucket assignment for user=$userId:
        - Feature: $featureKey
        - StableId: ${stableId.hexId}
        - Bucket: $bucket (0-99)
    """.trimIndent())

  // Check against ramp-up threshold
  val rampUpPercentage = 50.0
  val inRampUp = bucket < rampUpPercentage

  logger.info("User is ${if (inRampUp) "IN" else "NOT IN"} the $rampUpPercentage% ramp-up")
}

// Usage
debugBucketAssignment("user-12345", "newCheckoutFlow")
```

### Step 3: Use the Explain API

Trace why evaluation returned a specific value:

```kotlin
fun debugEvaluation(userId: String) {
  val ctx = Context(stableId = StableId(userId))

  val explanation = AppFeatures.newCheckoutFlow.explain(ctx)

  logger.info("""
        Evaluation explanation:
        - Feature: ${explanation.feature.key}
        - Result: ${explanation.result}
        - Matched rule: ${explanation.matchedRule?.index ?: "default"}

        Rule evaluation trace:
    """.trimIndent())

  explanation.evaluationTrace.forEach { step ->
    val icon = if (step.matched) "✓" else "✗"
    logger.info("  $icon Rule #${step.ruleIndex}: ${step.reason}")
  }
}

// Usage
debugEvaluation("user-12345")
```

**Example output:**

```
Evaluation explanation:
- Feature: newCheckoutFlow
- Result: true
- Matched rule: 0

Rule evaluation trace:
  ✓ Rule #0: rampUp(50%) - bucket=42, threshold=50
  • Rule #1: platforms([IOS]) - not evaluated (previous rule matched)
  • Default: false - not reached
```

### Step 4: Test Determinism Locally

Reproduce the issue locally with the same inputs:

```kotlin
fun testDeterminism(userId: String) {
  val ctx = Context(stableId = StableId(userId))

  // Evaluate 100 times
  val results = (1..100).map {
    AppFeatures.newCheckoutFlow.evaluate(ctx)
  }

  // All results should be identical
  val allSame = results.all { it == results.first() }

  if (allSame) {
    logger.info("✓ Determinism verified for user=$userId: always returns ${results.first()}")
  } else {
    logger.error("✗ NON-DETERMINISTIC for user=$userId: got mixed results $results")
  }
}
```

## Common Causes of Non-Determinism

### Cause 1: Using Session ID as StableId

```kotlin
// ✗ WRONG: Session ID changes every session
val ctx = Context(stableId = StableId(sessionId))

// ✓ CORRECT: Use persistent user ID
val ctx = Context(stableId = StableId(userId))  // Database ID
```

**Symptom:** User gets different behavior on each session.

### Cause 2: Using Random Values

```kotlin
// ✗ WRONG: Random value changes every request
val ctx = Context(stableId = StableId(UUID.randomUUID().toString()))

// ✓ CORRECT: Use persistent identifier
val ctx = Context(stableId = StableId(getUserId()))
```

**Symptom:** User gets different behavior on every request.

### Cause 3: Salt Changed Unintentionally

```kotlin
// Before (deployed Monday)
rule(true) { rampUp { 50.0 } }  // Default salt

// After (deployed Tuesday)
rule(true) { rampUp(salt = "v2") { 50.0 } }  // Different salt!
```

**Symptom:** After Tuesday's deploy, users who had the feature lost it, and vice versa.

**Fix:** Only change salt when you explicitly want to reshuffle. Document salt changes.

### Cause 4: Inconsistent StableId Across Platforms

```kotlin
// Mobile app: uses device ID
val mobileStableId = StableId(deviceId)

// Web app: uses user ID
val webStableId = StableId(userId)

// Same user, different platforms → different buckets!
```

**Symptom:** User sees different behavior on mobile vs web.

**Fix:** Use consistent identifier across platforms (prefer user ID if logged in).

### Cause 5: Feature Key Typo

```kotlin
// Code references "newCheckoutFlow"
AppFeatures.newCheckoutFlow.evaluate(ctx)

// Config uses "new_checkout_flow" (different key!)
{
  "new_checkout_flow": { "rules": [...] }
}
```

**Symptom:** Config never loads for this feature. Feature uses static rules instead. If static rules differ from config,
behavior changes.

**Fix:** Ensure feature keys match between code and config. Keys are derived from property names.

## Debugging Checklist

When investigating non-determinism:

### 1. Capture StableId from Logs

```kotlin
logger.info("User=$userId, StableId=${ctx.stableId.hexId}")
```

Check if the same user has the same `stableId` across requests.

### 2. Verify Bucket Assignment

```kotlin
val bucket = RampUpBucketing.calculateBucket(stableId, featureKey, salt)
logger.info("User bucket: $bucket")
```

Check that bucket matches expectations.

### 3. Check for Salt Changes

```bash
# Search git history for salt changes
git log -p --all -S 'rampUp(salt' -- '*.kt'
```

### 4. Verify Feature Key Consistency

```kotlin
// In code
logger.info("Feature key: ${AppFeatures.newCheckoutFlow.key}")

// In config
logger.info("Config keys: ${jsonConfig.keys}")
```

Keys must match exactly.

### 5. Test Locally with Same Inputs

```kotlin
val ctx = Context(stableId = StableId("user-12345"))
val results = (1..100).map { AppFeatures.newCheckoutFlow.evaluate(ctx) }
require(results.all { it == results.first() }) { "Non-deterministic!" }
```

### 6. Check Configuration History

```kotlin
AppFeatures.hooks.afterLoad.add { event ->
  when (event.result) {
    is ParseResult.Success -> {
      logger.info("Config loaded at ${event.timestamp}")
      logger.info("Loaded features: ${event.result.loadedFeatures}")
    }
  }
}
```

Verify when configuration changed and what changed.

## Advanced Debugging

### Debugging Bucket Distribution

Verify that buckets distribute evenly across users:

```kotlin
fun analyzeBucketDistribution(sampleSize: Int = 10_000) {
  val buckets = (0 until sampleSize).map { i ->
    val stableId = StableId("user-$i")
    RampUpBucketing.calculateBucket(stableId, "testFeature", "default")
  }

  // Count users per bucket (0-99)
  val distribution = buckets.groupingBy { it }.eachCount()

  // Each bucket should have ~100 users (1% of 10,000)
  distribution.forEach { (bucket, count) ->
    val percentage = (count.toDouble() / sampleSize) * 100
    logger.info("Bucket $bucket: $count users ($percentage%)")
  }

  // Standard deviation should be low
  val mean = sampleSize / 100.0
  val variance = distribution.values.map { (it - mean).pow(2) }.average()
  val stdDev = sqrt(variance)

  logger.info("Distribution stdDev: $stdDev (should be < 10 for good distribution)")
}
```

### Debugging Configuration Drift

Compare loaded config to expected config:

```kotlin
fun debugConfigDrift() {
  val expectedRampUp = 50.0
  val actualRampUp = getLoadedRampUpPercentage(AppFeatures.newCheckoutFlow)

  if (actualRampUp != expectedRampUp) {
    logger.error("""
            Config drift detected:
            - Expected ramp-up: $expectedRampUp%
            - Actual ramp-up: $actualRampUp%
            - Possible causes: config load failed, wrong config deployed
        """.trimIndent())
  }
}
```

### Reproducing Production Behavior

Capture context from production and replay locally:

```kotlin
// In production: log context on evaluation
AppFeatures.hooks.afterEvaluation.add { event ->
  logger.info("Eval: user=${event.context.stableId}, result=${event.result}, context=${event.context}")
}

// Locally: replay with same context
fun replayEvaluation(productionLog: String) {
  // Parse: "Eval: user=abc123, result=true, context=..."
  val stableId = extractStableId(productionLog)
  val platform = extractPlatform(productionLog)

  val ctx = Context(
      stableId = StableId(stableId),
      platform = platform
  )

  val result = AppFeatures.newCheckoutFlow.evaluate(ctx)
  logger.info("Local replay: result=$result")
}
```

## Testing for Determinism

### Unit Test: Same User, Same Result

```kotlin
@Test
fun `evaluation is deterministic for same user`() {
  val userId = "test-user-123"
  val ctx = Context(stableId = StableId(userId))

  val results = (1..1000).map {
    AppFeatures.newCheckoutFlow.evaluate(ctx)
  }

  assertTrue(results.all { it == results.first() })
}
```

### Integration Test: Cross-Platform Consistency

```kotlin
@Test
fun `same user gets same bucket on all platforms`() {
  val userId = "test-user-456"

  val mobileCtx = Context(
      stableId = StableId(userId),
      platform = Platform.ANDROID
  )

  val webCtx = Context(
      stableId = StableId(userId),
      platform = Platform.WEB
  )

  // Same user → same bucket → potentially same result
  // (Actual result may differ if rules target specific platforms)
  val mobileBucket = RampUpBucketing.calculateBucket(
      StableId(userId), "feature", "default"
  )
  val webBucket = RampUpBucketing.calculateBucket(
      StableId(userId), "feature", "default"
  )

  assertEquals(mobileBucket, webBucket, "Same user should be in same bucket")
}
```

## Next Steps

- [Rolling Out Gradually](/how-to-guides/rolling-out-gradually) — Implement deterministic ramps
- [Operational Debugging](/production-operations/debugging) — Production debugging tools
- [Determinism Proofs (Theory)](/theory/determinism-proofs) — Why bucketing is deterministic
- [A/B Testing](/how-to-guides/ab-testing) — Variant assignment patterns
