# Engineering Deep Dive: Advanced Patterns

**Navigate**: [← Previous: Serialization](08-serialization.md) | [Next: Architecture Decisions →](10-architecture-decisions.md)

---

## Real-World Patterns

This chapter explores advanced patterns for using Konditional in production: custom contexts, reusable evaluables, multi-namespace architectures, remote configuration, testing strategies, and migration approaches.

---

## Pattern 1: Custom Context for Business Logic

### The Problem

Standard `Context` provides basic targeting (locale, platform, version, stableId). But your application has domain-specific requirements:
- User subscription tier
- Organization features
- A/B test assignments
- Custom user segments

### The Solution: Extend Context

```kotlin
data class AppContext(
    // Standard fields
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,

    // Business domain fields
    val userId: String?,
    val subscriptionTier: SubscriptionTier,
    val organizationId: String?,
    val betaPrograms: Set<String>,
    val isEmployee: Boolean,
    val accountCreatedAt: Instant
) : Context

enum class SubscriptionTier {
    FREE, BASIC, PREMIUM, ENTERPRISE
}
```

### Custom Evaluables

```kotlin
object PremiumUserEvaluable : Evaluable<AppContext> {
    override fun matches(context: AppContext): Boolean =
        context.subscriptionTier in setOf(
            SubscriptionTier.PREMIUM,
            SubscriptionTier.ENTERPRISE
        )

    override fun specificity(): Int = 1
}

object EmployeeEvaluable : Evaluable<AppContext> {
    override fun matches(context: AppContext): Boolean =
        context.isEmployee

    override fun specificity(): Int = 1
}

class BetaProgramEvaluable(private val programName: String) : Evaluable<AppContext> {
    override fun matches(context: AppContext): Boolean =
        programName in context.betaPrograms

    override fun specificity(): Int = 1

    override fun equals(other: Any?): Boolean =
        other is BetaProgramEvaluable && other.programName == programName

    override fun hashCode(): Int = programName.hashCode()
}
```

### Using Custom Evaluables

```kotlin
object Features : FeatureContainer<Namespace.Global>(Namespace.Global) {
    val ADVANCED_ANALYTICS by boolean<AppContext>(default = false) {
        rule {
            note = "Premium users only"
            extension(PremiumUserEvaluable)
            rollout { 100.0 }
        } returns true
    }

    val BETA_FEATURE by boolean<AppContext>(default = false) {
        rule {
            note = "Beta program participants"
            extension(BetaProgramEvaluable("new_dashboard"))
            rollout { 100.0 }
        } returns true

        rule {
            note = "Employees (dogfooding)"
            extension(EmployeeEvaluable)
            rollout { 100.0 }
        } returns true
    }
}
```

### Creating Contexts

```kotlin
fun createContext(user: User, device: Device): AppContext {
    return AppContext(
        // Standard
        locale = AppLocale.valueOf(user.preferredLanguage),
        platform = device.platform,
        appVersion = device.appVersion,
        stableId = StableId.of(user.id.toHexString()),

        // Custom
        userId = user.id,
        subscriptionTier = user.subscription.tier,
        organizationId = user.organizationId,
        betaPrograms = user.betaPrograms,
        isEmployee = user.email.endsWith("@company.com"),
        accountCreatedAt = user.createdAt
    )
}
```

**Benefits**:
- Type-safe business logic in flags
- Compiler enforces context requirements
- Reusable evaluables across features

---

## Pattern 2: Reusable Evaluable Library

### The Problem

Multiple flags need same targeting logic. Duplicating code is error-prone and hard to maintain.

### The Solution: Evaluable Library

