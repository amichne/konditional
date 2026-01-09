# How-To: Test Your Feature Flags

## Problem

You need to:

- Verify feature evaluation logic works correctly
- Test rule matching and precedence
- Validate ramp-up bucketing and determinism
- Test configuration loading and parsing
- Ensure regression protection for feature behavior

## Solution

### Step 1: Unit Test Basic Evaluation

```kotlin
@Test
fun `iOS users get dark mode enabled`() {
    val ctx = Context(
        stableId = StableId("user-123"),
        platform = Platform.IOS,
        locale = Locale.US,
        appVersion = Version.of(2, 1, 0)
    )

    val enabled = AppFeatures.darkMode.evaluate(ctx)

    assertTrue(enabled)
}

@Test
fun `Android users get dark mode disabled`() {
    val ctx = Context(
        stableId = StableId("user-123"),
        platform = Platform.ANDROID,
        locale = Locale.US,
        appVersion = Version.of(2, 1, 0)
    )

    val enabled = AppFeatures.darkMode.evaluate(ctx)

    assertFalse(enabled)
}
```

**What to test:** Verify that rules match expected contexts.

### Step 2: Use Parameterized Tests for Variants

```kotlin
@ParameterizedTest
@CsvSource(
    "IOS, true",
    "ANDROID, false",
    "WEB, false"
)
fun `dark mode platform targeting`(platform: Platform, expected: Boolean) {
    val ctx = Context(
        stableId = StableId("user-123"),
        platform = platform,
        locale = Locale.US,
        appVersion = Version.of(2, 1, 0)
    )

    assertEquals(expected, AppFeatures.darkMode.evaluate(ctx))
}
```

**Benefits:** Test multiple scenarios with single test method. Easier to add new cases.

### Step 3: Test Rule Matching (AND Semantics)

```kotlin
@Test
fun `rule matches only when ALL criteria match`() {
    // All criteria match
    val matchingCtx = Context(
        stableId = StableId("user"),
        platform = Platform.IOS,          // ✓ iOS
        locale = Locale.US,                // ✓ US
        appVersion = Version.of(2, 1, 0)   // ✓ >= 2.0.0
    )
    assertTrue(AppFeatures.premiumFeature.evaluate(matchingCtx))

    // Missing one criterion (wrong platform)
    val wrongPlatform = matchingCtx.copy(platform = Platform.ANDROID)
    assertFalse(AppFeatures.premiumFeature.evaluate(wrongPlatform))

    // Missing one criterion (wrong locale)
    val wrongLocale = matchingCtx.copy(locale = Locale.UK)
    assertFalse(AppFeatures.premiumFeature.evaluate(wrongLocale))

    // Missing one criterion (wrong version)
    val wrongVersion = matchingCtx.copy(appVersion = Version.of(1, 9, 0))
    assertFalse(AppFeatures.premiumFeature.evaluate(wrongVersion))
}
```

**What to test:** Verify AND semantics—all predicates must match.

### Step 4: Test Ramp-Up Determinism

```kotlin
@Test
fun `same user always gets same bucket`() {
    val userId = "user-123"
    val ctx = Context(stableId = StableId(userId))

    // Evaluate 100 times
    val results = (1..100).map {
        AppFeatures.experimentalFeature.evaluate(ctx)
    }

    // All results must be identical
    assertTrue(results.all { it == results.first() })
}

@Test
fun `different users get different buckets`() {
    val results = (0 until 100).map { i ->
        val ctx = Context(stableId = StableId("user-$i"))
        AppFeatures.experimentalFeature.evaluate(ctx)
    }

    // Should have mix of true and false
    assertTrue(results.any { it })
    assertTrue(results.any { !it })
}
```

**What to test:** Verify determinism and distribution.

### Step 5: Test Ramp-Up Distribution

```kotlin
@Test
fun `50 percent ramp-up distributes correctly`() {
    val sampleSize = 10_000
    val rampUpPercentage = 50.0

    val inTreatment = (0 until sampleSize).count { i ->
        val ctx = Context(stableId = StableId("user-$i"))
        AppFeatures.fiftyPercentFeature.evaluate(ctx)
    }

    val actualPercentage = (inTreatment.toDouble() / sampleSize) * 100

    // Should be within 1% of target
    assertEquals(rampUpPercentage, actualPercentage, delta = 1.0)
}
```

**What to test:** Verify percentage distribution is accurate.

## Testing Configuration Loading

### Test Valid Configuration

