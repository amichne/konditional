# Roll Out a Feature Gradually

## Problem

You have a new feature and want to:

- Start with 10% of users to validate behavior
- Gradually increase to 100% over days/weeks
- Ensure the same user always gets the same experience
- Monitor metrics and roll back if needed

## Solution

### Step 1: Define the Feature with Ramp-Up

```kotlin
object AppFeatures : Namespace("app") {
  val newCheckoutFlow by boolean<Context>(default = false) {
    rule(true) { rampUp { 10.0 } }  // Start at 10%
  }
}
```

**How it works:**

- `rampUp { 10.0 }` enables the feature for 10% of users
- Bucketing is deterministic: SHA-256 hash of `(salt, featureKey, stableId)` determines bucket
- Users in buckets 0-9 (out of 0-99) get `true`, others get `false`

### Step 2: Evaluate with Consistent StableId

```kotlin
// Build context with persistent user ID
val ctx = Context(
    stableId = StableId(userId),  // Use database ID, NOT session ID
    platform = Platform.ANDROID
)

// Evaluate
val enabled: Boolean = AppFeatures.newCheckoutFlow.evaluate(ctx)

if (enabled) {
  showNewCheckoutFlow()
} else {
  showClassicCheckoutFlow()
}
```

**Critical:** Use a persistent identifier (database user ID, device ID) as `stableId`, not session IDs or random values.

### Step 3: Increase the Percentage

Update the ramp-up percentage via remote configuration:

```json
{
  "newCheckoutFlow": {
    "rules": [
      {
        "value": true,
        "rampUp": {
          "percentage": 25.0
        }
      }
    ]
  }
}
```

Load the updated configuration:

```kotlin
val json = fetchRemoteConfig()
when (val result = NamespaceSnapshotLoader(AppFeatures).load(json)) {
  is ParseResult.Success -> logger.info("Ramped up to 25%")
  is ParseResult.Failure -> {
    logger.error("Config load failed: ${result.error}")
    // Last-known-good (10%) remains active
  }
}
```

**What happens:** Users in buckets 0-24 now get the feature. Users in buckets 0-9 (the original 10%) remain in treatment—no one is removed when you increase
percentage.

### Step 4: Monitor and Adjust

Add observability to track rollout:

```kotlin
AppFeatures.hooks.afterEvaluation.add { event ->
  if (event.feature.key == "newCheckoutFlow") {
    metrics.increment("checkout_flow.${event.result}", tags = mapOf(
        "platform" to event.context.platform.toString()
    ))
  }
}
```

Monitor key metrics:

- Error rates in new checkout flow
- Conversion rates (treatment vs control)
- Latency differences
- User-reported issues

## Guarantees

- **Deterministic bucketing**: Same user + same feature + same salt = same bucket
  - **Mechanism**: SHA-256 hash of `"$salt:$featureKey:${stableId.hexId}"` mod 100
  - **Boundary**: Only deterministic if `stableId` is consistent across evaluations

- **Stable rollout**: Increasing percentage only adds users, never removes
  - **Mechanism**: Bucket thresholds increase monotonically (10% → 25% adds buckets 10-24)
  - **Boundary**: Changing salt reshuffles ALL users, breaking stability

- **Atomic updates**: All evaluations see either old % or new %, never partial state
  - **Mechanism**: Configuration load is atomic (see [Thread Safety](/production-operations/thread-safety))
  - **Boundary**: No guarantee about *when* a particular request sees the update

## What Can Go Wrong?

### Using Non-Persistent StableId

```kotlin
// DON'T: Session ID changes every session
val ctx = Context(stableId = StableId(sessionId))

// DON'T: Random ID changes every request
val ctx = Context(stableId = StableId(UUID.randomUUID().toString()))

// DO: Persistent user identifier
val ctx = Context(stableId = StableId(userId))  // Database ID
```

**Result of wrong stableId:** User gets different bucket on each session/request. Metrics become meaningless.

### Changing the Salt

```kotlin
// Before
rule(true) { rampUp { 50.0 } }  // Default salt

// After (RESHUFFLES ALL USERS!)
rule(true) { rampUp(salt = "v2") { 50.0 } }
```

**Result:** Every user gets reshuffled. Some users who had the feature lose it. Some who didn't have it suddenly get it. A/B test results invalidated.

**When to change salt:** Only when you explicitly want to reshuffle (e.g., addressing bias in original assignment).

### Decreasing Percentage Without Understanding

```kotlin
// From 50% down to 25%
rule(true) { rampUp { 25.0 } }
```

**Result:** Users in buckets 25-49 lose the feature. If they had data in the new system, it might become inaccessible.

**Best practice:** Only decrease if you understand the implications. Use kill switches instead for emergencies.

### Not Handling Configuration Load Failures

```kotlin
// DON'T: Ignore ParseResult
NamespaceSnapshotLoader(AppFeatures).load(json)

// DO: Handle failures
when (val result = NamespaceSnapshotLoader(AppFeatures).load(json)) {
  is ParseResult.Failure -> alertOps("Ramp-up config failed to load")
}
```

**Result of ignoring failures:** You think you've ramped up to 25%, but the config didn't load, so it's still at 10%. Metrics look wrong.

## Testing Ramp-Ups

### Test Determinism

```kotlin
@Test
fun `same user always gets same bucket`() {
  val userId = "test-user-123"
  val ctx = Context(stableId = StableId(userId))

  val results = (1..100).map {
    AppFeatures.newCheckoutFlow.evaluate(ctx)
  }

  // All evaluations should return the same value
  assertTrue(results.all { it == results.first() })
}
```

### Test Percentage Distribution

```kotlin
@Test
fun `10 percent ramp-up distributes correctly`() {
  val sampleSize = 10_000
  val rampUpPercentage = 10.0

  val inTreatment = (0 until sampleSize).count { i ->
    val ctx = Context(stableId = StableId("user-$i"))
    AppFeatures.newCheckoutFlow.evaluate(ctx)
  }

  val actualPercentage = (inTreatment.toDouble() / sampleSize) * 100

  // Should be within 1% of target
  assertEquals(rampUpPercentage, actualPercentage, delta = 1.0)
}
```

### Test Specific Users

```kotlin
@Test
fun `specific user is in treatment group`() {
  val userId = "VIP-user-456"
  val ctx = Context(stableId = StableId(userId))

  val result = AppFeatures.newCheckoutFlow.evaluate(ctx)

  assertTrue(result, "VIP user should be in treatment")
}
```

## Common Rollout Strategy

**Week 1:** 10% → monitor for crashes, errors
**Week 2:** 25% → validate metrics (conversion, latency)
**Week 3:** 50% → assess at scale
**Week 4:** 100% → full rollout

Between each increase:

- Review error rates
- Check conversion metrics
- Verify no performance degradation
- Gather user feedback

## Next Steps

- [Debugging Determinism Issues](/how-to-guides/debugging-determinism) — Troubleshoot bucketing problems
- [Safe Remote Configuration](/how-to-guides/safe-remote-config) — Load ramp-up changes from remote
- [Determinism Proofs (Theory)](/theory/determinism-proofs) — Why bucketing is deterministic
- [Production Operations: Debugging](/production-operations/debugging) — Inspect bucket assignments
