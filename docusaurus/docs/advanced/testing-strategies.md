# Testing Strategies

Comprehensive testing patterns for features, rules, ramp-ups, and configuration lifecycle.

---

## Unit Testing Features

### Basic Evaluation Test

```kotlin
@Test
fun `iOS users get dark mode enabled`() {
    val ctx = Context(
        locale = AppLocale.UNITED_STATES,
        platform = Platform.IOS,
        appVersion = Version.of(2, 1, 0),
        stableId = StableId.of("user-123")
    )

    val enabled = AppFeatures.darkMode.evaluate(ctx)
    assertTrue(enabled)
}

@Test
fun `Android users get dark mode disabled`() {
    val ctx = Context(
        locale = AppLocale.UNITED_STATES,
        platform = Platform.ANDROID,
        appVersion = Version.of(2, 1, 0),
        stableId = StableId.of("user-123")
    )

    val enabled = AppFeatures.darkMode.evaluate(ctx)
    assertFalse(enabled)
}
```

### Parameterized Tests

```kotlin
@ParameterizedTest
@CsvSource(
    "IOS, true",
    "ANDROID, false",
    "WEB, false"
)
fun `dark mode enabled only for iOS`(platform: Platform, expected: Boolean) {
    val ctx = Context(
        locale = AppLocale.UNITED_STATES,
        platform = platform,
        appVersion = Version.of(2, 1, 0),
        stableId = StableId.of("user-123")
    )

    val actual = AppFeatures.darkMode.evaluate(ctx)
    assertEquals(expected, actual)
}
```

---

## Testing Rule Matching

### AND Semantics

```kotlin
@Test
fun `rule matches only when all criteria match`() {
    // Feature definition
    val premiumFeature = object : Namespace("test") {
        val FEATURE by boolean<Context>(default = false) {
            rule(true) {
                platforms(Platform.IOS)
                locales(AppLocale.UNITED_STATES)
                versions { min(2, 0, 0) }
            }
        }
    }

    // All criteria match → rule matches
    val ctx1 = Context(
        locale = AppLocale.UNITED_STATES,
        platform = Platform.IOS,
        appVersion = Version.of(2, 1, 0),
        stableId = StableId.of("user-1")
    )
    assertTrue(premiumFeature.FEATURE.evaluate(ctx1))

    // Platform doesn't match → rule fails
    val ctx2 = ctx1.copy(platform = Platform.ANDROID)
    assertFalse(premiumFeature.FEATURE.evaluate(ctx2))

    // Locale doesn't match → rule fails
    val ctx3 = ctx1.copy(locale = AppLocale.FRANCE)
    assertFalse(premiumFeature.FEATURE.evaluate(ctx3))

    // Version doesn't match → rule fails
    val ctx4 = ctx1.copy(appVersion = Version.of(1, 9, 0))
    assertFalse(premiumFeature.FEATURE.evaluate(ctx4))
}
```

### Specificity Ordering

```kotlin
@Test
fun `most specific rule wins`() {
    val feature = object : Namespace("test") {
        val API_ENDPOINT by string<Context>(default = "https://api.example.com") {
            // Specificity = 2 (platform + locale)
            rule("https://api-ios-us.example.com") {
                platforms(Platform.IOS)
                locales(AppLocale.UNITED_STATES)
            }

            // Specificity = 1 (platform only)
            rule("https://api-ios.example.com") {
                platforms(Platform.IOS)
            }
        }
    }

    // iOS + US → most specific rule
    val ctx1 = Context(
        locale = AppLocale.UNITED_STATES,
        platform = Platform.IOS,
        appVersion = Version.of(2, 1, 0),
        stableId = StableId.of("user-1")
    )
    assertEquals("https://api-ios-us.example.com", feature.API_ENDPOINT.evaluate(ctx1))

    // iOS + other locale → less specific rule
    val ctx2 = ctx1.copy(locale = AppLocale.FRANCE)
    assertEquals("https://api-ios.example.com", feature.API_ENDPOINT.evaluate(ctx2))
}
```

---

## Testing Ramp-Ups

### Determinism Test

