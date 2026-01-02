# Multiple Namespaces

When and how to use namespace isolation for team ownership, failure isolation, and independent lifecycles.

---

## When to Use Multiple Namespaces

### 1. Team Ownership Boundaries

Different teams own different feature domains:

```kotlin
sealed class TeamDomain(id: String) : Namespace(id) {
    data object Recommendations : TeamDomain("recommendations") {
        val COLLABORATIVE_FILTERING by boolean<Context>(default = true)
        val CONTENT_BASED_FILTERING by boolean<Context>(default = false)
        val HYBRID_APPROACH by boolean<Context>(default = false)
    }

    data object Search : TeamDomain("search") {
        val FUZZY_MATCHING by boolean<Context>(default = true)
        val AUTOCOMPLETE by boolean<Context>(default = true)
        val QUERY_SUGGESTIONS by boolean<Context>(default = false)
    }

    data object Personalization : TeamDomain("personalization") {
        val USER_HISTORY_ENABLED by boolean<Context>(default = true)
        val PREFERENCE_LEARNING by boolean<Context>(default = false)
    }
}
```

**Benefits:**
- Recommendations team controls `recommendations` namespace independently
- No coordination required for config updates
- Clear ownership via code structure

### 2. Different Update Frequencies

Separate fast-changing experiments from stable infrastructure:

```kotlin
object ExperimentFlags : Namespace("experiments") {
    // Updated daily: A/B tests, feature rollouts
    val NEW_CHECKOUT_FLOW by boolean<Context>(default = false)
    val RECOMMENDATION_ALGORITHM by string<Context>(default = "v1")
}

object InfrastructureFlags : Namespace("infrastructure") {
    // Updated rarely: circuit breakers, kill switches
    val PAYMENT_GATEWAY_ENABLED by boolean<Context>(default = true)
    val DATABASE_REPLICA_READS by boolean<Context>(default = false)
}

object FeatureToggles : Namespace("features") {
    // Updated occasionally: long-lived feature gates
    val DARK_MODE_AVAILABLE by boolean<Context>(default = true)
    val SOCIAL_LOGIN_ENABLED by boolean<Context>(default = true)
}
```

**Benefits:**
- Experiment changes don't risk infrastructure stability
- Infrastructure flags have higher review standards
- Clear SLA expectations per namespace

### 3. Failure Isolation

Isolate critical path from analytics/observability:

```kotlin
object CriticalPath : Namespace("critical") {
    val PAYMENT_PROCESSING_ENABLED by boolean<Context>(default = true)
    val ORDER_FULFILLMENT_ENABLED by boolean<Context>(default = true)
}

object Analytics : Namespace("analytics") {
    val EVENT_TRACKING_ENABLED by boolean<Context>(default = false)
    val PERFORMANCE_MONITORING by boolean<Context>(default = false)
}

object Marketing : Namespace("marketing") {
    val PROMOTIONAL_BANNERS by boolean<Context>(default = false)
    val CROSS_SELL_RECOMMENDATIONS by boolean<Context>(default = false)
}
```

**Benefits:**
- Analytics config error doesn't break checkout
- Critical path config has highest priority/review
- Marketing experiments can fail independently

---

## Governance Patterns

### Pattern 1: Sealed Hierarchy (Compile-Time Exhaustiveness)

```kotlin
sealed class AppDomain(id: String) : Namespace(id) {
    data object Auth : AppDomain("auth") {
        val SOCIAL_LOGIN by boolean<Context>(default = false)
    }

    data object Payments : AppDomain("payments") {
        val APPLE_PAY by boolean<Context>(default = false)
    }

    data object Analytics : AppDomain("analytics") {
        val TRACKING_ENABLED by boolean<Context>(default = false)
    }
}

// All namespaces discoverable via sealed hierarchy
val allNamespaces: List<Namespace> = listOf(
    AppDomain.Auth,
    AppDomain.Payments,
    AppDomain.Analytics
)
```