```kotlin
object Evaluables {
    // Platform groups
    object Mobile : Evaluable<Context> {
        override fun matches(context: Context) =
            context.platform in setOf(Platform.IOS, Platform.ANDROID)
        override fun specificity() = 1
    }

    object Desktop : Evaluable<Context> {
        override fun matches(context: Context) =
            context.platform in setOf(Platform.DESKTOP, Platform.WEB)
        override fun specificity() = 1
    }

    // Locale groups
    object EnglishSpeaking : Evaluable<Context> {
        override fun matches(context: Context) =
            context.locale in setOf(
                AppLocale.EN_US, AppLocale.EN_CA, AppLocale.EN_GB
            )
        override fun specificity() = 1
    }

    object European : Evaluable<Context> {
        override fun matches(context: Context) =
            context.locale in setOf(
                AppLocale.EN_GB, AppLocale.FR_FR, AppLocale.DE_DE,
                AppLocale.ES_ES, AppLocale.IT_IT
            )
        override fun specificity() = 1
    }

    // Version checks
    class MinVersion(private val min: Version) : Evaluable<Context> {
        override fun matches(context: Context) =
            context.appVersion >= min
        override fun specificity() = 1
    }

    // Combinators
    class And<C : Context>(
        private val evaluables: List<Evaluable<C>>
    ) : Evaluable<C> {
        override fun matches(context: C) =
            evaluables.all { it.matches(context) }
        override fun specificity() =
            evaluables.sumOf { it.specificity() }
    }

    class Or<C : Context>(
        private val evaluables: List<Evaluable<C>>
    ) : Evaluable<C> {
        override fun matches(context: C) =
            evaluables.any { it.matches(context) }
        override fun specificity() =
            evaluables.maxOfOrNull { it.specificity() } ?: 0
    }

    class Not<C : Context>(
        private val evaluable: Evaluable<C>
    ) : Evaluable<C> {
        override fun matches(context: C) =
            !evaluable.matches(context)
        override fun specificity() =
            evaluable.specificity()
    }
}
```

### Using Reusable Evaluables

```kotlin
object Features : FeatureContainer<Namespace.Global>(Namespace.Global) {
    val MOBILE_FEATURE by boolean(default = false) {
        rule {
            extension(Evaluables.Mobile)
        } returns true
    }

    val DESKTOP_EUROPEAN by boolean(default = false) {
        rule {
            extension(
                Evaluables.And(
                    listOf(Evaluables.Desktop, Evaluables.European)
                )
            )
        } returns true
    }

    val MODERN_VERSION_ONLY by boolean(default = false) {
        rule {
            extension(Evaluables.MinVersion(Version(3, 0, 0)))
        } returns true
    }
}
```