```kotlin
@Test
fun `ramp-up is deterministic for same context`() {
    val ctx = Context(
        locale = AppLocale.UNITED_STATES,
        platform = Platform.IOS,
        appVersion = Version.of(2, 1, 0),
        stableId = StableId.of("user-123")
    )

    val results = (1..100).map {
        AppFeatures.rampUpFlag.evaluate(ctx)
    }

    // All evaluations return the same value
    assertEquals(1, results.distinct().size)
}
```

### Distribution Test

```kotlin
@Test
fun `50 percent ramp-up distributes correctly`() {
    val feature = object : Namespace("test") {
        val FEATURE by boolean<Context>(default = false) {
            rule(true) { rampUp { 50.0 } }
        }
    }

    val sampleSize = 10_000
    val enabled = (0 until sampleSize).count { i ->
        val ctx = Context(
            locale = AppLocale.UNITED_STATES,
            platform = Platform.IOS,
            appVersion = Version.of(2, 1, 0),
            stableId = StableId.of(i.toString().padStart(32, '0'))
        )
        feature.FEATURE.evaluate(ctx)
    }

    val percentage = (enabled.toDouble() / sampleSize) * 100
    assertTrue(percentage in 48.0..52.0, "Ramp-up distribution is ${percentage}%, expected ~50%")
}
```

### Allowlist Test

```kotlin
@Test
fun `allowlisted users bypass ramp-up`() {
    val allowlistedId = StableId.of("tester-1")

    val feature = object : Namespace("test") {
        val FEATURE by boolean<Context>(default = false) {
            allowlist(allowlistedId)
            rule(true) { rampUp { 0.0 } }  // 0% ramp-up
        }
    }

    val ctx = Context(
        locale = AppLocale.UNITED_STATES,
        platform = Platform.IOS,
        appVersion = Version.of(2, 1, 0),
        stableId = allowlistedId
    )

    // Allowlisted user is in ramp-up despite 0%
    assertTrue(feature.FEATURE.evaluate(ctx))
}
```

---

## Testing Configuration Lifecycle

### Parse Success Test

```kotlin
@Test
fun `valid JSON loads successfully`() {
    val json = """
    {
      "flags": [
        {
          "key": "feature::app::darkMode",
          "defaultValue": { "type": "BOOLEAN", "value": false },
          "salt": "v1",
          "isActive": true,
          "rules": []
        }
      ]
    }
    """

    when (val result = AppFeatures.fromJson(json)) {
        is ParseResult.Success -> {
            // Success expected
        }
        is ParseResult.Failure -> {
            fail("Expected success, got: ${result.error}")
        }
    }
}
```

### Parse Failure Test

```kotlin
@Test
fun `invalid JSON is rejected`() {
    val json = """
    {
      "flags": [
        {
          "key": "feature::app::darkMode",
          "defaultValue": { "type": "STRING", "value": "invalid" }  // Type mismatch
        }
      ]
    }
    """

    when (val result = SnapshotSerializer.fromJson(json)) {
        is ParseResult.Success -> {
            fail("Expected failure, got success")
        }
        is ParseResult.Failure -> {
            // Failure expected
            assertIs<ParseError.TypeMismatch>(result.error)
        }
    }
}
```

### Rollback Test

```kotlin
@Test
fun `rollback restores previous config`() {
    val initialConfig = AppFeatures.configuration

    // Load new config
    val newJson = """{ "flags": [ ... ] }"""
    AppFeatures.fromJson(newJson)

    val updatedConfig = AppFeatures.configuration
    assertNotEquals(initialConfig, updatedConfig)

    // Rollback
    val success = AppFeatures.rollback(steps = 1)
    assertTrue(success)

    val rolledBackConfig = AppFeatures.configuration
    assertEquals(initialConfig, rolledBackConfig)
}
```

---

## Testing Custom Context Types

### Extension Predicate Test

```kotlin
@Test
fun `enterprise users with 100+ employees get feature`() {
    val ctx = EnterpriseContext(
        locale = AppLocale.UNITED_STATES,
        platform = Platform.IOS,
        appVersion = Version.of(2, 1, 0),
        stableId = StableId.of("user-123"),
        subscriptionTier = SubscriptionTier.ENTERPRISE,
        employeeCount = 150
    )

    val enabled = PremiumFeatures.ADVANCED_ANALYTICS.evaluate(ctx)
    assertTrue(enabled)
}

@Test
fun `enterprise users with fewer employees do not get feature`() {
    val ctx = EnterpriseContext(
        locale = AppLocale.UNITED_STATES,
        platform = Platform.IOS,
        appVersion = Version.of(2, 1, 0),
        stableId = StableId.of("user-456"),
        subscriptionTier = SubscriptionTier.ENTERPRISE,
        employeeCount = 50
    )

    val enabled = PremiumFeatures.ADVANCED_ANALYTICS.evaluate(ctx)
    assertFalse(enabled)
}
```

