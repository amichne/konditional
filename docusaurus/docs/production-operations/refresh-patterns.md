# Configuration Refresh Patterns

How to safely update feature flag configuration in production systems.

---

## Overview

Configuration refresh is the act of replacing a namespace's current configuration with new configuration, typically
loaded from a remote source. Konditional
provides several patterns for safe configuration updates.

---

## Refresh Strategies

### 1. Manual Refresh (Explicit Control)

Load configuration explicitly when you want to update:

```kotlin
object AppFeatures : Namespace("app")

// Initial load
val result = NamespaceSnapshotLoader(AppFeatures).load(initialConfig)
when {
  result.isSuccess -> logger.info("Initial config loaded")
  result.isFailure -> logger.error("Initial load failed: ${result.parseErrorOrNull()}")
}

// Later: manual refresh
fun refreshConfiguration() {
  val newConfig = fetchFromRemote()
  val result = NamespaceSnapshotLoader(AppFeatures).load(newConfig)
when {
    result.isSuccess -> logger.info("Config refreshed")
    result.isFailure -> {
      logger.error("Refresh failed: ${result.parseErrorOrNull()}")
      // Last-known-good remains active
    }
  }
}
```

**Use when:**

- You want explicit control over refresh timing
- Configuration changes are triggered by specific events (deployments, admin actions)
- You need to coordinate refresh with other operations

### 2. Polling Pattern (Periodic Refresh)

Check for configuration updates on a schedule:

```kotlin
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.minutes

class ConfigurationPoller(
    private val namespace: Namespace,
    private val fetchConfig: suspend () -> String,
    private val pollInterval: Duration = 5.minutes
) {
  private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

  fun start() {
    scope.launch {
      while (isActive) {
        try {
          val config = fetchConfig()
          val result = NamespaceSnapshotLoader(namespace).load(config)
when {
            result.isSuccess -> logger.info("Config updated via poll")
            result.isFailure -> logger.warn("Poll config invalid: ${result.parseErrorOrNull()}")
          }
        } catch (e: Exception) {
          logger.error("Poll fetch failed", e)
        }
        delay(pollInterval)
      }
    }
  }

  fun stop() {
    scope.cancel()
  }
}

// Usage
val poller = ConfigurationPoller(
    namespace = AppFeatures,
    fetchConfig = { httpClient.get("https://config.example.com/app-features.json").body() }
)
poller.start()
```

**Use when:**

- Configuration changes are infrequent but need to be picked up eventually
- You want to avoid maintaining persistent connections
- Latency of several minutes is acceptable

**Considerations:**

- Balance poll frequency with load on config server
- Add jitter to avoid thundering herd
- Consider exponential backoff on failure

### 3. Webhook / Push Pattern (Event-Driven)

Receive notifications when configuration changes:

```kotlin
@POST("/config/webhook")
fun handleConfigUpdate(request: ConfigWebhookRequest): Response {
  return when {
    !request.isValidSignature() -> Response.status(401).build()
    else -> {
      val newConfig = fetchConfigFromCDN(request.configVersion)
      val result = NamespaceSnapshotLoader(AppFeatures).load(newConfig)
when {
        result.isSuccess -> {
          logger.info("Config updated via webhook")
          Response.ok().build()
        }
        result.isFailure -> {
          logger.error("Webhook config invalid: ${result.parseErrorOrNull()}")
          Response.status(400).build()
        }
      }
    }
  }
}
```

**Use when:**

- Configuration changes need to propagate quickly (seconds, not minutes)
- Your config service supports push notifications
- You can validate webhook authenticity (HMAC signatures, etc.)

**Considerations:**

- Implement webhook signature validation
- Handle webhook retry/failure scenarios
- Consider rate limiting to prevent abuse

### 4. File Watch Pattern (Local Development)

Watch a local file for changes:

```kotlin
import java.nio.file.*
import kotlin.io.path.readText

class ConfigFileWatcher(
    private val namespace: Namespace,
    private val configPath: Path
) {
  private val watchService = FileSystems.getDefault().newWatchService()

  fun start() {
    configPath.parent.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY)

    thread(start = true, name = "config-watcher") {
      while (true) {
        val key = watchService.take()
        key.pollEvents().forEach { event ->
          if ((event.context() as Path).fileName == configPath.fileName) {
            reloadConfig()
          }
        }
        key.reset()
      }
    }
  }

  private fun reloadConfig() {
    try {
      val config = configPath.readText()
      val result = NamespaceSnapshotLoader(namespace).load(config)
when {
        result.isSuccess -> logger.info("Config reloaded from file")
        result.isFailure -> logger.error("File config invalid: ${result.parseErrorOrNull()}")
      }
    } catch (e: Exception) {
      logger.error("Failed to read config file", e)
    }
  }
}

// Usage (development only)
if (environment == "development") {
  ConfigFileWatcher(AppFeatures, Paths.get("config/features.json")).start()
}
```

**Use when:**

- Local development or testing
- Configuration is managed via local files
- You want instant feedback on configuration changes

---

## Safety Guarantees

### Atomic Replacement

Configuration updates are atomic—evaluation never sees partial state:

```kotlin
// Thread 1: Loading new config
NamespaceSnapshotLoader(AppFeatures).load(newConfig)

// Thread 2: Evaluating (concurrent)
val result = AppFeatures.someFeature.evaluate(ctx)
// Always sees either old config OR new config, never partial
```

**Mechanism:** `load()` atomically swaps the internal `Configuration` reference.
See [Thread Safety](/production-operations/thread-safety) for details.

### Last-Known-Good on Failure

