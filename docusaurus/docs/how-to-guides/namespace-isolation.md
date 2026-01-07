# How-To: Organize Features with Namespace Isolation

## Problem

You need to:

- Organize features by team, domain, or update frequency
- Allow independent configuration updates per team/domain
- Isolate configuration failures to specific namespaces
- Maintain clear ownership boundaries in code

## Solution

### Step 1: Identify Isolation Boundaries

Choose namespace boundaries based on:

**1. Team ownership:**

- Recommendations team owns recommendation features
- Search team owns search features
- Independent updates, no coordination needed

**2. Update frequency:**

- Experiments (updated daily)
- Infrastructure flags (updated rarely, high review standards)
- Feature toggles (updated occasionally)

**3. Failure isolation:**

- Critical path (payments, orders)
- Analytics (tracking, monitoring)

### Step 2: Define Namespaces

```kotlin
sealed class AppDomain(id: String) : Namespace(id) {
  // Recommendations team
  data object Recommendations : AppDomain("recommendations") {
    val collaborativeFiltering by boolean<Context>(default = true)
    val contentBasedFiltering by boolean<Context>(default = false)
    val hybridApproach by boolean<Context>(default = false)
  }

  // Search team
  data object Search : AppDomain("search") {
    val fuzzyMatching by boolean<Context>(default = true)
    val autocomplete by boolean<Context>(default = true)
    val querySuggestions by boolean<Context>(default = false)
  }

  // Personalization team
  data object Personalization : AppDomain("personalization") {
    val userHistoryEnabled by boolean<Context>(default = true)
    val preferenceLearning by boolean<Context>(default = false)
  }
}
```

**Benefits:**

- Each `data object` is an independent namespace
- Sealed class provides type-safe enumeration
- Clear ownership in code structure

### Step 3: Load Configuration Independently

```kotlin
class MultiNamespaceLoader {
  fun loadAllConfigurations() {
    // Load each namespace independently
    loadNamespace(AppDomain.Recommendations, "recommendations-config.json")
    loadNamespace(AppDomain.Search, "search-config.json")
    loadNamespace(AppDomain.Personalization, "personalization-config.json")
  }

  private fun loadNamespace(
      namespace: Namespace,
      configFile: String
  ) {
    try {
      val json = fetchConfig(configFile)
      when (val result = NamespaceSnapshotLoader(namespace).load(json)) {
        is ParseResult.Success -> {
          logger.info("Loaded ${namespace.id} config")
        }
        is ParseResult.Failure -> {
          logger.error("Failed to load ${namespace.id}: ${result.error}")
          // Other namespaces unaffected
        }
      }
    } catch (e: Exception) {
      logger.error("Error loading ${namespace.id}", e)
      // Other namespaces unaffected
    }
  }
}
```

**Key insight:** Failure in one namespace doesn't affect others. Recommendations config failure doesn't break Search.

### Step 4: Evaluate from Correct Namespace

```kotlin
fun buildUserExperience(ctx: Context): UserExperience {
  return UserExperience(
      // Recommendations namespace
      recommendations = getRecommendations(
          collaborative = AppDomain.Recommendations.collaborativeFiltering.evaluate(ctx),
          contentBased = AppDomain.Recommendations.contentBasedFiltering.evaluate(ctx)
      ),

      // Search namespace
      searchResults = performSearch(
          fuzzy = AppDomain.Search.fuzzyMatching.evaluate(ctx),
          autocomplete = AppDomain.Search.autocomplete.evaluate(ctx)
      ),

      // Personalization namespace
      personalization = buildPersonalization(
          history = AppDomain.Personalization.userHistoryEnabled.evaluate(ctx),
          learning = AppDomain.Personalization.preferenceLearning.evaluate(ctx)
      )
  )
}
```

## Guarantees

- **Namespace isolation**: Configuration in one namespace doesn't affect others
  - **Mechanism**: Each namespace maintains independent configuration state
  - **Boundary**: Namespaces share the same evaluation context type

- **Independent updates**: Load configuration for one namespace without affecting others
  - **Mechanism**: `NamespaceSnapshotLoader` targets a specific namespace
  - **Boundary**: Must use correct namespace reference when loading

- **Failure isolation**: Failed load in one namespace doesn't affect others
  - **Mechanism**: Each namespace's state is independent
  - **Boundary**: Application logic must handle partial availability

## Common Patterns

### Pattern 1: Update Frequency Separation

```kotlin
// Updated daily: experiments
object Experiments : Namespace("experiments") {
  val newCheckoutFlow by boolean<Context>(default = false)
  val recommendationAlgorithm by string<Context>(default = "v1")
}

// Updated rarely: infrastructure
object Infrastructure : Namespace("infra") {
  val paymentGatewayEnabled by boolean<Context>(default = true)
  val databaseReplicaReads by boolean<Context>(default = false)
}

// Updated occasionally: features
object Features : Namespace("features") {
  val darkModeAvailable by boolean<Context>(default = true)
  val socialLoginEnabled by boolean<Context>(default = true)
}
```