```kotlin
@Test
fun `valid configuration loads successfully`() {
    val json = """
    {
      "darkMode": {
        "rules": [
          {
            "value": true,
            "predicates": {
              "platforms": ["IOS"]
            }
          }
        ]
      }
    }
    """.trimIndent()

    val result = NamespaceSnapshotLoader(AppFeatures).load(json)

    assertTrue(result is ParseResult.Success)

    // Verify loaded config is active
    val ctx = Context(stableId = StableId("user"), platform = Platform.IOS)
    assertTrue(AppFeatures.darkMode.evaluate(ctx))
}
```

### Test Invalid Configuration Rejection

```kotlin
@Test
fun `invalid JSON is rejected`() {
    val invalidJson = """{ "darkMode": { invalid json } }"""

    val result = NamespaceSnapshotLoader(AppFeatures).load(invalidJson)

    assertTrue(result is ParseResult.Failure)
    assertTrue((result as ParseResult.Failure).error is ParseError.InvalidJSON)
}

@Test
fun `type mismatch is rejected`() {
    val json = """{ "maxRetries": { "rules": [{ "value": "five" }] } }"""

    val result = NamespaceSnapshotLoader(AppFeatures).load(json)

    assertTrue(result is ParseResult.Failure)
    assertTrue((result as ParseResult.Failure).error is ParseError.TypeMismatch)
}

@Test
fun `unknown feature is rejected`() {
    val json = """{ "unknownFeature": { "rules": [{ "value": true }] } }"""

    val result = NamespaceSnapshotLoader(AppFeatures).load(json)

    assertTrue(result is ParseResult.Failure)
    assertTrue((result as ParseResult.Failure).error is ParseError.UnknownFeature)
}
```

### Test Last-Known-Good Preservation

```kotlin
@Test
fun `failed load preserves last-known-good`() {
    // Load valid config
    val validJson = """{ "darkMode": { "rules": [{ "value": true }] } }"""
    val result1 = NamespaceSnapshotLoader(AppFeatures).load(validJson)
    require(result1 is ParseResult.Success)

    val ctx = Context(stableId = StableId("user"))
    assertTrue(AppFeatures.darkMode.evaluate(ctx))  // true from config

    // Try to load invalid config
    val invalidJson = """{ "darkMode": { "rules": [{ "value": "invalid" }] } }"""
    val result2 = NamespaceSnapshotLoader(AppFeatures).load(invalidJson)
    require(result2 is ParseResult.Failure)

    // Verify last-known-good preserved
    assertTrue(AppFeatures.darkMode.evaluate(ctx))  // Still true
}
```

## Using Konditional Test Helpers

