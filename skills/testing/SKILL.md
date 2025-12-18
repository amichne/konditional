---
name: testing
description: Generate comprehensive test patterns for Konditional features covering rule matching, determinism, rollout distribution, and configuration lifecycle
---

# Konditional Testing

## Instructions

### Test Rule Matching
```kotlin
@Test
fun `rule matches when all criteria satisfied`() {
    val context = Context(
        locale = AppLocale.UNITED_STATES,
        platform = Platform.IOS,
        appVersion = Version.of(2, 1, 0),
        stableId = StableId.of("user-123")
    )

    val result = Features.darkMode.evaluate(context)
    assertTrue(result, "iOS users in US should get dark mode")
}
```

### Test Rule Precedence
```kotlin
@Test
fun `higher specificity rules win`() {
    // Feature with two rules:
    // - platforms(IOS) + locales(US) → specificity 2
    // - platforms(IOS) → specificity 1

    val iosUs = Context(platform = Platform.IOS, locale = AppLocale.UNITED_STATES, ...)
    val iosFr = Context(platform = Platform.IOS, locale = AppLocale.FRANCE, ...)

    val specificResult = Features.feature.evaluate(iosUs)
    val generalResult = Features.feature.evaluate(iosFr)

    assertNotEquals(specificResult, generalResult,
        "More specific rule should return different value")
}
```

### Test Determinism
```kotlin
@Test
fun `evaluation is deterministic`() {
    val context = Context(
        locale = AppLocale.UNITED_STATES,
        platform = Platform.IOS,
        appVersion = Version.of(2, 1, 0),
        stableId = StableId.of("user-123")
    )

    val results = (1..1000).map { Features.darkMode.evaluate(context) }

    assertTrue(results.distinct().size == 1,
        "Evaluation must be deterministic—only one unique result")
}
```

### Test Rollout Distribution
```kotlin
@Test
fun `50 percent rollout distributes correctly`() {
    val sampleSize = 10_000

    val enabledCount = (0 until sampleSize).count { i ->
        val ctx = Context(
            locale = AppLocale.UNITED_STATES,
            platform = Platform.IOS,
            appVersion = Version.of(2, 1, 0),
            stableId = StableId.of(i.toString().padStart(32, '0'))
        )
        Features.rolloutFeature.evaluate(ctx)
    }

    val percentage = (enabledCount.toDouble() / sampleSize) * 100

    // Allow ±2% margin for statistical variance
    assertTrue(percentage in 48.0..52.0, "Expected ~50%, got $percentage%")
}
```

### Test Rollout Stability
```kotlin
@Test
fun `increasing rollout adds users without reshuffling`() {
    // Test 10% rollout
    val at10Percent = (0..999).filter { i ->
        val ctx = Context(stableId = StableId.of(i.toString().padStart(32, '0')), ...)
        Features.feature.evaluate(ctx)  // 10% rollout
    }.toSet()

    // Update to 20% rollout
    updateFeatureRollout(20.0)

    val at20Percent = (0..999).filter { i ->
        val ctx = Context(stableId = StableId.of(i.toString().padStart(32, '0')), ...)
        Features.feature.evaluate(ctx)  // 20% rollout
    }.toSet()

    // All users in 10% should still be in 20%
    assertTrue(at10Percent.all { it in at20Percent },
        "Increasing rollout should only add users")

    val ratio = at20Percent.size.toDouble() / at10Percent.size
    assertTrue(ratio in 1.8..2.2, "20% should have ~2x users of 10%")
}
```

### Test Allowlist Bypass
```kotlin
@Test
fun `allowlisted users bypass rollout`() {
    val allowlistedId = StableId.of("tester-1")

    // Feature with 0% rollout but allowlist
    val context = Context(stableId = allowlistedId, ...)

    val result = Features.experimentalFeature.evaluate(context)

    assertTrue(result, "Allowlisted users should get feature despite 0% rollout")
}
```

### Test Configuration Lifecycle
```kotlin
@Test
fun `valid JSON loads successfully`() {
    val json = """
    {
      "flags": [{
        "key": "feature::app::darkMode",
        "defaultValue": { "type": "BOOLEAN", "value": false },
        "salt": "v1",
        "isActive": true,
        "rules": []
      }]
    }
    """.trimIndent()

    val result = SnapshotSerializer.fromJson(json)
    assertTrue(result is ParseResult.Success, "Valid JSON should parse successfully")
}

@Test
fun `failed parse preserves last known good`() {
    // Load valid config
    SnapshotSerializer.fromJson(validJson).let {
        if (it is ParseResult.Success) Features.load(it.value)
    }

    val enabledBefore = Features.darkMode.evaluate(context)

    // Try to load invalid config
    SnapshotSerializer.fromJson(invalidJson).let {
        if (it is ParseResult.Success) Features.load(it.value)
    }

    val enabledAfter = Features.darkMode.evaluate(context)

    assertEquals(enabledBefore, enabledAfter, "Failed parse should not affect evaluation")
}
```

## Examples

### Context Builder Pattern
```kotlin
fun testContext(
    locale: AppLocale = AppLocale.UNITED_STATES,
    platform: Platform = Platform.IOS,
    version: Version = Version.of(2, 0, 0),
    stableId: StableId = StableId.of("test-user")
) = Context(
    locale = locale,
    platform = platform,
    appVersion = version,
    stableId = stableId
)

@Test
fun `example with builder`() {
    val iosContext = testContext(platform = Platform.IOS)
    val androidContext = testContext(platform = Platform.ANDROID)
    // ... assertions
}
```

### Parameterized Tests
```kotlin
@ParameterizedTest
@CsvSource(
    "IOS, UNITED_STATES, true",
    "IOS, FRANCE, false",
    "ANDROID, UNITED_STATES, false"
)
fun `platform and locale combinations`(
    platform: Platform,
    locale: AppLocale,
    expected: Boolean
) {
    val context = testContext(platform = platform, locale = locale)
    assertEquals(expected, Features.darkMode.evaluate(context))
}
```

### Common Mistakes

**Mistake 1: Not testing determinism**
**Wrong**: Test evaluation once
**Right**: Test evaluation 100-1000 times to verify determinism

**Mistake 2: Small sample for distribution tests**
**Wrong**: Test with 100 users
**Right**: Test with 10,000+ users for statistical confidence

**Mistake 3: Assuming rule definition order matters**
**Wrong**: Assume last rule defined is evaluated first
**Right**: Test that specificity determines order, not definition order

**Mistake 4: Not testing negative cases**
**Wrong**: Only test when rules should match
**Right**: Test both matching and non-matching cases