**Use when:**

- Experiment configs change frequently, infrastructure rarely
- You want different approval processes (experiments: self-service, infrastructure: architecture review)

### Pattern 2: Critical Path Isolation

```kotlin
// Critical: cannot fail
object CriticalPath : Namespace("critical") {
  val paymentProcessingEnabled by boolean<Context>(default = true)
  val orderFulfillmentEnabled by boolean<Context>(default = true)
}

// Non-critical: failures acceptable
object Analytics : Namespace("analytics") {
  val eventTrackingEnabled by boolean<Context>(default = false)
  val performanceMonitoring by boolean<Context>(default = false)
}

// Evaluation
fun processOrder(ctx: Context) {
  // Critical path: must work
  if (CriticalPath.paymentProcessingEnabled.evaluate(ctx)) {
    processPayment()
  } else {
    // Fail fast: cannot process order without payment
    throw PaymentDisabledException()
  }

  // Non-critical: safe to skip
  if (Analytics.eventTrackingEnabled.evaluate(ctx)) {
    try {
      trackEvent("order_processed")
    } catch (e: Exception) {
      logger.warn("Analytics failed, continuing", e)
      // Order processing continues
    }
  }
}
```

**Use when:**

- You want to isolate critical functionality from analytics/monitoring
- Analytics config failures shouldn't affect core business logic

### Pattern 3: Team Ownership with Versioned Config

```kotlin
sealed class TeamNamespace(
    id: String,
    val team: String
) : Namespace(id) {
  data object Payments : TeamNamespace("payments", "payments-team") {
    val stripeEnabled by boolean<Context>(default = true)
    val paypalEnabled by boolean<Context>(default = false)
  }

  data object Fulfillment : TeamNamespace("fulfillment", "fulfillment-team") {
    val autoShipEnabled by boolean<Context>(default = true)
    val sameDayDelivery by boolean<Context>(default = false)
  }
}

class TeamConfigLoader {
  fun loadTeamConfig(namespace: TeamNamespace) {
    val configUrl = "https://config.example.com/${namespace.team}/${namespace.id}.json"
    val json = httpClient.get(configUrl).body<String>()

    when (val result = NamespaceSnapshotLoader(namespace).load(json)) {
      is ParseResult.Success -> {
        logger.info("${namespace.team} config loaded")
        notifyTeam(namespace.team, "Config loaded successfully")
      }
      is ParseResult.Failure -> {
        logger.error("${namespace.team} config failed: ${result.error}")
        alertTeam(namespace.team, "Config load failed", result.error)
      }
    }
  }
}
```

## What Can Go Wrong?

### Loading Config to Wrong Namespace

```kotlin
// ✗ DON'T: Load recommendations config to search namespace
val recommendationsJson = fetchConfig("recommendations-config.json")
NamespaceSnapshotLoader(AppDomain.Search).load(recommendationsJson)
// Result: ParseError.UnknownFeature (recommendations features don't exist in Search)

// ✓ DO: Load to correct namespace
NamespaceSnapshotLoader(AppDomain.Recommendations).load(recommendationsJson)
```

### Evaluating from Wrong Namespace

```kotlin
// ✗ DON'T: Evaluate from wrong namespace
val enabled = AppDomain.Search.fuzzyMatching.evaluate(ctx)
// But you wanted: AppDomain.Recommendations.collaborativeFiltering

// ✓ DO: Explicitly reference correct namespace
val enabled = AppDomain.Recommendations.collaborativeFiltering.evaluate(ctx)
```

**Mitigation:** Use sealed classes or enums to enumerate namespaces. Compiler catches typos.

### Not Handling Partial Namespace Failures

```kotlin
// ✗ DON'T: Assume all namespaces loaded
fun buildExperience(ctx: Context): Experience {
  return Experience(
      recs = AppDomain.Recommendations.collaborativeFiltering.evaluate(ctx),
      search = AppDomain.Search.fuzzyMatching.evaluate(ctx)
  )
  // If Recommendations config failed to load, features use defaults (might not be desired)
}

// ✓ DO: Explicitly handle namespace availability
fun buildExperience(ctx: Context): Experience {
  val recsAvailable = checkNamespaceHealth(AppDomain.Recommendations)
  val searchAvailable = checkNamespaceHealth(AppDomain.Search)

  return Experience(
      recs = if (recsAvailable) {
        AppDomain.Recommendations.collaborativeFiltering.evaluate(ctx)
      } else {
        fallbackRecommendations()
      },
      search = if (searchAvailable) {
        AppDomain.Search.fuzzyMatching.evaluate(ctx)
      } else {
        fallbackSearch()
      }
  )
}
```

