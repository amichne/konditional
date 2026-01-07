# A/B Test Feature Variants

## Problem

You want to:

- Test multiple variants of a feature (A, B, C)
- Assign users deterministically to variants
- Measure conversion, engagement, or other metrics per variant
- Ensure the same user always sees the same variant

## Solution

### Step 1: Define Variants as an Enum

```kotlin
enum class CheckoutVariant {
  CLASSIC,      // Control
  SIMPLIFIED,   // Treatment A
  ENHANCED      // Treatment B
}
```

**Why enum:** Type-safe, exhaustive when-expressions, clear intent.

### Step 2: Define the Feature with Variant Rules

```kotlin
object AppFeatures : Namespace("app") {
  val checkoutExperiment by enum<CheckoutVariant, Context>(
      default = CheckoutVariant.CLASSIC
  ) {
    // 33% get SIMPLIFIED
    rule(CheckoutVariant.SIMPLIFIED) {
      rampUp { 33.0 }
    }

    // 33% get ENHANCED (different bucket range)
    rule(CheckoutVariant.ENHANCED) {
      rampUp { 66.0 }  // 66% total, so this is the next 33%
    }

    // Remaining 34% get CLASSIC (default)
  }
}
```

**How bucketing works:**

- Users in buckets 0-32 (33%) → SIMPLIFIED
- Users in buckets 33-65 (33%) → ENHANCED
- Users in buckets 66-99 (34%) → CLASSIC

### Step 3: Implement Variant-Specific Logic

```kotlin
val ctx = Context(stableId = StableId(userId))
val variant: CheckoutVariant = AppFeatures.checkoutExperiment.evaluate(ctx)

when (variant) {
  CheckoutVariant.CLASSIC -> showClassicCheckout()
  CheckoutVariant.SIMPLIFIED -> showSimplifiedCheckout()
  CheckoutVariant.ENHANCED -> showEnhancedCheckout()
}
```

**Exhaustive when:** Compiler ensures you handle all variants.

### Step 4: Track Metrics Per Variant

```kotlin
AppFeatures.hooks.afterEvaluation.add { event ->
  if (event.feature.key == "checkoutExperiment") {
    val variant = event.result as CheckoutVariant

    // Track which variant user sees
    analytics.track("checkout_experiment_assigned", mapOf(
        "variant" to variant.name,
        "user_id" to event.context.stableId.hexId
    ))
  }
}

// Track conversion per variant
fun onCheckoutCompleted(
    userId: String,
    revenue: Double
) {
  val ctx = Context(stableId = StableId(userId))
  val variant = AppFeatures.checkoutExperiment.evaluate(ctx)

  analytics.track("checkout_completed", mapOf(
      "variant" to variant.name,
      "revenue" to revenue
  ))
}
```

## Guarantees

- **Deterministic assignment**: Same user always gets same variant
  - **Mechanism**: SHA-256 bucketing based on stableId
  - **Boundary**: Only deterministic if stableId is consistent

- **Stable variants**: Users don't switch variants during experiment
  - **Mechanism**: Bucket assignment doesn't change unless salt changes
  - **Boundary**: Changing salt or percentages reshuffles users

- **Type safety**: Can't assign invalid variants
  - **Mechanism**: Enum constrains possible values at compile-time
  - **Boundary**: Runtime config still needs validation (ParseResult)

## Advanced Patterns

### Unequal Split (90% Control, 10% Treatment)

```kotlin
val experimentalFeature by enum<Variant, Context>(default = Variant.CONTROL) {
  rule(Variant.TREATMENT) { rampUp { 10.0 } }
  // Remaining 90% get CONTROL (default)
}
```

### Multiple Factors (Platform + Variant)

```kotlin
val checkoutExperiment by enum<CheckoutVariant, Context>(
    default = CheckoutVariant.CLASSIC
) {
  // iOS users: 50/50 SIMPLIFIED vs ENHANCED
  rule(CheckoutVariant.SIMPLIFIED) {
    ios()
    rampUp { 50.0 }
  }
  rule(CheckoutVariant.ENHANCED) {
    ios()
    rampUp { 100.0 }  // Remaining 50% of iOS users
  }

  // Android users: 33/33/34 split
  rule(CheckoutVariant.SIMPLIFIED) {
    android()
    rampUp { 33.0 }
  }
  rule(CheckoutVariant.ENHANCED) {
    android()
    rampUp { 66.0 }
  }
}
```

**Note:** Rules are evaluated in order. Platform-specific ramps are evaluated before general rules.

### Holdout Group (Always Control)

```kotlin
val experimentWithHoldout by enum<Variant, Context>(default = Variant.CONTROL) {
  // 10% forced control (holdout)
  rule(Variant.CONTROL) {
    extension { userId in holdoutList }
  }

  // 45% treatment A
  rule(Variant.TREATMENT_A) { rampUp { 45.0 } }

  // 45% treatment B
  rule(Variant.TREATMENT_B) { rampUp { 90.0 } }

  // Remaining 10% natural control
}
```

## What Can Go Wrong?

### Overlapping Ramp-Up Ranges

