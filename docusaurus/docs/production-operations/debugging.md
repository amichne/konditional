# Operational Debugging

Diagnosing and resolving issues with feature evaluation, determinism, and ramp-up bucketing.

---

## Overview

When features don't behave as expected in production, you need tools to understand what's happening. Konditional provides several debugging mechanisms:

1. **Explain API** — Trace why a specific evaluation returned a value
2. **Bucketing introspection** — Verify ramp-up determinism
3. **Rule evaluation logs** — Understand which rules matched
4. **Configuration inspection** — Validate loaded configuration

---

## Debugging Feature Evaluation

### Problem: "Why did this user get variant X?"

Use the `explain()` API to trace evaluation:

```kotlin
val ctx = Context(
    stableId = StableId("user-12345"),
    platform = Platform.IOS
)

val explanation = AppFeatures.checkoutFlow.explain(ctx)

println(explanation.summary())
/*
Feature: checkoutFlow
Result: CheckoutFlow.OPTIMIZED
Matched rule: #2 (platforms: [IOS])
Evaluation path:
  ✗ Rule #1 (EXPERIMENTAL): rampUp(10%) - user not in bucket
  ✓ Rule #2 (OPTIMIZED): platforms([IOS]) - matched
  • Default: CLASSIC (not reached)
*/
```

**When to use:**
- User reports unexpected behavior ("I don't see the new feature")
- A/B test results don't match expectations
- Verifying rule precedence in production

### Understanding Explain Output

```kotlin
data class EvaluationExplanation<T, C : Context>(
    val feature: Feature<T, C, *>,
    val context: C,
    val result: T,
    val matchedRule: Rule<T, C>?,
    val evaluationTrace: List<RuleEvaluationStep<T, C>>
)

data class RuleEvaluationStep<T, C : Context>(
    val ruleIndex: Int,
    val ruleValue: T,
    val matched: Boolean,
    val reason: String
)
```

**Example trace:**
```kotlin
explanation.evaluationTrace.forEach { step ->
    val icon = if (step.matched) "✓" else "✗"
    println("$icon Rule #${step.ruleIndex}: ${step.reason}")
}
```

---

## Debugging Ramp-Up Determinism

### Problem: "Users are getting different buckets"

Verify that bucketing is deterministic:

```kotlin
val userId = "user-12345"
val stableId = StableId(userId)
val ctx = Context(stableId = stableId)

// Evaluate multiple times
val results = (1..10).map {
    AppFeatures.newCheckout.evaluate(ctx)
}

// All results should be identical
require(results.all { it == results.first() }) {
    "Bucketing is non-deterministic for user $userId: $results"
}
```

**Common causes of non-determinism:**
1. **stableId changes between evaluations**
   ```kotlin
   // DON'T: New StableId every time
   val ctx1 = Context(stableId = StableId(UUID.randomUUID().toString()))
   val ctx2 = Context(stableId = StableId(UUID.randomUUID().toString()))

   // DO: Consistent stableId
   val userId = getConsistentUserId()  // e.g., database ID
   val ctx = Context(stableId = StableId(userId))
   ```

2. **Salt changed without understanding implications**
   ```kotlin
   // Changing salt reshuffles ALL users
   val feature by boolean<Context>(default = false) {
       rule(true) { rampUp { 50.0 } }  // Default salt
   }

   // Later: salt changed
   val feature by boolean<Context>(default = false) {
       rule(true) { rampUp(salt = "v2") { 50.0 } }  // Different bucket assignments!
   }
   ```

### Inspecting Bucket Assignment

```kotlin
import io.amichne.konditional.rules.RampUpBucketing

val userId = "user-12345"
val featureKey = "new_checkout"
val salt = "default"  // Or your custom salt

// Calculate bucket (0-99)
val bucket = RampUpBucketing.calculateBucket(
    stableId = StableId(userId),
    featureKey = featureKey,
    salt = salt
)

println("User $userId is in bucket $bucket for feature $featureKey")

// Check if user is in ramp-up
val rampUpPercentage = 50.0
val inRampUp = bucket < rampUpPercentage
println("User is ${if (inRampUp) "IN" else "NOT IN"} the $rampUpPercentage% ramp-up")
```

**Use when:**
- Verifying specific users should/shouldn't be in a ramp-up
- Debugging reported inconsistencies
- Understanding bucket distribution

### Verifying Ramp-Up Distribution