Konditional provides pre-built test helpers via testFixtures. Add the dependency (see [Installation](/getting-started/installation#test-fixtures-optional)) to access:

### CommonTestFeatures and EnterpriseTestFeatures

Pre-configured feature flags for testing common scenarios:

```kotlin
import io.amichne.konditional.fixtures.CommonTestFeatures
import io.amichne.konditional.fixtures.EnterpriseTestFeatures

@Test
fun `test using pre-built features`() {
    val ctx = Context(stableId = StableId("user-123"))

    // CommonTestFeatures provides standard testing flags
    val enabled = CommonTestFeatures.testFeature.evaluate(ctx)

    // EnterpriseTestFeatures provides enterprise-tier testing flags
    val premiumEnabled = EnterpriseTestFeatures.enterpriseFeature.evaluate(ctx)
}
```

### TargetingIds — Deterministic Bucket Targeting

Pre-computed stable IDs for targeting specific ramp-up buckets:

```kotlin
import io.amichne.konditional.fixtures.utilities.TargetingIds

@Test
fun `test with known bucket assignments`() {
    // TargetingIds provides IDs that hash to specific buckets
    val inBucketId = TargetingIds.idInBucket(percentage = 50.0)
    val outOfBucketId = TargetingIds.idOutOfBucket(percentage = 50.0)

    val inCtx = Context(stableId = StableId(inBucketId))
    val outCtx = Context(stableId = StableId(outOfBucketId))

    assertTrue(AppFeatures.fiftyPercentFeature.evaluate(inCtx))
    assertFalse(AppFeatures.fiftyPercentFeature.evaluate(outCtx))
}
```

### FeatureMutators — Dynamic Configuration

Utilities for modifying feature configurations during tests:

```kotlin
import io.amichne.konditional.fixtures.utilities.FeatureMutators

@Test
fun `test with modified feature configuration`() {
    val ctx = Context(stableId = StableId("user"))

    // Temporarily modify a feature's configuration
    FeatureMutators.withOverride(AppFeatures.darkMode, value = true) {
        assertTrue(AppFeatures.darkMode.evaluate(ctx))
    }

    // Configuration restored after block
    assertFalse(AppFeatures.darkMode.evaluate(ctx))
}
```

### TestNamespace and TestStableId

Testing utilities for namespace isolation and deterministic IDs:

```kotlin
import io.amichne.konditional.fixtures.core.TestNamespace
import io.amichne.konditional.fixtures.core.id.TestStableId

@Test
fun `test with test namespace`() {
    // TestNamespace provides an isolated namespace for testing
    val testNs = TestNamespace("test-ns")

    // TestStableId provides predictable stable IDs
    val deterministicId = TestStableId.forTest("test-user-1")
    val ctx = Context(stableId = deterministicId)
}
```

---

## Advanced Testing Patterns

### Pattern: Test Fixtures for Contexts

```kotlin
object TestContexts {
    fun iosUser(userId: String = "test-user") = Context(
        stableId = StableId(userId),
        platform = Platform.IOS,
        locale = Locale.US,
        appVersion = Version.of(2, 0, 0)
    )

    fun androidUser(userId: String = "test-user") = Context(
        stableId = StableId(userId),
        platform = Platform.ANDROID,
        locale = Locale.US,
        appVersion = Version.of(2, 0, 0)
    )

    fun premiumUser(userId: String = "test-user") = BusinessContext(
        stableId = StableId(userId),
        platform = Platform.IOS,
        locale = Locale.US,
        appVersion = Version.of(2, 0, 0),
        subscriptionTier = SubscriptionTier.PRO,
        accountAgeMonths = 12,
        lifetimeRevenue = 500.0
    )
}

@Test
fun `test using fixtures`() {
    assertTrue(AppFeatures.darkMode.evaluate(TestContexts.iosUser()))
    assertFalse(AppFeatures.darkMode.evaluate(TestContexts.androidUser()))
}
```

### Pattern: Test Data Builders

```kotlin
class ContextBuilder {
    private var stableId: StableId = StableId("default-user")
    private var platform: Platform = Platform.IOS
    private var locale: Locale = Locale.US
    private var appVersion: Version = Version.of(2, 0, 0)

    fun withStableId(id: String) = apply { this.stableId = StableId(id) }
    fun withPlatform(p: Platform) = apply { this.platform = p }
    fun withLocale(l: Locale) = apply { this.locale = l }
    fun withVersion(v: Version) = apply { this.appVersion = v }

    fun build() = Context(
        stableId = stableId,
        platform = platform,
        locale = locale,
        appVersion = appVersion
    )
}

@Test
fun `test using builder`() {
    val ctx = ContextBuilder()
        .withPlatform(Platform.ANDROID)
        .withLocale(Locale.UK)
        .build()

    assertFalse(AppFeatures.premiumFeature.evaluate(ctx))
}
```

### Pattern: Test Specific Users in Buckets

```kotlin
@Test
fun `verify specific user is in treatment bucket`() {
    val userId = "VIP-user-789"
    val bucket = RampUpBucketing.calculateBucket(
        stableId = StableId(userId),
        featureKey = "experimentalFeature",
        salt = "default"
    )

    // VIP user should be in bucket < 50 (50% ramp-up)
    assertTrue(bucket < 50, "VIP user bucket=$bucket should be < 50")

    // Verify via evaluation
    val ctx = Context(stableId = StableId(userId))
    assertTrue(AppFeatures.experimentalFeature.evaluate(ctx))
}
```

### Pattern: Property-Based Testing

```kotlin
@Property
fun `any valid context returns a result`(
    @ForAll userId: String,
    @ForAll platform: Platform,
    @ForAll locale: Locale
) {
    val ctx = Context(
        stableId = StableId(userId),
        platform = platform,
        locale = locale,
        appVersion = Version.of(2, 0, 0)
    )

    // Should not throw
    val result = AppFeatures.someFeature.evaluate(ctx)

    // Result should be a valid Boolean
    assertTrue(result is Boolean)
}
```

## Testing Custom Business Logic

### Test Extension Predicates

```kotlin
@Test
fun `enterprise users with high revenue get advanced analytics`() {
    val ctx = BusinessContext(
        stableId = StableId("user"),
        platform = Platform.IOS,
        locale = Locale.US,
        appVersion = Version.of(2, 0, 0),
        subscriptionTier = SubscriptionTier.ENTERPRISE,
        accountAgeMonths = 12,
        lifetimeRevenue = 15_000.0,
        isEmployee = false
    )

    assertTrue(PremiumFeatures.advancedAnalytics.evaluate(ctx))
}

@ParameterizedTest
@CsvSource(
    "ENTERPRISE, 15000.0, true",
    "ENTERPRISE, 9999.0, false",
    "PRO, 15000.0, false",
    "FREE, 15000.0, false"
)
fun `advanced analytics edge cases`(
    tier: SubscriptionTier,
    revenue: Double,
    expected: Boolean
) {
    val ctx = BusinessContext(
        stableId = StableId("user"),
        platform = Platform.IOS,
        locale = Locale.US,
        appVersion = Version.of(2, 0, 0),
        subscriptionTier = tier,
        accountAgeMonths = 12,
        lifetimeRevenue = revenue,
        isEmployee = false
    )

    assertEquals(expected, PremiumFeatures.advancedAnalytics.evaluate(ctx))
}
```

## Integration Testing

### Test End-to-End Flow

```kotlin
@Test
fun `load config and evaluate features end-to-end`() {
    // 1. Load configuration
    val json = fetchRemoteConfig()
    val loadResult = NamespaceSnapshotLoader(AppFeatures).load(json)
    require(loadResult is ParseResult.Success)

    // 2. Build context
    val ctx = Context(
        stableId = StableId("integration-test-user"),
        platform = Platform.IOS,
        locale = Locale.US,
        appVersion = Version.of(2, 1, 0)
    )

    // 3. Evaluate features
    val darkMode = AppFeatures.darkMode.evaluate(ctx)
    val maxRetries = AppFeatures.maxRetries.evaluate(ctx)

    // 4. Verify behavior
    assertTrue(darkMode)
    assertEquals(5, maxRetries)
}
```

### Test Configuration Refresh

```kotlin
@Test
fun `configuration refresh updates evaluation`() {
    val ctx = Context(stableId = StableId("user"))

    // Initial evaluation
    assertFalse(AppFeatures.darkMode.evaluate(ctx))  // Default: false

    // Load new config
    val json = """{ "darkMode": { "rules": [{ "value": true }] } }"""
    NamespaceSnapshotLoader(AppFeatures).load(json)

    // Evaluation reflects new config
    assertTrue(AppFeatures.darkMode.evaluate(ctx))
}
```

## Testing Best Practices

### 1. Test Defaults

```kotlin
@Test
fun `features return defaults when no rules match`() {
    val ctx = Context(
        stableId = StableId("user"),
        platform = Platform.WEB,  // No rules target WEB
        locale = Locale.US,
        appVersion = Version.of(2, 0, 0)
    )

    // Should return default value
    assertEquals(CheckoutVariant.CLASSIC, AppFeatures.checkoutVariant.evaluate(ctx))
}
```

### 2. Test Rule Precedence

```kotlin
@Test
fun `rules are evaluated in order`() {
    // Rule 1: iOS → true (more specific)
    // Rule 2: rampUp(50%) → true (less specific)
    // Default: false

    // iOS user NOT in 50% bucket should match Rule 1
    val iosUser = Context(
        stableId = StableId("ios-user-not-in-bucket"),
        platform = Platform.IOS,
        /* ... */
    )
    assertTrue(AppFeatures.feature.evaluate(iosUser))  // Matches Rule 1

    // Android user IN 50% bucket should match Rule 2
    val androidInBucket = Context(
        stableId = StableId("android-in-bucket"),
        platform = Platform.ANDROID,
        /* ... */
    )
    // (Verify bucket first, then test)
}
```

### 3. Test Edge Cases

```kotlin
@Test
fun `empty string stableId is valid`() {
    val ctx = Context(stableId = StableId(""))
    // Should not throw
    AppFeatures.someFeature.evaluate(ctx)
}

@Test
fun `very long stableId is valid`() {
    val longId = "x".repeat(10_000)
    val ctx = Context(stableId = StableId(longId))
    // Should not throw
    AppFeatures.someFeature.evaluate(ctx)
}
```

## Next Steps

- [Rolling Out Gradually](/how-to-guides/rolling-out-gradually) — Implement and test ramps
- [A/B Testing](/how-to-guides/ab-testing) — Test variant assignment
- [Custom Business Logic](/how-to-guides/custom-business-logic) — Test extension predicates
- [Debugging Determinism](/how-to-guides/debugging-determinism) — Debug test failures