**Benefits**:
- DRY (Don't Repeat Yourself)
- Consistent behavior across flags
- Easy to test evaluables independently
- Composable logic with And/Or/Not

---

## Pattern 3: Multi-Namespace Architecture

### The Problem

Large applications have multiple teams managing different domains. Need isolation between domains while sharing some configuration.

### The Solution: Namespace Per Domain

```kotlin
sealed class AppNamespace(id: String) : Namespace(id) {
    data object Core : AppNamespace("core")
    data object Payments : AppNamespace("payments")
    data object Messaging : AppNamespace("messaging")
    data object Analytics : AppNamespace("analytics")
}

// Core features (shared across app)
object CoreFeatures : FeatureContainer<AppNamespace.Core>(AppNamespace.Core) {
    val DARK_MODE by boolean(default = false)
    val NEW_NAVIGATION by boolean(default = false)
}

// Payments features (payments team)
object PaymentFeatures : FeatureContainer<AppNamespace.Payments>(AppNamespace.Payments) {
    val APPLE_PAY by boolean(default = false)
    val CRYPTO_PAYMENTS by boolean(default = false)
    val PAYMENT_TIMEOUT_MS by int(default = 30000)
}

// Messaging features (messaging team)
object MessagingFeatures : FeatureContainer<AppNamespace.Messaging>(AppNamespace.Messaging) {
    val REAL_TIME_MESSAGING by boolean(default = false)
    val MESSAGE_ENCRYPTION by boolean(default = true)
    val MAX_ATTACHMENT_SIZE_MB by int(default = 10)
}

// Analytics features (data team)
object AnalyticsFeatures : FeatureContainer<AppNamespace.Analytics>(AppNamespace.Analytics) {
    val ADVANCED_TRACKING by boolean(default = false)
    val SAMPLE_RATE by double(default = 1.0)
}
```

### Independent Updates

```kotlin
// Update payments configuration without affecting others
val paymentsConfig = fetchPaymentsConfigFromServer()
when (val result = SnapshotSerializer.fromJson(paymentsConfig)) {
    is ParseResult.Success -> AppNamespace.Payments.load(result.value)
    is ParseResult.Failure -> logger.error("Payments config failed: ${result.error}")
}

// Core, Messaging, Analytics unaffected
```

### Coordinated Rollout

```kotlin
// Rollout new feature that spans multiple namespaces
suspend fun rolloutUnifiedExperience(percentage: Double) {
    // Update all namespaces atomically
    val coreConfig = buildCoreConfig {
        CoreFeatures.NEW_NAVIGATION with { default(true) }
    }
    val paymentsConfig = buildPaymentsConfig {
        PaymentFeatures.APPLE_PAY with { default(true) }
    }
    val messagingConfig = buildMessagingConfig {
        MessagingFeatures.REAL_TIME_MESSAGING with { default(true) }
    }

    // Apply all at once
    AppNamespace.Core.load(coreConfig)
    AppNamespace.Payments.load(paymentsConfig)
    AppNamespace.Messaging.load(messagingConfig)

    logger.info("Unified experience rolled out at $percentage%")
}
```

**Benefits**:
- Team ownership (payments team manages PaymentFeatures)
- Reduced blast radius (payments failure doesn't affect messaging)
- Independent deployment (teams can ship at own pace)
- Clear boundaries (no accidental cross-namespace dependencies)

---

## Pattern 4: Remote Configuration Management

### The Problem

Configurations need to be updated without app deployment. Need server-driven configuration with client-side evaluation.

### The Solution: Configuration Service + Client Library

**Server**:
```kotlin
// Configuration service API
@RestController
class ConfigurationController {
    @GetMapping("/api/config/{namespace}")
    fun getConfiguration(@PathVariable namespace: String): ResponseEntity<String> {
        val config = configRepository.findByNamespace(namespace)
            ?: return ResponseEntity.notFound().build()

        val json = SnapshotSerializer.serialize(config)
        return ResponseEntity.ok(json)
    }

    @PostMapping("/api/config/{namespace}")
    fun updateConfiguration(
        @PathVariable namespace: String,
        @RequestBody json: String
    ): ResponseEntity<String> {
        when (val result = SnapshotSerializer.fromJson(json)) {
            is ParseResult.Success -> {
                configRepository.save(namespace, result.value)
                return ResponseEntity.ok("Updated")
            }
            is ParseResult.Failure -> {
                return ResponseEntity.badRequest()
                    .body("Parse error: ${result.error.message}")
            }
        }
    }
}
```

**Client**:
```kotlin
class RemoteConfigLoader(
    private val apiClient: ApiClient,
    private val refreshIntervalMs: Long = 60_000
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun startPolling(namespace: Namespace) {
        scope.launch {
            while (isActive) {
                try {
                    val json = apiClient.fetchConfiguration(namespace.id)
                    when (val result = SnapshotSerializer.fromJson(json)) {
                        is ParseResult.Success -> {
                            namespace.load(result.value)
                            logger.info("Config refreshed for ${namespace.id}")
                        }
                        is ParseResult.Failure -> {
                            logger.error("Config parse failed: ${result.error}")
                        }
                    }
                } catch (e: Exception) {
                    logger.error("Config fetch failed", e)
                }
                delay(refreshIntervalMs)
            }
        }
    }

    fun stop() {
        scope.cancel()
    }
}
```

**Usage**:
```kotlin
// App startup
val configLoader = RemoteConfigLoader(apiClient, refreshIntervalMs = 60_000)
configLoader.startPolling(AppNamespace.Core)
configLoader.startPolling(AppNamespace.Payments)
configLoader.startPolling(AppNamespace.Messaging)

// App shutdown
configLoader.stop()
```

### Optimizations

**Conditional GET** (only fetch if changed):
```kotlin
suspend fun fetchConfigurationIfChanged(namespace: String, etag: String?): ConfigResponse {
    val response = httpClient.get("/api/config/$namespace") {
        etag?.let { header("If-None-Match", it) }
    }

    return when (response.status) {
        HttpStatusCode.OK -> ConfigResponse.Updated(
            json = response.bodyAsText(),
            etag = response.headers["ETag"]
        )
        HttpStatusCode.NotModified -> ConfigResponse.NotModified
        else -> ConfigResponse.Error(response.status)
    }
}
```

**Incremental patches**:
```kotlin
// Server sends only changes
@GetMapping("/api/config/{namespace}/patch")
fun getConfigurationPatch(
    @PathVariable namespace: String,
    @RequestParam since: Long
): ResponseEntity<String> {
    val patch = configRepository.getPatchesSince(namespace, since)
    val json = SnapshotSerializer.serializePatch(patch)
    return ResponseEntity.ok(json)
}

// Client applies patch
val patchJson = apiClient.fetchPatch(namespace.id, lastUpdateTime)
val currentConfig = namespace.configuration()
when (val result = SnapshotSerializer.applyPatchJson(currentConfig, patchJson)) {
    is ParseResult.Success -> namespace.load(result.value)
    is ParseResult.Failure -> logger.error("Patch failed: ${result.error}")
}
```

**Benefits**:
- Configuration updates without app deployment
- Reduced bandwidth (conditional GET, patches)
- Real-time feature control
- A/B test adjustments

---

## Pattern 5: Testing Strategies

### Unit Testing Features

```kotlin
class FeatureTests {
    @Test
    fun `premium feature enabled for premium users`() {
        // Arrange
        val context = AppContext(
            locale = AppLocale.EN_US,
            platform = Platform.IOS,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("a".repeat(32)),
            subscriptionTier = SubscriptionTier.PREMIUM,
            // ... other fields
        )

        // Act
        val enabled = context.evaluate(Features.ADVANCED_ANALYTICS)

        // Assert
        assertTrue(enabled)
    }

    @Test
    fun `premium feature disabled for free users`() {
        val context = createContext(subscriptionTier = SubscriptionTier.FREE)
        val enabled = context.evaluate(Features.ADVANCED_ANALYTICS)
        assertFalse(enabled)
    }
}
```

### Integration Testing with Registry

```kotlin
class RegistryIntegrationTests {
    private lateinit var testRegistry: InMemoryNamespaceRegistry

    @BeforeEach
    fun setup() {
        testRegistry = InMemoryNamespaceRegistry()
    }

    @Test
    fun `configuration loading updates registry`() {
        // Arrange
        val json = """
        {
          "flags": [
            {
              "key": "TEST_FEATURE",
              "type": "boolean",
              "default": {"value": true}
            }
          ]
        }
        """

        // Act
        when (val result = SnapshotSerializer.fromJson(json)) {
            is ParseResult.Success -> testRegistry.load(result.value)
            is ParseResult.Failure -> fail("Should parse successfully")
        }

        // Assert
        val config = testRegistry.configuration
        assertNotNull(config.flags)
    }
}
```

### Testing Rollout Bucketing

```kotlin
class BucketingTests {
    @Test
    fun `rollout percentage is accurate`() {
        val targetPercentage = 10.0
        val numUsers = 10_000
        val userIds = (1..numUsers).map { StableId.of("user$it".padEnd(32, '0')) }

        val contexts = userIds.map { id ->
            Context(
                locale = AppLocale.EN_US,
                platform = Platform.IOS,
                appVersion = Version(1, 0, 0),
                stableId = id
            )
        }

        val enabledCount = contexts.count { ctx ->
            ctx.evaluate(TestFeatures.ROLLOUT_FEATURE)
        }

        val actualPercentage = (enabledCount.toDouble() / numUsers) * 100.0

        // Allow 1% margin of error
        assertEquals(targetPercentage, actualPercentage, delta = 1.0)
    }

    @Test
    fun `same user gets same bucket across sessions`() {
        val userId = StableId.of("a".repeat(32))
        val context1 = createContext(stableId = userId)
        val context2 = createContext(stableId = userId)

        val result1 = context1.evaluate(Features.ROLLOUT_FEATURE)
        val result2 = context2.evaluate(Features.ROLLOUT_FEATURE)

        assertEquals(result1, result2)
    }
}
```

### Mock Contexts for Testing

```kotlin
object TestContexts {
    fun default() = Context(
        locale = AppLocale.EN_US,
        platform = Platform.IOS,
        appVersion = Version(1, 0, 0),
        stableId = StableId.of("test".repeat(8))
    )

    fun iosUser() = default().copy(platform = Platform.IOS)
    fun androidUser() = default().copy(platform = Platform.ANDROID)
    fun webUser() = default().copy(platform = Platform.WEB)

    fun oldVersion() = default().copy(appVersion = Version(0, 9, 0))
    fun newVersion() = default().copy(appVersion = Version(2, 0, 0))

    fun withLocale(locale: AppLocale) = default().copy(locale = locale)
    fun withId(id: String) = default().copy(stableId = StableId.of(id.padEnd(32, '0')))
}

// Usage in tests
@Test
fun `feature enabled on iOS`() {
    val enabled = TestContexts.iosUser().evaluate(Features.IOS_FEATURE)
    assertTrue(enabled)
}
```

**Benefits**:
- Fast tests (in-memory registry)
- Deterministic (no network, no randomness)
- Comprehensive (test all code paths)

---

## Pattern 6: Migration from Legacy System

### The Problem

Existing app uses different feature flag system. Need gradual migration without disruption.

### The Solution: Adapter Pattern

**Legacy system**:
```kotlin
// Old feature flag API
object LegacyFlags {
    fun isEnabled(key: String, userId: String): Boolean
}
```

**Adapter**:
```kotlin
class KonditionalAdapter(
    private val legacyFlags: LegacyFlags,
    private val namespace: Namespace
) {
    fun isEnabled(key: String, userId: String): Boolean {
        // Try Konditional first
        val feature = findFeatureByKey(key)
        if (feature != null) {
            val context = createContext(userId)
            return context.evaluate(feature)
        }

        // Fallback to legacy
        return legacyFlags.isEnabled(key, userId)
    }

    private fun findFeatureByKey(key: String): BooleanFeature<Context, Namespace>? {
        val config = namespace.configuration()
        return config.flags.keys
            .filterIsInstance<BooleanFeature<Context, Namespace>>()
            .find { it.key == key }
    }

    private fun createContext(userId: String): Context {
        // Create context from userId
        return Context(
            locale = AppLocale.EN_US,
            platform = Platform.IOS,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of(userId.padEnd(32, '0'))
        )
    }
}
```

**Gradual migration**:
```kotlin
// Phase 1: Both systems in parallel
val adapter = KonditionalAdapter(legacyFlags, AppNamespace.Core)

// Migrate flags one at a time
object MigratedFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
    val NEW_UI by boolean(default = false) {
        // Define in Konditional
    }
}

// Phase 2: App code uses adapter
if (adapter.isEnabled("NEW_UI", userId)) {
    showNewUI()
}

// Phase 3: All flags migrated, remove adapter and legacy system
val enabled = context.evaluate(MigratedFeatures.NEW_UI)
```

**Benefits**:
- No big-bang migration
- Rollback capability (if issue, remove from Konditional)
- Side-by-side comparison (validate behavior matches)

---

## Pattern 7: Feature Flag Analytics

### The Problem

Need to track which users see which feature variants for analysis.

### The Solution: Evaluation Hooks

```kotlin
class AnalyticsTracker {
    fun trackEvaluation(
        feature: Feature<*, *, *, *>,
        context: Context,
        result: Any
    ) {
        analytics.track("feature_evaluated", mapOf(
            "feature_key" to feature.key,
            "feature_namespace" to feature.namespace.id,
            "user_id" to context.stableId.hexId.id,
            "platform" to context.platform.name,
            "locale" to context.locale.name,
            "app_version" to context.appVersion.toString(),
            "result" to result.toString()
        ))
    }
}

// Extension function to add tracking
fun <T : Any> Context.evaluateWithTracking(
    feature: Feature<*, T, *, *>,
    tracker: AnalyticsTracker
): T {
    val result = this.evaluate(feature)
    tracker.trackEvaluation(feature, this, result)
    return result
}

// Usage
val enabled = context.evaluateWithTracking(Features.NEW_UI, analyticsTracker)
```

### Aggregate Metrics

```kotlin
class FeatureFlagMetrics {
    private val evaluations = ConcurrentHashMap<String, AtomicLong>()
    private val enabledCounts = ConcurrentHashMap<String, AtomicLong>()

    fun recordEvaluation(featureKey: String, enabled: Boolean) {
        evaluations.computeIfAbsent(featureKey) { AtomicLong() }.incrementAndGet()
        if (enabled) {
            enabledCounts.computeIfAbsent(featureKey) { AtomicLong() }.incrementAndGet()
        }
    }

    fun getMetrics(featureKey: String): FeatureMetrics {
        val total = evaluations[featureKey]?.get() ?: 0
        val enabled = enabledCounts[featureKey]?.get() ?: 0
        return FeatureMetrics(
            evaluations = total,
            enabled = enabled,
            enabledPercentage = if (total > 0) (enabled.toDouble() / total) * 100.0 else 0.0
        )
    }
}

data class FeatureMetrics(
    val evaluations: Long,
    val enabled: Long,
    val enabledPercentage: Double
)
```

**Benefits**:
- Track actual rollout percentages
- Identify unused features
- Debug rollout issues
- Compliance and auditing

---

## Pattern 8: Feature Flag Lifecycle Management

### The Problem

Feature flags accumulate over time. Need process to clean up old flags.

### The Solution: Flag Lifecycle Tracking

```kotlin
data class FeatureMetadata(
    val key: String,
    val createdAt: Instant,
    val createdBy: String,
    val purpose: String,
    val expectedRemovalDate: Instant?,
    val status: FeatureStatus
)

enum class FeatureStatus {
    DEVELOPMENT,   // Being built
    TESTING,       // In QA
    ROLLING_OUT,   // Gradual rollout
    ENABLED,       // 100% enabled
    DEPRECATED,    // Scheduled for removal
    REMOVED        // Deleted from code
}

object FeatureRegistry {
    private val metadata = mapOf(
        "NEW_UI" to FeatureMetadata(
            key = "NEW_UI",
            createdAt = Instant.parse("2024-01-15T00:00:00Z"),
            createdBy = "ui-team",
            purpose = "Rollout redesigned user interface",
            expectedRemovalDate = Instant.parse("2024-06-01T00:00:00Z"),
            status = FeatureStatus.ROLLING_OUT
        ),
        "OLD_PAYMENT_FLOW" to FeatureMetadata(
            key = "OLD_PAYMENT_FLOW",
            createdAt = Instant.parse("2023-06-01T00:00:00Z"),
            createdBy = "payments-team",
            purpose = "Temporary fallback for payment issues",
            expectedRemovalDate = Instant.parse("2024-03-01T00:00:00Z"),
            status = FeatureStatus.DEPRECATED
        )
    )

    fun getFlagsDueForRemoval(): List<FeatureMetadata> {
        val now = Instant.now()
        return metadata.values.filter {
            it.expectedRemovalDate?.isBefore(now) == true
        }
    }

    fun getDeprecatedFlags(): List<FeatureMetadata> =
        metadata.values.filter { it.status == FeatureStatus.DEPRECATED }
}
```

### Automated Cleanup Warnings

```kotlin
@Test
fun `flag cleanup check`() {
    val flagsDueForRemoval = FeatureRegistry.getFlagsDueForRemoval()

    if (flagsDueForRemoval.isNotEmpty()) {
        val message = flagsDueForRemoval.joinToString("\n") {
            "Flag '${it.key}' should be removed (due date: ${it.expectedRemovalDate})"
        }
        println("⚠️  FLAGS DUE FOR REMOVAL:\n$message")
    }
}
```

**Benefits**:
- Prevent flag accumulation
- Clear ownership and timelines
- Automated reminders
- Documentation of intent

---

## Review: Advanced Patterns

### Custom Contexts
- Extend `Context` with domain-specific fields
- Create reusable evaluables for business logic
- Type-safe targeting beyond standard dimensions

### Reusable Evaluables
- Build library of common evaluables
- Use combinators (And, Or, Not)
- Maintain DRY principle

### Multi-Namespace Architecture
- Namespace per domain for team ownership
- Independent updates reduce blast radius
- Coordinated rollouts when needed

### Remote Configuration
- Server-driven configuration updates
- Polling or push-based refresh
- Conditional GET and patches for efficiency

### Testing
- Unit test features with mock contexts
- Integration test with in-memory registry
- Validate rollout percentages and bucketing

### Migration
- Adapter pattern for gradual migration
- Side-by-side comparison with legacy system
- No big-bang cutover

### Analytics & Lifecycle
- Track evaluations for metrics
- Lifecycle management to prevent accumulation
- Automated cleanup reminders

---

## Next Steps

Now that you've seen advanced patterns, we can explore the architectural decisions behind Konditional's design.

**Next chapter**: [Architecture Decisions](10-architecture-decisions.md)
- Why property delegation over builder pattern
- Why sealed interfaces over open classes
- Why immutability is central
- Why SHA-256 over simpler hashing
- Why atomic references over locks
- Why parse-don't-validate
- Trade-offs and alternatives considered

Understanding the "why" behind the design helps you make informed decisions in your own systems.

---

**Navigate**: [← Previous: Serialization](08-serialization.md) | [Next: Architecture Decisions →](10-architecture-decisions.md)