Test that bucketing distributes users evenly:

```kotlin
fun testRampUpDistribution() {
    val sampleSize = 10000
    val rampUpPercentage = 30.0

    val inRampUp = (0 until sampleSize).count { userId ->
        val ctx = Context(stableId = StableId("user-$userId"))
        AppFeatures.experimentalFeature.evaluate(ctx)  // Returns true if in ramp-up
    }

    val actualPercentage = (inRampUp.toDouble() / sampleSize) * 100

    // Should be within ~1% of target
    require((actualPercentage - rampUpPercentage).absoluteValue < 1.0) {
        "Ramp-up distribution off: expected $rampUpPercentage%, got $actualPercentage%"
    }
}
```

---

## Debugging Rule Evaluation

### Problem: "Rule isn't matching when it should"

Add logging to trace rule evaluation:

```kotlin
val feature by boolean<Context>(default = false) {
    rule(true) {
        android()
        logger.debug("Android rule evaluated: $this")
    }
    rule(true) {
        rampUp { 50.0 }
        logger.debug("Ramp-up rule evaluated: $this")
    }
}
```

Or use observability hooks:

```kotlin
AppFeatures.hooks.afterEvaluation.add { event ->
    logger.debug("""
        Feature: ${event.feature.key}
        Context: ${event.context}
        Result: ${event.result}
        Matched rule: ${event.matchedRule?.let { "Rule #${it.index}" } ?: "default"}
    """.trimIndent())
}
```

### Understanding Rule Specificity

Rules are evaluated in order until one matches:

```kotlin
val feature by boolean<Context>(default = false) {
    rule(true) { rampUp { 10.0 } }         // Rule #1: Most specific
    rule(true) { platforms(Platform.IOS) }  // Rule #2: Less specific
    rule(false) { android() }               // Rule #3: Least specific
}

// Evaluation stops at first match:
// - If user in 10% ramp-up → returns true (Rule #1 matches, stops)
// - Else if iOS platform → returns true (Rule #2 matches, stops)
// - Else if Android → returns false (Rule #3 matches, stops)
// - Else → returns false (default)
```

**Debugging tip:** Add temporary logging to each rule to see evaluation order.

---

## Debugging Configuration Loading

### Problem: "Configuration isn't loading correctly"

Add logging around `ParseResult`:

```kotlin
when (val result = NamespaceSnapshotLoader(AppFeatures).load(configJson)) {
    is ParseResult.Success -> {
        logger.info("Config loaded successfully")
        logger.debug("Loaded features: ${result.loadedFeatures}")
    }
    is ParseResult.Failure -> {
        logger.error("Config load failed")
        logger.error("Error: ${result.error}")
        logger.error("JSON: $configJson")

        when (result.error) {
            is ParseError.InvalidJSON -> logger.error("JSON syntax error")
            is ParseError.UnknownFeature -> logger.error("Reference to undefined feature")
            is ParseError.TypeMismatch -> logger.error("Type doesn't match definition")
        }
    }
}
```

### Inspecting Loaded Configuration

After a successful load, inspect what was loaded:

```kotlin
when (val result = NamespaceSnapshotLoader(AppFeatures).load(configJson)) {
    is ParseResult.Success -> {
        result.loadedFeatures.forEach { (featureKey, overrides) ->
            logger.info("Feature $featureKey: ${overrides.size} override(s) loaded")
        }
    }
}
```

### Validating JSON Before Loading

Pre-validate JSON to catch issues early:

```kotlin
import kotlinx.serialization.json.Json

fun validateConfigJson(json: String): Result<Unit> {
    return runCatching {
        Json.parseToJsonElement(json)  // Validates JSON syntax
    }
}

// Usage
when (validateConfigJson(configJson)) {
    is Result.Success -> {
        // JSON is syntactically valid, now try to load
        NamespaceSnapshotLoader(AppFeatures).load(configJson)
    }
    is Result.Failure -> {
        logger.error("Invalid JSON syntax", e)
    }
}
```

---

## Debugging Context Issues

### Problem: "Feature evaluation depends on context, but behavior is wrong"

Inspect the context being passed:

```kotlin
val ctx = buildContext()

// Log context before evaluation
logger.debug("""
    Evaluating with context:
    - stableId: ${ctx.stableId}
    - platform: ${ctx.platform}
    - locale: ${ctx.locale}
    - version: ${ctx.appVersion}
""".trimIndent())

val result = AppFeatures.someFeature.evaluate(ctx)
```

