# Serialization

Export and import feature flag configurations as JSON for remote configuration, database persistence, and version control.

## Overview

Konditional's serialization system provides type-safe JSON conversion for feature flag configurations. The serialization API follows **parse-don't-validate** principles, returning `ParseResult` instead of throwing exceptions, enabling compile-time-checked error handling.

**Core Principles:**

- **Storage-agnostic**: Serialization handles only JSON conversion - you choose the storage (files, databases, cloud storage)
- **Type-safe parsing**: All deserialization returns `ParseResult<T>` for structured error handling
- **Namespace isolation**: Serialize and load configurations per namespace for independent deployment
- **Patch support**: Apply incremental updates without replacing entire configurations

**Use Cases:**

- Remote configuration delivery from servers or CDNs
- Database persistence for dynamic flag management
- Configuration versioning and rollback
- Backup and restore operations
- A/B test configuration management

---

## SnapshotSerializer

`SnapshotSerializer` is the primary serialization API for converting entire `Konfig` snapshots to and from JSON.

### Serializing Configurations

Convert a `Konfig` to JSON format:

```kotlin
import io.amichne.konditional.serialization.SnapshotSerializer
import io.amichne.konditional.core.Namespace

// Get current configuration
val configuration = Namespace.Global.configuration()

// Serialize to JSON string
val json = SnapshotSerializer.serialize(configuration)

// Store however you like
File("config/flags.json").writeText(json)
```

**Output format**: 2-space indented JSON for readability.

### Deserializing Configurations

Parse JSON back into a `Konfig`:

```kotlin
// Load JSON from your storage
val json = File("config/flags.json").readText()

// Deserialize with type-safe error handling
when (val result = SnapshotSerializer.fromJson(json)) {
    is ParseResult.Success -> {
        val configuration = result.value
        println("Loaded ${configuration.flags.size} flags")

        // Load into namespace
        Namespace.Global.load(configuration)
    }
    is ParseResult.Failure -> {
        when (val error = result.error) {
            is ParseError.InvalidJson ->
                println("JSON parsing failed: ${error.message}")
            is ParseError.FeatureNotFound ->
                println("Unknown feature: ${error.key}")
            is ParseError.InvalidSnapshot ->
                println("Invalid configuration: ${error.message}")
        }
    }
}
```

**Important**: `fromJson()` does NOT automatically load the configuration into any namespace. You must explicitly call `Namespace.load()` if you want to update the runtime configuration.

### Applying Patch Updates

Apply incremental changes to an existing configuration without replacing it entirely:

```kotlin
// Current configuration
val currentConfiguration = Namespace.Global.configuration()

// Patch JSON (adds, updates, or removes specific flags)
val patchJson = """
{
  "flags": [
    {
      "key": "new_feature",
      "defaultValue": {
        "type": "BOOLEAN",
        "value": true
      },
      "salt": "v1",
      "isActive": true,
      "rules": []
    }
  ],
  "removeKeys": ["deprecated_flag"]
}
""".trimIndent()

// Apply patch
when (val result = SnapshotSerializer.applyPatchJson(currentConfiguration, patchJson)) {
    is ParseResult.Success -> {
        val patchedKonfig = result.value
        Namespace.Global.load(patchedKonfig)
        println("Patch applied successfully")
    }
    is ParseResult.Failure -> {
        println("Patch failed: ${result.error}")
    }
}
```

**Patch operations**:
- Flags in `flags` array are added or updated (by key)
- Keys in `removeKeys` array are removed from configuration
- Existing flags not mentioned are preserved unchanged

### Custom Moshi Configuration

Access the default Moshi instance for advanced use cases:

```kotlin
import com.squareup.moshi.Moshi

val moshi: Moshi = SnapshotSerializer.defaultMoshi()

// Use for custom adapters or inspection
```

**Note**: The default Moshi includes all necessary adapters for Konditional types (version ranges, flag values, etc.). Most users never need to access this directly.

---

## NamespaceSnapshotSerializer

`NamespaceSnapshotSerializer` provides namespace-scoped serialization, enabling independent configuration management per domain.

### Creating a Namespace Serializer

Create a serializer for a specific namespace:

```kotlin
import io.amichne.konditional.serialization.NamespaceSnapshotSerializer

// Constructor approach
val serializer = NamespaceSnapshotSerializer(Namespace.Payments)

// Or factory method
val serializer = NamespaceSnapshotSerializer.forModule(Namespace.Payments)
```

### Serializing a Namespace

Export only the flags from a specific namespace:

```kotlin
val serializer = NamespaceSnapshotSerializer(Namespace.Payments)

// Serialize current state
val json = serializer.toJson()

// Save to namespace-specific storage
File("configs/payments.json").writeText(json)
```

**Isolation**: Only flags from the specified namespace are included in the output.

### Deserializing into a Namespace

Load JSON and automatically update the namespace:

```kotlin
val serializer = NamespaceSnapshotSerializer(Namespace.Payments)

// Load from storage
val json = File("configs/payments.json").readText()

// Deserialize and load (automatic side effect)
when (val result = serializer.fromJson(json)) {
    is ParseResult.Success -> {
        // Configuration already loaded into Namespace.Payments
        println("Loaded ${result.value.flags.size} payment flags")
    }
    is ParseResult.Failure -> {
        println("Failed to load: ${result.error}")
    }
}
```

**Important side effect**: Unlike `SnapshotSerializer.fromJson()`, this method **automatically loads** the deserialized configuration into the namespace upon success. The namespace's runtime state is immediately updated.

### Namespace Isolation Example

Manage multiple domains independently:

```kotlin
// Serialize each namespace separately
val authSerializer = NamespaceSnapshotSerializer(Namespace.Authentication)
val paymentSerializer = NamespaceSnapshotSerializer(Namespace.Payments)

val authJson = authSerializer.toJson()
val paymentJson = paymentSerializer.toJson()

// Store in separate locations
s3.put("configs/auth.json", authJson)
s3.put("configs/payments.json", paymentJson)

// Load independently (e.g., different deployment schedules)
authSerializer.fromJson(s3.get("configs/auth.json"))
paymentSerializer.fromJson(s3.get("configs/payments.json"))
```