Invalid configuration is rejected; previous configuration remains active:

```kotlin
// Current config: { "maxRetries": 3 }
val badConfig = """{ "maxRetries": "invalid" }"""

val result = NamespaceSnapshotLoader(AppFeatures).load(badConfig)
when {
  result.isFailure -> {
    // Load rejected, maxRetries still returns 3
    logger.error("Bad config rejected: ${result.parseErrorOrNull()}")
  }
}

// Evaluation still works with last-known-good
val retries: Int = AppFeatures.maxRetries.evaluate(ctx)  // Returns 3
```

**Guarantee:** Failed loads never affect evaluation.

---

## Operational Patterns

### Pattern: Initial Load + Polling Fallback

```kotlin
// 1. Load initial config synchronously at startup
val initialConfig = fetchConfigOrDefault()
val result = NamespaceSnapshotLoader(AppFeatures).load(initialConfig)
when {
  result.isFailure -> {
    logger.error("Initial load failed, using defaults: ${result.parseErrorOrNull()}")
    // Defaults remain active
  }
  result.isSuccess -> logger.info("Initial config loaded")
}

// 2. Start polling for updates
ConfigurationPoller(AppFeatures, fetchConfig = ::fetchFromRemote).start()
```

**Benefits:**

- Service starts even if remote config unavailable
- Updates picked up automatically via polling

### Pattern: Webhook + Polling Backup

```kotlin
// Primary: webhooks for fast updates
app.post("/config/webhook", ::handleConfigUpdate)

// Backup: polling in case webhooks fail
ConfigurationPoller(
    namespace = AppFeatures,
    fetchConfig = ::fetchFromRemote,
    pollInterval = 15.minutes  // Longer interval since webhooks handle most updates
).start()
```

**Benefits:**

- Fast updates via webhook (seconds)
- Resilient to webhook delivery failures

### Pattern: Versioned Configuration with Rollback

```kotlin
data class VersionedConfig(
    val version: String,
    val config: String
)

class ConfigHistory(private val namespace: Namespace) {
  private val history = mutableListOf<VersionedConfig>()
  private val maxHistory = 10

  fun loadAndTrack(
      version: String,
      config: String
  ): Result<MaterializedConfiguration> {
    val result = NamespaceSnapshotLoader(namespace).load(config)
    return when {
      result.isSuccess -> {
        history.add(0, VersionedConfig(version, config))
        if (history.size > maxHistory) history.removeLast()
        result
      }
      result.isFailure -> result
    }
  }

  fun rollback(toVersion: String): Result? {
    val target = history.find { it.version == toVersion }
    return target?.let { loadAndTrack(it.version, it.config) }
  }
}
```

**Use when:**

- You need to quickly revert bad configuration
- Auditing configuration changes is required

---

## Common Pitfalls

### Pitfall: Ignoring Result Failures

```kotlin
// DON'T
NamespaceSnapshotLoader(AppFeatures).load(config)  // Ignored result

// DO
val result = NamespaceSnapshotLoader(AppFeatures).load(config)
when {
  result.isSuccess -> logger.info("Config loaded")
  result.isFailure -> {
    logger.error("Load failed: ${result.parseErrorOrNull()}")
    alertOps("Configuration load failure")
  }
}
```

### Pitfall: Polling Too Frequently

```kotlin
// DON'T
pollInterval = 10.seconds  // High load on config server

// DO
pollInterval = 5.minutes  // Reasonable for most use cases
// Use webhooks if you need faster propagation
```

### Pitfall: No Webhook Authentication

```kotlin
// DON'T
@POST("/config/webhook")
fun update(config: String) {
  NamespaceSnapshotLoader(AppFeatures).load(config)  // Anyone can POST
}

// DO
@POST("/config/webhook")
fun update(
    signature: String,
    config: String
) {
  if (!validateHMAC(signature, config, webhookSecret)) {
    throw UnauthorizedException()
  }
  NamespaceSnapshotLoader(AppFeatures).load(config)
}
```

---

## Monitoring Refresh Operations

### Metrics to Track

1. **Refresh frequency:** How often configuration is updated
2. **Parse failure rate:** Percentage of loads that fail validation
3. **Refresh latency:** Time from config change to application update
4. **Configuration version lag:** Difference between expected and actual version

### Example with Observability Hooks

```kotlin
AppFeatures.hooks.afterLoad.add { event ->
  when {
    event.result.isSuccess -> {
      metrics.increment("config.refresh.success")
      metrics.gauge("config.version", event.version)
    }
    event.result.isFailure -> {
      metrics.increment("config.refresh.failure")
      logger.error("Config load failed", event.result.parseErrorOrNull())
    }
  }
  metrics.recordLatency("config.refresh.duration", event.durationMs)
}
```

---

## Summary

Konditional supports multiple refresh patterns:

- **Manual:** Explicit control, event-driven
- **Polling:** Periodic checks, simple implementation
- **Webhook:** Fast propagation, event-driven
- **File watch:** Development/testing

All patterns benefit from:

- **Atomic updates:** No partial state
- **Last-known-good on failure:** Invalid config rejected
- **Thread-safe:** Concurrent evaluation always safe

Choose the pattern that fits your:

- **Latency requirements:** Webhook (seconds) vs polling (minutes)
- **Infrastructure:** Push vs pull
- **Complexity tolerance:** Manual vs automated

---

## Next Steps

- [Thread Safety](/production-operations/thread-safety) — How atomic updates work
- [Failure Modes](/production-operations/failure-modes) — Handling invalid configuration
- [How-To: Safe Remote Configuration](/how-to-guides/safe-remote-config) — Step-by-step pattern