## Testing Multiple Namespaces

### Test Namespace Isolation

```kotlin
@Test
fun `failed load in one namespace does not affect others`() {
  // Load valid config to Recommendations
  val validJson = """{ "collaborativeFiltering": { "rules": [{ "value": true }] } }"""
  val result1 = NamespaceSnapshotLoader(AppDomain.Recommendations).load(validJson)
  require(result1 is ParseResult.Success)

  // Load invalid config to Search
  val invalidJson = """{ "invalidFeature": { "rules": [{ "value": true }] } }"""
  val result2 = NamespaceSnapshotLoader(AppDomain.Search).load(invalidJson)
  require(result2 is ParseResult.Failure)

  // Verify Recommendations still works
  val ctx = Context(stableId = StableId("user"))
  assertTrue(AppDomain.Recommendations.collaborativeFiltering.evaluate(ctx))

  // Verify Search uses defaults (unaffected by failed load)
  assertTrue(AppDomain.Search.fuzzyMatching.evaluate(ctx))  // Default is true
}
```

### Test Independent Updates

```kotlin
@Test
fun `updating one namespace does not affect others`() {
  val ctx = Context(stableId = StableId("user"))

  // Initial state: both use defaults
  assertTrue(AppDomain.Recommendations.collaborativeFiltering.evaluate(ctx))  // true
  assertTrue(AppDomain.Search.fuzzyMatching.evaluate(ctx))  // true

  // Update only Recommendations
  val recsJson = """{ "collaborativeFiltering": { "rules": [{ "value": false }] } }"""
  NamespaceSnapshotLoader(AppDomain.Recommendations).load(recsJson)

  // Verify Recommendations changed
  assertFalse(AppDomain.Recommendations.collaborativeFiltering.evaluate(ctx))

  // Verify Search unchanged
  assertTrue(AppDomain.Search.fuzzyMatching.evaluate(ctx))
}
```

## Monitoring Multiple Namespaces

### Track Health Per Namespace

```kotlin
class NamespaceHealthMonitor {
  private val lastSuccessfulLoad = mutableMapOf<String, Instant>()

  fun recordLoad(
      namespace: Namespace,
      result: ParseResult
  ) {
    when (result) {
      is ParseResult.Success -> {
        lastSuccessfulLoad[namespace.id] = Instant.now()
        metrics.increment("namespace.load.success", tags = mapOf(
            "namespace" to namespace.id
        ))
      }
      is ParseResult.Failure -> {
        metrics.increment("namespace.load.failure", tags = mapOf(
            "namespace" to namespace.id,
            "error_type" to result.error::class.simpleName!!
        ))
      }
    }
  }

  fun checkHealth(
      namespace: Namespace,
      maxAge: Duration = 1.hours
  ): Boolean {
    val lastLoad = lastSuccessfulLoad[namespace.id]
    return lastLoad?.let {
      Duration.between(it, Instant.now()) < maxAge
    } ?: false
  }
}
```

### Alert on Namespace Failures

```kotlin
AppDomain.Recommendations.hooks.afterLoad.add { event ->
  when (event.result) {
    is ParseResult.Failure -> {
      alertOps(
          severity = Severity.HIGH,
          message = "Recommendations config failed",
          namespace = "recommendations",
          error = event.result.error
      )
    }
  }
}
```

## Governance Patterns

### Pattern: Namespace Ownership Registry

```kotlin
data class NamespaceOwnership(
    val namespace: Namespace,
    val teamSlackChannel: String,
    val approvers: List<String>,
    val slaMinutes: Int
)

val namespaceRegistry = mapOf(
    AppDomain.Recommendations.id to NamespaceOwnership(
        namespace = AppDomain.Recommendations,
        teamSlackChannel = "#recs-team",
        approvers = listOf("recs-lead@example.com"),
        slaMinutes = 15
    ),
    AppDomain.Search.id to NamespaceOwnership(
        namespace = AppDomain.Search,
        teamSlackChannel = "#search-team",
        approvers = listOf("search-lead@example.com"),
        slaMinutes = 30
    )
)

fun alertNamespaceOwners(
    namespaceId: String,
    message: String
) {
  val ownership = namespaceRegistry[namespaceId]
  ownership?.let {
    slackClient.postMessage(it.teamSlackChannel, message)
    emailService.send(it.approvers, "Namespace alert: $namespaceId", message)
  }
}
```

## Next Steps

- [Configuration Lifecycle](/fundamentals/configuration-lifecycle) — How config flows through namespaces
- [Safe Remote Configuration](/how-to-guides/safe-remote-config) — Loading patterns per namespace
- [Handling Failures](/how-to-guides/handling-failures) — Failure isolation strategies
- [Namespace Isolation (Theory)](/theory/namespace-isolation) — Formal isolation guarantees
