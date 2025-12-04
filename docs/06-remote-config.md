# Remote Configuration

Update flags without redeployment. Konditional provides UI-based management with RBAC or JSON-based configuration for dynamic rule updates.

---

## Why Remote Configuration?

Konditional flags are defined in code (type-safe), but **rules can be updated via UI or JSON** for dynamic updates:

```kotlin
// Flags defined in code (type-safe, version-controlled)
object Features : FeatureContainer<Namespace.Global>(Namespace.Global) {
    val DARK_MODE by boolean(default = false)
    val API_ENDPOINT by string(default = "https://api.example.com")
}

// Rules loaded from JSON (updatable without deployment)
val remoteJson = fetchFromServer("/flags.json")
when (val result = SnapshotSerializer.fromJson(remoteJson)) {
    is ParseResult.Success -> Namespace.Global.load(result.value)
    is ParseResult.Failure -> logger.error("Failed to load: ${result.error}")
}
```

**Key distinction:**

- Flag **definitions** live in code (compile-time safety, version-controlled)
- Flag **rules** can be updated via UI (RBAC-controlled) or JSON (runtime flexibility)

---

## JSON Serialization

### Export Configuration

```kotlin
// Serialize current configuration
val config = Namespace.Global.configuration()
val json = SnapshotSerializer.serialize(config)

// Save to file
File("flags.json").writeText(json)
```

**Example output:**

```json
{
  "flags": {
    "dark_mode": {
      "default": false,
      "active": true,
      "salt": "v1",
      "rules": [
        {
          "platforms": ["IOS"],
          "rollout": 50.0,
          "value": true
        }
      ]
    }
  }
}
```

### Import Configuration

```kotlin
// Load from file
val json = File("flags.json").readText()

when (val result = SnapshotSerializer.fromJson(json)) {
    is ParseResult.Success -> {
        Namespace.Global.load(result.value)
        logger.info("Configuration loaded successfully")
    }
    is ParseResult.Failure -> {
        logger.error("Parse failed: ${result.error}")
        // Keep existing configuration
    }
}
```

### Patch Updates

Apply incremental changes without full replacement:

```kotlin
val currentConfig = Namespace.Global.configuration()

// Patch JSON contains only changed flags
val patchJson = """
{
  "dark_mode": {
    "rules": [{
      "platforms": ["IOS", "ANDROID"],
      "rollout": 100.0,
      "value": true
    }]
  }
}
"""

when (val result = SnapshotSerializer.applyPatchJson(currentConfig, patchJson)) {
    is ParseResult.Success -> Namespace.Global.load(result.value)
    is ParseResult.Failure -> logger.error("Patch failed: ${result.error}")
}
```

**Benefits:**

- Smaller payloads
- Partial updates (other flags unchanged)
- Reduced risk (only changed flags affected)

---

## Storage Patterns

### Pattern 1: File-Based

Store configurations as local files:

```kotlin
class FileConfigLoader(private val filePath: String) {
    fun load(): Configuration? {
        val file = File(filePath)
        if (!file.exists()) return null

        val json = file.readText()
        return when (val result = SnapshotSerializer.fromJson(json)) {
            is ParseResult.Success -> result.value
            is ParseResult.Failure -> {
                logger.error("Failed to parse $filePath: ${result.error}")
                null
            }
        }
    }

    fun save(config: Configuration) {
        val json = SnapshotSerializer.serialize(config)
        File(filePath).writeText(json)
    }
}

// Usage
val loader = FileConfigLoader("/etc/app/flags.json")
loader.load()?.let { Namespace.Global.load(it) }
```

**Use cases:**

- Desktop applications
- Server-side with local config files
- Docker containers with mounted volumes

### Pattern 2: Database

Store in database for multi-instance consistency:

```kotlin
class DbConfigLoader(private val db: Database) {
    fun load(namespace: String): Configuration? {
        val json = db.query("SELECT config FROM flag_configs WHERE namespace = ?", namespace)
            .firstOrNull()
            ?: return null

        return when (val result = SnapshotSerializer.fromJson(json)) {
            is ParseResult.Success -> result.value
            is ParseResult.Failure -> null
        }
    }

    fun save(namespace: String, config: Configuration) {
        val json = SnapshotSerializer.serialize(config)
        db.execute(
            "INSERT INTO flag_configs (namespace, config) VALUES (?, ?) ON CONFLICT (namespace) DO UPDATE SET config = ?",
            namespace, json, json
        )
    }
}
```

**Use cases:**

- Multi-instance servers (shared config)
- Audit trail (store updates in DB)
- Versioning (store historical configs)

### Pattern 3: Cloud Storage

Fetch from S3, CloudFront CDN, etc.:

```kotlin
class CloudConfigLoader(private val httpClient: HttpClient) {
    suspend fun load(url: String): Configuration? {
        val json = try {
            httpClient.get(url).bodyAsText()
        } catch (e: Exception) {
            logger.error("Failed to fetch $url", e)
            return null
        }

        return when (val result = SnapshotSerializer.fromJson(json)) {
            is ParseResult.Success -> result.value
            is ParseResult.Failure -> null
        }
    }
}

// Usage
val loader = CloudConfigLoader(client)
launch {
    val config = loader.load("https://cdn.example.com/flags.json")
    config?.let { Namespace.Global.load(it) }
}
```

**Use cases:**

- Mobile apps (fetch on launch)
- Edge computing (CDN-backed configs)
- Global deployments (region-specific endpoints)

---

## Update Strategies

### Strategy 1: Polling

Periodically check for updates:

```kotlin
class PollingConfigUpdater(
    private val loader: CloudConfigLoader,
    private val url: String,
    private val intervalMs: Long = 60_000  // 1 minute
) {
    fun start() {
        timer(period = intervalMs) {
            runBlocking {
                val config = loader.load(url)
                config?.let {
                    Namespace.Global.load(it)
                    logger.info("Configuration updated")
                }
            }
        }
    }
}

// Usage
val updater = PollingConfigUpdater(loader, "https://api.example.com/flags.json")
updater.start()
```

**Pros:** Simple, reliable
**Cons:** Latency (up to poll interval), unnecessary requests

### Strategy 2: Streaming/Push

Server pushes updates to clients:

```kotlin
class StreamingConfigUpdater(private val wsClient: WebSocketClient) {
    fun connect(url: String) {
        wsClient.connect(url) { message ->
            when (val result = SnapshotSerializer.fromJson(message)) {
                is ParseResult.Success -> {
                    Namespace.Global.load(result.value)
                    logger.info("Configuration updated via WebSocket")
                }
                is ParseResult.Failure -> logger.error("Invalid config: ${result.error}")
            }
        }
    }
}
```

**Pros:** Instant updates, efficient
**Cons:** More complex, requires WebSocket infrastructure

### Strategy 3: On-Demand

Load on app launch or user action:

```kotlin
suspend fun updateConfiguration() {
    val json = httpClient.get("https://api.example.com/flags.json").bodyAsText()
    when (val result = SnapshotSerializer.fromJson(json)) {
        is ParseResult.Success -> {
            Namespace.Global.load(result.value)
            showToast("Configuration updated")
        }
        is ParseResult.Failure -> showError("Update failed")
    }
}

// Usage
// On app launch
lifecycleScope.launch {
    updateConfiguration()
}

// Or on button click
button.setOnClickListener {
    lifecycleScope.launch { updateConfiguration() }
}
```

**Pros:** User-controlled, simple
**Cons:** Requires user action

---

## DSL Quick Reference

### Boolean Flags

```kotlin
val FLAG by boolean(default = false) {
    salt("v1")
    active { true }
    rule {
        platforms(Platform.IOS)
        locales(AppLocale.UNITED_STATES)
        versions { min(2, 0, 0) }
        rollout { 50.0 }
        note("iOS US users, v2+, 50%")
    } returns true
}
```