**Benefits:**
- Compiler enforces exhaustiveness (can't forget a namespace)
- IDE autocomplete shows all namespaces
- Clear inventory of all isolation boundaries

### Pattern 2: Package Structure Mirrors Team Structure

```
src/main/kotlin/
  com/example/teams/
    auth/
      AuthFeatures.kt : Namespace("auth")
    payments/
      PaymentFeatures.kt : Namespace("payments")
    analytics/
      AnalyticsFeatures.kt : Namespace("analytics")
```

**Benefits:**
- Code ownership via CODEOWNERS file maps to namespaces
- Team boundaries are structural (package-level)
- Easier to enforce review policies

### Pattern 3: Namespace Registry Per Environment

```kotlin
enum class Environment { PROD, STAGE, DEV }

class EnvironmentAwareNamespace(
    baseId: String,
    env: Environment
) : Namespace("$baseId-${env.name.lowercase()}")

val prodAuth = EnvironmentAwareNamespace("auth", Environment.PROD)
val stageAuth = EnvironmentAwareNamespace("auth", Environment.STAGE)
```

**Use case:** Different configurations per environment while sharing code.

---

## Independent Lifecycle Management

### Loading Configurations Independently

```kotlin
// Each namespace loads its own configuration
fun loadAllConfigs() {
    // Load Auth config
    when (val result = fetchAuthConfig()) {
        is ConfigFetchResult.Success -> {
            NamespaceSnapshotLoader(AppDomain.Auth).load(result.json)
        }
        is ConfigFetchResult.Failure -> {
            logger.error("Auth config fetch failed")
            // Auth uses last-known-good, other namespaces unaffected
        }
    }

    // Load Payments config (independent of Auth)
    when (val result = fetchPaymentsConfig()) {
        is ConfigFetchResult.Success -> {
            NamespaceSnapshotLoader(AppDomain.Payments).load(result.json)
        }
        is ConfigFetchResult.Failure -> {
            logger.error("Payments config fetch failed")
            // Payments uses last-known-good, Auth unaffected
        }
    }
}
```

### Rollback Per Namespace

```kotlin
// Rollback only Payments namespace
val success = AppDomain.Payments.rollback(steps = 1)

if (success) {
    logger.info("Payments rolled back to previous config")
    // Auth and Analytics unaffected
}
```

### Kill-Switch Per Namespace

```kotlin
// Emergency: disable all experiments
ExperimentFlags.disableAll()

// Critical path and infrastructure continue normally
val paymentEnabled = CriticalPath.PAYMENT_PROCESSING_ENABLED.evaluate(context)  // Normal evaluation
```

---

## Coordination Patterns

### Pattern 1: Centralized Config Loader

```kotlin
object ConfigLoader {
    private val configSources = mapOf(
        AppDomain.Auth to "s3://configs/auth.json",
        AppDomain.Payments to "s3://configs/payments.json",
        AppDomain.Analytics to "s3://configs/analytics.json"
    )

    suspend fun loadAll() {
        configSources.forEach { (namespace, source) ->
            launch {
                val json = fetchFromSource(source)
                when (val result = namespace.fromJson(json)) {
                    is ParseResult.Success -> logger.info("Loaded ${namespace.id}")
                    is ParseResult.Failure -> logger.error("Failed ${namespace.id}: ${result.error}")
                }
            }
        }
    }
}
```

### Pattern 2: Per-Namespace Polling

```kotlin
class NamespaceConfigPoller(
    private val namespace: Namespace,
    private val source: ConfigSource,
    private val interval: Duration
) {
    fun start() = CoroutineScope(Dispatchers.IO).launch {
        while (isActive) {
            val json = source.fetch()
            when (val result = namespace.fromJson(json)) {
                is ParseResult.Success -> logger.debug("${namespace.id} updated")
                is ParseResult.Failure -> logger.error("${namespace.id} parse failed")
            }
            delay(interval)
        }
    }
}

// Start pollers for each namespace
val authPoller = NamespaceConfigPoller(AppDomain.Auth, authSource, 30.seconds)
val paymentsPoller = NamespaceConfigPoller(AppDomain.Payments, paymentSource, 60.seconds)

authPoller.start()
paymentsPoller.start()
```

---

## Observability Per Namespace

### Namespace-Scoped Metrics

```kotlin
val metricsCollector = object : MetricsCollector {
    override fun recordConfigLoad(event: Metrics.ConfigLoadMetric) {
        metrics.gauge(
            "konditional.flags.count",
            event.featureCount.toDouble(),
            mapOf("namespaceId" to event.namespaceId),
        )
    }

    override fun recordEvaluation(event: Metrics.Evaluation) {
        metrics.histogram(
            "konditional.evaluation.durationNanos",
            event.durationNanos.toDouble(),
            mapOf(
                "namespaceId" to event.namespaceId,
                "featureKey" to event.featureKey,
                "mode" to event.mode.name,
                "decision" to event.decision.name,
            ),
        )
    }
}

AppDomain.Auth.setHooks(RegistryHooks.of(metrics = metricsCollector))
AppDomain.Payments.setHooks(RegistryHooks.of(metrics = metricsCollector))
```

### Namespace-Scoped Alerting

```kotlin
// Parse failures are reported at the parse/load boundary (not via RegistryHooks).
when (val result = namespace.fromJson(json)) {
    is ParseResult.Success -> logger.debug("${namespace.id} updated")
    is ParseResult.Failure -> {
        val severity = when (namespace.id) {
            "critical" -> AlertSeverity.CRITICAL
            "infrastructure" -> AlertSeverity.HIGH
            "experiments" -> AlertSeverity.LOW
            else -> AlertSeverity.MEDIUM
        }

        alerting.notify(
            message = "Config parse failed for ${namespace.id}",
            severity = severity,
            error = result.error,
        )
    }
}
```

---

## Anti-Patterns

### Anti-Pattern 1: Over-Segmentation

**Don't:**

```kotlin
object AuthSocialLogin : Namespace("auth-social-login")
object AuthTwoFactor : Namespace("auth-two-factor")
object AuthPasswordReset : Namespace("auth-password-reset")
object AuthEmailVerification : Namespace("auth-email-verification")
```

**Issues:**
- Too many namespaces increase complexity
- No isolation benefit (same team owns all)
- More config files to manage

**Better:**

```kotlin
object Auth : Namespace("auth") {
    val SOCIAL_LOGIN by boolean<Context>(default = false)
    val TWO_FACTOR_AUTH by boolean<Context>(default = true)
    val PASSWORD_RESET_ENABLED by boolean<Context>(default = true)
    val EMAIL_VERIFICATION_REQUIRED by boolean<Context>(default = true)
}
```

### Anti-Pattern 2: Namespace Per Feature

**Don't:**

```kotlin
object DarkModeNamespace : Namespace("dark-mode")
object ApiEndpointNamespace : Namespace("api-endpoint")
object MaxRetriesNamespace : Namespace("max-retries")
```

**Issues:**
- Defeats the purpose of namespaces (isolation)
- Each feature has separate config file
- No logical grouping

**Better:**

```kotlin
object AppConfig : Namespace("app") {
    val DARK_MODE by boolean<Context>(default = false)
    val API_ENDPOINT by string<Context>(default = "https://api.example.com")
    val MAX_RETRIES by integer<Context>(default = 3)
}
```

---

## Next Steps

- [Theory: Namespace Isolation](/theory/namespace-isolation) — Formal guarantees
- [API Reference: Namespace Operations](/api-reference/namespace-operations) — Lifecycle API
- [Fundamentals: Core Primitives](/fundamentals/core-primitives) — Namespace primitive