---

## Testing Observability

### Evaluation Hook Test

```kotlin
@Test
fun `evaluation hook is invoked`() {
    val events = mutableListOf<EvaluationEvent>()

    RegistryHooks.onEvaluationComplete = { event ->
        events.add(event)
    }

    val ctx = Context(
        locale = AppLocale.UNITED_STATES,
        platform = Platform.IOS,
        appVersion = Version.of(2, 1, 0),
        stableId = StableId.of("user-123")
    )

    AppFeatures.darkMode.evaluate(ctx)

    assertEquals(1, events.size)
    assertEquals(AppFeatures.darkMode.key, events[0].featureKey)
}
```

### Mismatch Detection Test

```kotlin
@Test
fun `shadow evaluation detects mismatches`() {
    val mismatches = mutableListOf<ShadowMismatch<*>>()

    val shadowOptions = ShadowEvaluationOptions(
        shadowRegistry = candidateRegistry,
        onMismatch = { mismatch ->
            mismatches.add(mismatch)
        }
    )

    val ctx = Context(
        locale = AppLocale.UNITED_STATES,
        platform = Platform.IOS,
        appVersion = Version.of(2, 1, 0),
        stableId = StableId.of("user-123")
    )

    AppFeatures.darkMode.evaluateWithShadow(ctx, shadowOptions)

    assertTrue(mismatches.size > 0)
}
```

---

## Test Fixtures

### Context Builder

```kotlin
object TestContexts {
    fun basic(
        locale: AppLocale = AppLocale.UNITED_STATES,
        platform: Platform = Platform.IOS,
        appVersion: Version = Version.of(2, 1, 0),
        stableId: StableId = StableId.of("test-user")
    ) = Context(locale, platform, appVersion, stableId)

    fun enterprise(
        subscriptionTier: SubscriptionTier = SubscriptionTier.ENTERPRISE,
        employeeCount: Int = 100
    ) = EnterpriseContext(
        locale = AppLocale.UNITED_STATES,
        platform = Platform.IOS,
        appVersion = Version.of(2, 1, 0),
        stableId = StableId.of("test-user"),
        subscriptionTier = subscriptionTier,
        employeeCount = employeeCount,
        accountAge = 365.days
    )
}

// Usage
@Test
fun `test with fixture`() {
    val ctx = TestContexts.basic(platform = Platform.ANDROID)
    val enabled = AppFeatures.darkMode.evaluate(ctx)
    assertFalse(enabled)
}
```

---

## Integration Testing

### End-to-End Flow

```kotlin
@Test
fun `end-to-end configuration lifecycle`() {
    // 1. Define features
    object TestFeatures : Namespace("test") {
        val FEATURE by boolean<Context>(default = false)
    }

    // 2. Export initial config
    val initialJson = TestFeatures.toJson()

    // 3. Load modified config
    val modifiedJson = initialJson.replace(""""value": false""", """"value": true""")
    when (val result = TestFeatures.fromJson(modifiedJson)) {
        is ParseResult.Success -> { /* Expected */ }
        is ParseResult.Failure -> fail("Load failed: ${result.error}")
    }

    // 4. Evaluate
    val ctx = TestContexts.basic()
    val enabled = TestFeatures.FEATURE.evaluate(ctx)
    assertTrue(enabled)

    // 5. Rollback
    TestFeatures.rollback(steps = 1)
    val rolledBack = TestFeatures.FEATURE.evaluate(ctx)
    assertFalse(rolledBack)
}
```

---

## Next Steps

- [Fundamentals: Evaluation Semantics](/fundamentals/evaluation-semantics) — Evaluation guarantees
- [Rules & Targeting: Rollout Strategies](/rules-and-targeting/rollout-strategies) — Ramp-up mechanics
- [Advanced: Custom Context Types](/advanced/custom-context-types) — Testing custom contexts