### String Flags

```kotlin
val CONFIG by string(default = "default") {
    rule { platforms(Platform.IOS) } returns "ios-value"
    rule { platforms(Platform.ANDROID) } returns "android-value"
}
```

### Integer/Double Flags

```kotlin
val MAX_RETRIES by int(default = 3) {
    rule { platforms(Platform.WEB) } returns 5
}

val TIMEOUT by double(default = 30.0) {
    rule { platforms(Platform.MOBILE) } returns 60.0
}
```

### Custom Logic

```kotlin
val PREMIUM by boolean<EnterpriseContext>(default = false) {
    rule {
        extension {
            Evaluable.factory { ctx ->
                ctx.subscriptionTier == SubscriptionTier.ENTERPRISE
            }
        }
    } returns true
}
```

---

## Best Practices

### 1. Validate After Loading

Always check parse results:

```kotlin
when (val result = SnapshotSerializer.fromJson(json)) {
    is ParseResult.Success -> {
        // Optionally validate business rules
        if (validateConfig(result.value)) {
            Namespace.Global.load(result.value)
        } else {
            logger.error("Configuration failed validation")
        }
    }
    is ParseResult.Failure -> logger.error("Parse failed: ${result.error}")
}
```

### 2. Cache Configurations

Avoid repeated network calls:

```kotlin
class CachedConfigLoader(
    private val remoteLoader: CloudConfigLoader,
    private val cacheFile: File
) {
    suspend fun load(url: String): Configuration? {
        // Try cache first
        if (cacheFile.exists()) {
            val cached = loadFromFile(cacheFile)
            if (cached != null) return cached
        }

        // Fetch from remote
        val config = remoteLoader.load(url)
        config?.let { saveToFile(it, cacheFile) }
        return config
    }
}
```

### 3. Handle Load Failures Gracefully

Keep existing configuration on failure:

```kotlin
val currentConfig = Namespace.Global.configuration()  // Backup

when (val result = loadRemoteConfig()) {
    is ParseResult.Success -> Namespace.Global.load(result.value)
    is ParseResult.Failure -> {
        logger.error("Failed to load remote config, keeping current")
        // currentConfig still in effect
    }
}
```

### 4. Version Your Configurations

Track changes over time:

```json
{
  "version": "2024-01-15T10:30:00Z",
  "flags": { ... }
}
```

### 5. Use Patches for Incremental Updates

Send only changes, not full configs:

```kotlin
// Small patch JSON
val patch = """{"dark_mode": {"rules": [...]}}"""

// Apply to current config
SnapshotSerializer.applyPatchJson(currentConfig, patch)
```

---

## Common Patterns

### Pattern: Kill Switch

Remotely disable feature without deployment:

```json
{
  "payment_processing": {
    "active": false,
    "default": false
  }
}
```

### Pattern: Gradual Rollout

Increase rollout percentage over time:

```json
// Day 1
{"new_feature": {"rules": [{"rollout": 10.0, "value": true}]}}

// Day 3
{"new_feature": {"rules": [{"rollout": 50.0, "value": true}]}}

// Day 7
{"new_feature": {"rules": [{"rollout": 100.0, "value": true}]}}
```

### Pattern: Platform-Specific Config

Different settings per platform:

```json
{
  "api_endpoint": {
    "default": "https://api.example.com",
    "rules": [
      {
        "platforms": ["IOS"],
        "value": "https://api-ios.example.com"
      },
      {
        "platforms": ["ANDROID"],
        "value": "https://api-android.example.com"
      }
    ]
  }
}
```

---

## Next Steps

**Just getting started?** See [Getting Started](01-getting-started.md) for your first flag.

**Need advanced targeting?** See [Targeting & Rollouts](04-targeting-rollouts.md) for rules and specificity.

**Understanding evaluation?** See [Evaluation](05-evaluation.md) for evaluation methods and flow.