```kotlin
// WRONG: Overlapping percentages
rule(Variant.A) { rampUp { 50.0 } }   // Buckets 0-49
rule(Variant.B) { rampUp { 50.0 } }   // ALSO buckets 0-49!
```

**Result:** Both rules match for users in buckets 0-49. First rule wins (Variant.A), Variant.B never assigned.

**Fix:** Use cumulative percentages:

```kotlin
rule(Variant.A) { rampUp { 50.0 } }   // Buckets 0-49
rule(Variant.B) { rampUp { 100.0 } }  // Buckets 50-99
```

### Not Handling All Variants in Code

```kotlin
// WRONG: Missing ENHANCED case
when (variant) {
  CheckoutVariant.CLASSIC -> showClassicCheckout()
  CheckoutVariant.SIMPLIFIED -> showSimplifiedCheckout()
  // ENHANCED falls through, compiler error if exhaustive required
}
```

**Fix:** Handle all cases or use exhaustive when:

```kotlin
when (variant) {
  CheckoutVariant.CLASSIC -> showClassicCheckout()
  CheckoutVariant.SIMPLIFIED -> showSimplifiedCheckout()
  CheckoutVariant.ENHANCED -> showEnhancedCheckout()
}  // Compiler enforces exhaustiveness
```

### Changing Variant Percentages Mid-Experiment

```kotlin
// Week 1: 33/33/34 split
// Week 2: Change to 50/25/25

// Users in buckets 33-49 switch from ENHANCED → SIMPLIFIED
// A/B test results contaminated
```

**Best practice:** Lock variant assignment percentages for experiment duration. Only adjust after experiment concludes.

### Inconsistent StableId

```kotlin
// Mobile: uses device ID
val mobileCtx = Context(stableId = StableId(deviceId))

// Web: uses user ID
val webCtx = Context(stableId = StableId(userId))

// Same user, different buckets on mobile vs web!
```

**Fix:** Use consistent identifier across platforms (user ID if logged in, device fingerprint if not).

## Testing A/B Variants

### Test Variant Distribution

```kotlin
@Test
fun `variants distribute evenly`() {
  val sampleSize = 10_000
  val results = (0 until sampleSize).map { i ->
    val ctx = Context(stableId = StableId("user-$i"))
    AppFeatures.checkoutExperiment.evaluate(ctx)
  }

  val counts = results.groupingBy { it }.eachCount()

  // Each variant should be ~33%
  assertEquals(3333, counts[CheckoutVariant.CLASSIC]!!, delta = 100)
  assertEquals(3333, counts[CheckoutVariant.SIMPLIFIED]!!, delta = 100)
  assertEquals(3334, counts[CheckoutVariant.ENHANCED]!!, delta = 100)
}
```

### Test Specific Variant Assignment

```kotlin
@Test
fun `specific user gets expected variant`() {
  val ctx = Context(stableId = StableId("test-user-123"))
  val variant = AppFeatures.checkoutExperiment.evaluate(ctx)

  // Calculate expected bucket
  val bucket = RampUpBucketing.calculateBucket(
      stableId = StableId("test-user-123"),
      featureKey = "checkoutExperiment",
      salt = "default"
  )

  val expectedVariant = when {
    bucket < 33 -> CheckoutVariant.SIMPLIFIED
    bucket < 66 -> CheckoutVariant.ENHANCED
    else -> CheckoutVariant.CLASSIC
  }

  assertEquals(expectedVariant, variant)
}
```

## Analyzing Results

### Calculate Conversion Per Variant

```kotlin
data class VariantMetrics(
    val variant: CheckoutVariant,
    val impressions: Int,
    val conversions: Int,
    val revenue: Double
) {
  val conversionRate: Double = conversions.toDouble() / impressions
  val avgRevenuePerUser: Double = revenue / impressions
}

// Query from analytics
val metrics = listOf(
    VariantMetrics(CheckoutVariant.CLASSIC, 10000, 1500, 75000.0),
    VariantMetrics(CheckoutVariant.SIMPLIFIED, 10000, 1650, 82500.0),
    VariantMetrics(CheckoutVariant.ENHANCED, 10000, 1800, 90000.0)
)

metrics.forEach { m ->
  println("${m.variant}: CR=${m.conversionRate}, ARPU=${m.avgRevenuePerUser}")
}
// CLASSIC: CR=0.15, ARPU=7.5
// SIMPLIFIED: CR=0.165, ARPU=8.25 (+10% CR, +10% revenue)
// ENHANCED: CR=0.18, ARPU=9.0 (+20% CR, +20% revenue)
```

### Statistical Significance

Use standard A/B test significance calculators with:

- Sample size per variant
- Conversion rate per variant
- Confidence level (typically 95%)

**Important:** Konditional provides deterministic assignment. Statistical analysis is your responsibility.

## Next Steps

- [Rolling Out Gradually](/how-to-guides/rolling-out-gradually) — Ramp up winning variant
- [Debugging Determinism](/how-to-guides/debugging-determinism) — Verify bucketing
- [Testing Features](/how-to-guides/testing-features) — Test variant logic
- [Determinism Proofs (Theory)](/theory/determinism-proofs) — Why bucketing is deterministic