### Common Context Mistakes

**1. Wrong stableId:**
```kotlin
// DON'T: Random or session-based ID
val ctx = Context(stableId = StableId(sessionId))  // Changes per session

// DO: Persistent user ID
val ctx = Context(stableId = StableId(userId))  // Consistent across sessions
```

**2. Missing context fields:**
```kotlin
// DON'T: Forgot to set platform
val ctx = Context(stableId = StableId(userId))  // platform = null

// DO: Provide all relevant fields
val ctx = Context(
    stableId = StableId(userId),
    platform = Platform.ANDROID,
    locale = Locale.US
)
```

**3. Wrong context type:**
```kotlin
interface PremiumContext : Context {
    val subscriptionTier: SubscriptionTier
}

val premiumFeature by boolean<PremiumContext>(default = false) {
    rule(true) { extension { subscriptionTier == SubscriptionTier.ENTERPRISE } }
}

// DON'T: Pass base Context
val ctx: Context = Context(...)
premiumFeature.evaluate(ctx)  // Compile error: wrong type

// DO: Pass PremiumContext
val ctx: PremiumContext = buildPremiumContext()
premiumFeature.evaluate(ctx)  // ✓
```

---

## Production Debugging Checklist

When investigating feature issues in production:

### 1. Verify stableId consistency
```kotlin
// Log stableId for the affected user
logger.info("User ${userId} has stableId: ${ctx.stableId}")

// Check that it's consistent across requests
```

### 2. Use explain() to trace evaluation
```kotlin
val explanation = feature.explain(ctx)
logger.info(explanation.summary())
```

### 3. Check ramp-up bucket assignment
```kotlin
val bucket = RampUpBucketing.calculateBucket(ctx.stableId, featureKey, salt)
logger.info("User in bucket $bucket (ramp-up threshold: $percentage%)")
```

### 4. Verify configuration is loaded
```kotlin
// Check when configuration was last updated
logger.info("Last config load: ${AppFeatures.lastLoadedAt}")

// Verify specific feature is configured as expected
val configured = AppFeatures.someFeature.hasOverrides()
logger.info("Feature has overrides: $configured")
```

### 5. Inspect context fields
```kotlin
logger.info("Context: platform=${ctx.platform}, locale=${ctx.locale}, version=${ctx.appVersion}")
```

### 6. Test locally with same inputs
```kotlin
// Reproduce the exact evaluation locally
val ctx = Context(
    stableId = StableId("user-12345"),  // From logs
    platform = Platform.IOS,
    locale = Locale.US
)

val result = AppFeatures.someFeature.evaluate(ctx)
logger.info("Local evaluation result: $result")
```

---

## Common Production Issues

### Issue: User reports "I don't see the new feature"

**Debug steps:**
1. Get user's stableId from logs
2. Use `explain()` to see why they didn't match any enabled rules
3. Check if they're in the ramp-up bucket (if applicable)
4. Verify context fields (platform, locale, version) match expectations

### Issue: A/B test results show 0% treatment group

**Debug steps:**
1. Verify ramp-up percentage in loaded configuration
2. Check that feature key matches between definition and JSON
3. Verify `ParseResult` was `Success` when config was loaded
4. Use `explain()` on sample users to verify bucketing

### Issue: "Feature behavior changed unexpectedly"

**Debug steps:**
1. Check if configuration was recently updated
2. Compare current config to previous version
3. Verify salt wasn't changed (causes reshuffle)
4. Check for rule changes that affect precedence

---

## Summary

Konditional provides debugging tools for production issues:

- **explain() API** — Trace why evaluation returned a specific value
- **Bucketing introspection** — Verify ramp-up determinism
- **ParseResult logging** — Diagnose configuration load failures
- **Context inspection** — Verify inputs to evaluation

When debugging:
1. Start with `explain()` to understand evaluation
2. Verify stableId consistency for determinism
3. Check configuration was loaded successfully
4. Inspect context fields match expectations
5. Reproduce locally with same inputs

---

## Next Steps

- [Thread Safety](/production-operations/thread-safety) — Understanding concurrent evaluation
- [Failure Modes](/production-operations/failure-modes) — Common failure scenarios
- [How-To: Debugging Determinism Issues](/how-to-guides/debugging-determinism) — Step-by-step guide