**Benefits**:
- Independent deployment schedules per domain
- Reduced blast radius (errors in one namespace don't affect others)
- Team ownership boundaries (each team manages their own config)

---

## Serializer Interface

The `Serializer<T>` interface provides a common contract for custom serialization implementations.

```kotlin
interface Serializer<T> {
    fun toJson(): String
    fun fromJson(json: String): ParseResult<T>
}
```

### When to Use

- `NamespaceSnapshotSerializer` implements this interface for namespace-scoped serialization
- `SnapshotSerializer` is an object with static methods and does NOT implement this interface
- Custom implementations for specialized serialization needs

### Example Usage

```kotlin
val serializer: Serializer<Konfig> = NamespaceSnapshotSerializer(Namespace.Global)

// Polymorphic usage
fun saveConfig(serializer: Serializer<Konfig>, path: String) {
    val json = serializer.toJson()
    File(path).writeText(json)
}

fun loadConfig(serializer: Serializer<Konfig>, path: String): ParseResult<Konfig> {
    val json = File(path).readText()
    return serializer.fromJson(json)
}
```

---

## JSON Format Reference

Konditional uses a structured JSON format for flag configurations.

### Top-Level Structure

```json
{
  "flags": [
    // Array of flag definitions
  ]
}
```

### Flag Definition Format

Each flag has this structure:

```json
{
  "key": "feature_key",
  "defaultValue": {
    "type": "BOOLEAN",
    "value": true
  },
  "salt": "v1",
  "isActive": true,
  "rules": [
    // Array of rule definitions
  ]
}
```

**Fields**:

- `key` (string, required): Unique flag identifier
- `defaultValue` (object, required): Default value with type
- `salt` (string, optional): Salt for rollout bucketing (default: "v1")
- `isActive` (boolean, optional): Whether flag is active (default: true)
- `rules` (array, optional): Conditional value rules (default: empty)

### Rule Format

Rules define targeting criteria and conditional values:

```json
{
  "value": {
    "type": "BOOLEAN",
    "value": true
  },
  "rampUp": 50.0,
  "note": "iOS users, 50% rollout",
  "platforms": ["IOS", "ANDROID"],
  "locales": ["UNITED_STATES", "CANADA"],
  "versionRange": {
    "type": "MIN_AND_MAX_BOUND",
    "min": {
      "major": 2,
      "minor": 0,
      "patch": 0
    },
    "max": {
      "major": 3,
      "minor": 0,
      "patch": 0
    }
  }
}
```

**Fields**:

- `value` (object, required): Value to return when rule matches
- `rampUp` (number, optional): Rollout percentage 0.0-100.0 (default: 100.0)
- `note` (string, optional): Documentation note
- `platforms` (array, optional): Allowed platforms (default: all)
- `locales` (array, optional): Allowed locales (default: all)
- `versionRange` (object, optional): Version constraints (default: all versions)

---

## Supported Value Types

Konditional supports five value types in JSON.

### BOOLEAN

Boolean true/false values:

```json
{
  "type": "BOOLEAN",
  "value": true
}
```

### STRING

Text values:

```json
{
  "type": "STRING",
  "value": "production-api-endpoint"
}
```

### INT

Integer values:

```json
{
  "type": "INT",
  "value": 42
}
```

### DOUBLE

Decimal values:

```json
{
  "type": "DOUBLE",
  "value": 3.14159
}
```

### JSON (Data Classes)

#### Temporarily disabled
~~Complex structured data:~~

```json
{
  "type": "JSON",
  "value": {
    "primaryColor": "#FFFFFF",
    "fontSize": 14,
    "darkMode": false
  }
}
```

~~JSON values must match the data class structure expected by the feature definition.~~
**Note**: There are plans to enable in the immedaite future, but for the time being these are not included

---

## Version Range Types

Version ranges use different JSON structures based on bound type.

### Unbounded (All Versions)

```json
{
  "type": "UNBOUNDED"
}
```

Matches all versions.

### Minimum Bound Only

```json
{
  "type": "MIN_BOUND",
  "min": {
    "major": 2,
    "minor": 0,
    "patch": 0
  }
}
```

Matches version >= 2.0.0.

### Maximum Bound Only

```json
{
  "type": "MAX_BOUND",
  "max": {
    "major": 2,
    "minor": 0,
    "patch": 0
  }
}
```

Matches version <= 2.0.0.

### Both Bounds

```json
{
  "type": "MIN_AND_MAX_BOUND",
  "min": {
    "major": 1,
    "minor": 5,
    "patch": 0
  },
  "max": {
    "major": 2,
    "minor": 0,
    "patch": 0
  }
}
```

Matches version >= 1.5.0 AND <= 2.0.0.

---

## Patch Format

Patches enable incremental configuration updates.

### Patch Structure

```json
{
  "flags": [
    // Flags to add or update
  ],
  "removeKeys": [
    // Flag keys to remove
  ]
}
```

### Adding a Flag

```json
{
  "flags": [
    {
      "key": "new_feature",
      "defaultValue": {
        "type": "BOOLEAN",
        "value": false
      },
      "salt": "v1",
      "isActive": true,
      "rules": []
    }
  ],
  "removeKeys": []
}
```

### Updating a Flag

Same structure - flags are matched by key:

```json
{
  "flags": [
    {
      "key": "existing_feature",
      "defaultValue": {
        "type": "BOOLEAN",
        "value": true
      },
      "salt": "v2",
      "isActive": true,
      "rules": [
        {
          "value": {
            "type": "BOOLEAN",
            "value": false
          },
          "rampUp": 50.0,
          "platforms": ["IOS"]
        }
      ]
    }
  ],
  "removeKeys": []
}
```

### Removing a Flag

```json
{
  "flags": [],
  "removeKeys": ["deprecated_feature", "old_experiment"]
}
```

### Combined Operations

Add, update, and remove in a single patch:

```json
{
  "flags": [
    {
      "key": "updated_feature",
      "defaultValue": {
        "type": "STRING",
        "value": "new-default"
      }
    },
    {
      "key": "new_feature",
      "defaultValue": {
        "type": "INT",
        "value": 100
      }
    }
  ],
  "removeKeys": ["old_feature"]
}
```

---

## Common Patterns

### Remote Configuration

Fetch configuration from a remote server:

```kotlin
import io.amichne.konditional.serialization.SnapshotSerializer
import java.net.URL

suspend fun fetchRemoteConfig(url: String) {
    try {
        // Fetch JSON from server
        val json = URL(url).readText()

        // Parse and validate
        when (val result = SnapshotSerializer.fromJson(json)) {
            is ParseResult.Success -> {
                // Load into namespace
                Namespace.Global.load(result.value)
                println("Remote config loaded successfully")
            }
            is ParseResult.Failure -> {
                println("Failed to parse remote config: ${result.error}")
                // Fall back to local cached config
                loadCachedConfig()
            }
        }
    } catch (e: Exception) {
        println("Network error: ${e.message}")
        loadCachedConfig()
    }
}

// Periodic polling
suspend fun pollRemoteConfig(url: String, intervalMs: Long) {
    while (true) {
        fetchRemoteConfig(url)
        delay(intervalMs)
    }
}
```

**Best practices**:
- Cache configurations locally for offline support
- Validate JSON before loading to avoid partial updates
- Use versioning to prevent applying stale configurations

### Database Storage

Store configurations in a database:

```kotlin
import io.amichne.konditional.serialization.NamespaceSnapshotSerializer

// Save to database
fun saveToDatabase(namespace: Namespace, db: Database) {
    val serializer = NamespaceSnapshotSerializer(namespace)
    val json = serializer.toJson()

    db.execute(
        "INSERT OR REPLACE INTO configurations (namespace_id, json, updated_at) VALUES (?, ?, ?)",
        namespace.id,
        json,
        System.currentTimeMillis()
    )
}

// Load from database
fun loadFromDatabase(namespace: Namespace, db: Database) {
    val row = db.query(
        "SELECT json FROM configurations WHERE namespace_id = ?",
        namespace.id
    ).firstOrNull()

    if (row != null) {
        val serializer = NamespaceSnapshotSerializer(namespace)
        when (val result = serializer.fromJson(row["json"] as String)) {
            is ParseResult.Success ->
                println("Loaded from database: ${result.value.flags.size} flags")
            is ParseResult.Failure ->
                println("Database config invalid: ${result.error}")
        }
    }
}

// Audit trail
fun saveWithHistory(namespace: Namespace, db: Database, userId: String) {
    val serializer = NamespaceSnapshotSerializer(namespace)
    val json = serializer.toJson()

    db.transaction {
        // Update current config
        execute(
            "INSERT OR REPLACE INTO configurations (namespace_id, json) VALUES (?, ?)",
            namespace.id, json
        )

        // Save to history
        execute(
            "INSERT INTO config_history (namespace_id, json, user_id, created_at) VALUES (?, ?, ?, ?)",
            namespace.id, json, userId, System.currentTimeMillis()
        )
    }
}
```

### Backup and Restore

Version control and rollback:

```kotlin
// Backup current configuration
fun backupConfiguration(namespace: Namespace, backupDir: File) {
    val serializer = NamespaceSnapshotSerializer(namespace)
    val json = serializer.toJson()

    val timestamp = System.currentTimeMillis()
    val filename = "${namespace.id}-$timestamp.json"

    backupDir.resolve(filename).writeText(json)
    println("Backed up to $filename")
}

// List available backups
fun listBackups(namespace: Namespace, backupDir: File): List<File> {
    return backupDir.listFiles { file ->
        file.name.startsWith("${namespace.id}-") && file.extension == "json"
    }?.sortedByDescending { it.name } ?: emptyList()
}

// Restore from backup
fun restoreConfiguration(namespace: Namespace, backupFile: File) {
    val json = backupFile.readText()
    val serializer = NamespaceSnapshotSerializer(namespace)

    when (val result = serializer.fromJson(json)) {
        is ParseResult.Success ->
            println("Restored from ${backupFile.name}")
        is ParseResult.Failure ->
            println("Failed to restore: ${result.error}")
    }
}

// Automated backup on change
fun configureAutoBackup(namespace: Namespace, backupDir: File) {
    var lastJson = ""

    // Poll for changes (or use observer pattern)
    Timer().scheduleAtFixedRate(0, 60_000) { // Every minute
        val currentJson = NamespaceSnapshotSerializer(namespace).toJson()

        if (currentJson != lastJson) {
            backupConfiguration(namespace, backupDir)
            lastJson = currentJson
        }
    }
}
```

### Configuration Versioning

Track configuration versions:

```kotlin
data class VersionedConfig(
    val version: Int,
    val json: String,
    val timestamp: Long,
    val author: String
)

class ConfigVersionManager(private val namespace: Namespace) {
    private val versions = mutableListOf<VersionedConfig>()
    private var currentVersion = 0

    fun saveVersion(author: String) {
        val serializer = NamespaceSnapshotSerializer(namespace)
        val json = serializer.toJson()

        val version = VersionedConfig(
            version = ++currentVersion,
            json = json,
            timestamp = System.currentTimeMillis(),
            author = author
        )

        versions.add(version)
        println("Saved version $currentVersion by $author")
    }

    fun rollbackToVersion(version: Int) {
        val config = versions.find { it.version == version }
            ?: error("Version $version not found")

        val serializer = NamespaceSnapshotSerializer(namespace)
        when (val result = serializer.fromJson(config.json)) {
            is ParseResult.Success ->
                println("Rolled back to version $version")
            is ParseResult.Failure ->
                println("Rollback failed: ${result.error}")
        }
    }

    fun listVersions(): List<VersionedConfig> = versions.toList()
}
```

---

## Best Practices

### Validate Before Loading

Always validate configurations before applying:

```kotlin
// Good: Validate first
when (val result = SnapshotSerializer.fromJson(json)) {
    is ParseResult.Success -> {
        // Validation passed, safe to load
        Namespace.Global.load(result.value)
    }
    is ParseResult.Failure -> {
        // Handle error, don't load
        logError("Invalid config: ${result.error}")
    }
}

// Bad: Blindly applying without validation
val configuration = SnapshotSerializer.fromJson(json) as ParseResult.Success
Namespace.Global.load(configuration.value)  // Crashes if parsing failed
```

### Cache Configurations Locally

Always maintain a local cache:

```kotlin
suspend fun syncConfiguration(remoteUrl: String, cacheFile: File) {
    try {
        // Try to fetch remote
        val remoteJson = fetchFromServer(remoteUrl)

        when (val result = SnapshotSerializer.fromJson(remoteJson)) {
            is ParseResult.Success -> {
                // Valid remote config - cache it
                cacheFile.writeText(remoteJson)
                Namespace.Global.load(result.value)
            }
            is ParseResult.Failure -> {
                // Invalid remote - use cache
                loadFromCache(cacheFile)
            }
        }
    } catch (e: Exception) {
        // Network error - use cache
        loadFromCache(cacheFile)
    }
}
```

### Use Namespace Isolation

Organize flags by domain for independent deployment:

```kotlin
// Good: Independent taxonomies
NamespaceSnapshotSerializer(Namespace.Payments).fromJson(paymentsJson)
NamespaceSnapshotSerializer(Namespace.Search).fromJson(searchJson)

// Better: Each team manages their own
object PaymentsTeam {
    fun deployConfig(json: String) {
        NamespaceSnapshotSerializer(Namespace.Payments).fromJson(json)
    }
}
```

### Version Your Configurations

Track changes for debugging and rollback:

```kotlin
// Include version metadata in storage
data class StoredConfig(
    val version: String,
    val timestamp: Long,
    val author: String,
    val json: String
)

fun saveWithMetadata(configuration: Konfig, author: String) {
    val json = SnapshotSerializer.serialize(configuration)

    val stored = StoredConfig(
        version = "1.2.3",
        timestamp = System.currentTimeMillis(),
        author = author,
        json = json
    )

    database.save(stored)
}
```

### Handle Errors Gracefully

Provide fallbacks for all error cases:

```kotlin
fun loadConfigWithFallback(
    primarySource: () -> String,
    fallbackSource: () -> String
): Konfig {
    // Try primary source
    val primaryResult = try {
        SnapshotSerializer.fromJson(primarySource())
    } catch (e: Exception) {
        ParseResult.Failure(ParseError.InvalidJson("Primary source error: ${e.message}"))
    }

    when (primaryResult) {
        is ParseResult.Success -> return primaryResult.value
        is ParseResult.Failure -> {
            // Log primary failure
            logWarning("Primary source failed: ${primaryResult.error}")

            // Try fallback
            when (val fallbackResult = SnapshotSerializer.fromJson(fallbackSource())) {
                is ParseResult.Success -> return fallbackResult.value
                is ParseResult.Failure -> {
                    // Both failed - use empty configuration
                    logError("All sources failed: ${fallbackResult.error}")
                    return Konfig(emptyMap())
                }
            }
        }
    }
}
```

### Use Patches for Incremental Updates

Prefer patches over full replacements when possible:

```kotlin
// Good: Incremental patch
val patch = """
{
  "flags": [
    {"key": "new_feature", "defaultValue": {"type": "BOOLEAN", "value": true}}
  ],
  "removeKeys": []
}
"""
SnapshotSerializer.applyPatchJson(currentConfiguration, patch)

// Less efficient: Full replacement
val fullConfig = """
{
  "flags": [
    // Entire configuration repeated
  ]
}
"""
```

---

## Complete Example

End-to-end remote configuration with caching:

```kotlin
import io.amichne.konditional.serialization.NamespaceSnapshotSerializer
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.result.ParseResult
import java.io.File

class RemoteConfigManager(
    private val namespace: Namespace,
    private val remoteUrl: String,
    private val cacheFile: File
) {
    private val serializer = NamespaceSnapshotSerializer(namespace)

    suspend fun initialize() {
        // Load from cache immediately
        loadFromCache()

        // Then fetch remote in background
        fetchRemote()
    }

    suspend fun fetchRemote() {
        try {
            val json = httpClient.get(remoteUrl).bodyAsText()

            when (val result = serializer.fromJson(json)) {
                is ParseResult.Success -> {
                    // Cache successful remote config
                    cacheFile.writeText(json)
                    println("Remote config loaded: ${result.value.flags.size} flags")
                }
                is ParseResult.Failure -> {
                    println("Remote config invalid: ${result.error}")
                }
            }
        } catch (e: Exception) {
            println("Failed to fetch remote: ${e.message}")
        }
    }

    private fun loadFromCache() {
        if (!cacheFile.exists()) {
            println("No cache found, using defaults")
            return
        }

        val json = cacheFile.readText()
        when (val result = serializer.fromJson(json)) {
            is ParseResult.Success ->
                println("Loaded from cache: ${result.value.flags.size} flags")
            is ParseResult.Failure ->
                println("Cache invalid: ${result.error}")
        }
    }

    fun backup(backupDir: File) {
        val json = serializer.toJson()
        val filename = "${namespace.id}-${System.currentTimeMillis()}.json"
        backupDir.resolve(filename).writeText(json)
    }
}

// Usage
suspend fun main() {
    val manager = RemoteConfigManager(
        namespace = Namespace.Payments,
        remoteUrl = "https://config.example.com/payments.json",
        cacheFile = File("cache/payments.json")
    )

    // Initialize with cache, then fetch remote
    manager.initialize()

    // Periodic sync
    Timer().scheduleAtFixedRate(0, 300_000) { // Every 5 minutes
        runBlocking {
            manager.fetchRemote()
        }
    }
}
```

---

## Summary

**Key Takeaways:**

- Use `SnapshotSerializer` for global serialization without automatic loading
- Use `NamespaceSnapshotSerializer` for namespace-scoped serialization with automatic loading
- All deserialization returns `ParseResult` for type-safe error handling
- Patches enable incremental updates without full configuration replacement
- Always validate configurations before loading
- Maintain local caches for offline support
- Use namespace isolation for independent deployment

---

## Next Steps

- **[Registry](Registry.md)** - Namespace management and registry operations
- **[Results](Results.md)** - Error handling with ParseResult and EvaluationResult
- **[Configuration](Configuration.md)** - DSL reference for building configurations
- **[Context](Context.md)** - Custom contexts for domain-specific logic
