# Serialization

Konditional provides JSON serialization for feature flag configurations, enabling external configuration management, snapshot storage, and dynamic updates.

## Overview

The serialization system allows you to:

- Export flag configurations to JSON
- Import configurations from JSON
- Apply incremental patches to existing configurations
- Store configurations in databases or file systems
- Load configurations from remote servers

## SnapshotSerializer

The `SnapshotSerializer` class handles all serialization operations.

### Creating a Serializer

```kotlin
// Use default instance
val serializer = SnapshotSerializer.default

// Or create with custom Moshi instance
val customMoshi = Moshi.Builder()
    // ... custom configuration
    .build()
val serializer = SnapshotSerializer(customMoshi)
```

## Serializing Configurations

### Basic Serialization

Export a `Konfig` to JSON:

```kotlin
// Build configuration
val konfig = buildSnapshot {
    MyFeatures.DARK_MODE with {
        default(false)
        rule {
            platforms(Platform.IOS)
        }.implies(true)
    }
}

// Serialize to JSON
val json = SnapshotSerializer.default.serialize(konfig)
```

### Serializing from Registry

Export the current registry state:

```kotlin
// Get current configuration
val currentKonfig = FlagRegistry.konfig()

// Serialize
val json = SnapshotSerializer.default.serialize(currentKonfig)

// Save to file
File("flags.json").writeText(json)
```

## Deserializing Configurations

### Basic Deserialization

Import configuration from JSON:

```kotlin
val json = File("flags.json").readText()

when (val result = SnapshotSerializer.default.deserialize(json)) {
    is ParseResult.Success -> {
        val konfig = result.value
        FlagRegistry.load(konfig)
        println("Configuration loaded successfully")
    }
    is ParseResult.Failure -> {
        println("Failed to parse: ${result.error}")
    }
}
```

### Error Handling

The deserialization API returns `ParseResult` for explicit error handling:

```kotlin
sealed interface ParseResult<out T> {
    data class Success<T>(val value: T) : ParseResult<T>
    data class Failure(val error: ParseError) : ParseResult<Nothing>
}

sealed interface ParseError {
    data class InvalidJson(val message: String) : ParseError
    data class InvalidSnapshot(val message: String) : ParseError
    data class UnsupportedVersion(val version: String) : ParseError
}
```

Usage:

```kotlin
val result = SnapshotSerializer.default.deserialize(json)

result.fold(
    onSuccess = { konfig ->
        FlagRegistry.load(konfig)
    },
    onFailure = { error ->
        when (error) {
            is ParseError.InvalidJson -> logError("Malformed JSON: ${error.message}")
            is ParseError.InvalidSnapshot -> logError("Invalid snapshot: ${error.message}")
            is ParseError.UnsupportedVersion -> logError("Unsupported version: ${error.version}")
        }
    }
)
```

## JSON Format

### Snapshot Structure

```json
{
  "flags": [
    {
      "key": "dark_mode",
      "valueType": "BOOLEAN",
      "defaultValue": false,
      "isActive": true,
      "salt": "v1",
      "rules": [
        {
          "locales": [],
          "platforms": ["IOS"],
          "versionRange": {
            "type": "UNBOUNDED"
          },
          "rollout": 100.0,
          "note": "iOS users get dark mode",
          "value": true
        }
      ]
    }
  ]
}
```

### Version Ranges

```json
// Unbounded (all versions)
{
  "type": "UNBOUNDED"
}

// Minimum bound (>= 2.0.0)
{
  "type": "MIN_BOUND",
  "min": {
    "major": 2,
    "minor": 0,
    "patch": 0
  }
}

// Maximum bound (<= 3.0.0)
{
  "type": "MAX_BOUND",
  "max": {
    "major": 3,
    "minor": 0,
    "patch": 0
  }
}

// Fully bound (>= 2.0.0 and <= 3.0.0)
{
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
```

### Value Types

```json
// Boolean
{
  "valueType": "BOOLEAN",
  "defaultValue": false,
  "rules": [
    {
      "value": true
    }
  ]
}

// String
{
  "valueType": "STRING",
  "defaultValue": "production",
  "rules": [
    {
      "value": "staging"
    }
  ]
}

// Integer
{
  "valueType": "INTEGER",
  "defaultValue": 30,
  "rules": [
    {
      "value": 60
    }
  ]
}

// Decimal
{
  "valueType": "DECIMAL",
  "defaultValue": 0.5,
  "rules": [
    {
      "value": 0.75
    }
  ]
}

// JSON Object
{
  "valueType": "JSON",
  "defaultValue": {
    "baseUrl": "https://api.prod.example.com",
    "timeout": 30
  },
  "rules": [
    {
      "value": {
        "baseUrl": "https://api.staging.example.com",
        "timeout": 60
      }
    }
  ]
}
```

## Patches

Apply incremental updates to configurations without replacing the entire snapshot.

### Creating Patches

Patches are not directly created via DSL but through the internal serialization model. However, you can apply patches from JSON:

```json
{
  "flags": [
    {
      "key": "new_feature",
      "valueType": "BOOLEAN",
      "defaultValue": false,
      "isActive": true,
      "salt": "v1",
      "rules": []
    }
  ],
  "removeKeys": ["old_feature"]
}
```

### Applying Patches

```kotlin
// Get current configuration
val currentKonfig = FlagRegistry.konfig()

// Apply patch from JSON
val patchJson = """
{
  "flags": [...],
  "removeKeys": [...]
}
""".trimIndent()

when (val result = SnapshotSerializer.default.applyPatchJson(currentKonfig, patchJson)) {
    is ParseResult.Success -> {
        val updatedKonfig = result.value
        FlagRegistry.load(updatedKonfig)
    }
    is ParseResult.Failure -> {
        logError("Failed to apply patch: ${result.error}")
    }
}
```

## Remote Configuration

### Loading from Remote Server

```kotlin
class RemoteConfigLoader(
    private val apiClient: HttpClient,
    private val serializer: SnapshotSerializer = SnapshotSerializer.default
) {
    suspend fun loadConfiguration(url: String): Result<Konfig> {
        return try {
            val json = apiClient.get(url).bodyAsText()
            when (val result = serializer.deserialize(json)) {
                is ParseResult.Success -> Result.success(result.value)
                is ParseResult.Failure -> Result.failure(
                    ConfigurationException("Parse error: ${result.error}")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun applyRemotePatch(patchUrl: String): Result<Unit> {
        return try {
            val patchJson = apiClient.get(patchUrl).bodyAsText()
            val currentKonfig = FlagRegistry.konfig()

            when (val result = serializer.applyPatchJson(currentKonfig, patchJson)) {
                is ParseResult.Success -> {
                    FlagRegistry.load(result.value)
                    Result.success(Unit)
                }
                is ParseResult.Failure -> Result.failure(
                    ConfigurationException("Patch error: ${result.error}")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

Usage:

```kotlin
val loader = RemoteConfigLoader(httpClient)

// Initial load
loader.loadConfiguration("https://config.example.com/flags.json")
    .onSuccess { konfig ->
        FlagRegistry.load(konfig)
        println("Configuration loaded")
    }
    .onFailure { error ->
        logError("Failed to load configuration", error)
    }

// Apply incremental update
loader.applyRemotePatch("https://config.example.com/patches/123.json")
    .onSuccess {
        println("Patch applied")
    }
    .onFailure { error ->
        logError("Failed to apply patch", error)
    }
```

### Polling for Updates

```kotlin
class ConfigurationPoller(
    private val loader: RemoteConfigLoader,
    private val pollIntervalMs: Long = 60_000
) {
    private var pollingJob: Job? = null

    fun startPolling(configUrl: String, scope: CoroutineScope) {
        pollingJob = scope.launch {
            while (isActive) {
                try {
                    loader.loadConfiguration(configUrl)
                        .onSuccess { konfig ->
                            FlagRegistry.load(konfig)
                            logInfo("Configuration updated")
                        }
                        .onFailure { error ->
                            logError("Poll failed", error)
                        }
                } catch (e: Exception) {
                    logError("Polling error", e)
                }

                delay(pollIntervalMs)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }
}
```

## Database Storage

### Storing Configurations

```kotlin
class ConfigurationRepository(
    private val database: Database,
    private val serializer: SnapshotSerializer = SnapshotSerializer.default
) {
    fun saveConfiguration(name: String, konfig: Konfig) {
        val json = serializer.serialize(konfig)
        database.execute(
            "INSERT INTO configurations (name, json, updated_at) VALUES (?, ?, ?) " +
            "ON CONFLICT (name) DO UPDATE SET json = ?, updated_at = ?",
            name, json, Instant.now(), json, Instant.now()
        )
    }

    fun loadConfiguration(name: String): Konfig? {
        val json = database.queryString(
            "SELECT json FROM configurations WHERE name = ?",
            name
        ) ?: return null

        return when (val result = serializer.deserialize(json)) {
            is ParseResult.Success -> result.value
            is ParseResult.Failure -> {
                logError("Failed to deserialize configuration: ${result.error}")
                null
            }
        }
    }

    fun listConfigurations(): List<String> {
        return database.queryList(
            "SELECT name FROM configurations ORDER BY name"
        )
    }
}
```

## Versioning Configurations

Track configuration versions:

```kotlin
data class VersionedConfiguration(
    val version: Int,
    val timestamp: Instant,
    val konfig: Konfig,
    val author: String,
    val description: String
)

class VersionedConfigurationRepository(
    private val database: Database,
    private val serializer: SnapshotSerializer = SnapshotSerializer.default
) {
    fun saveVersion(config: VersionedConfiguration) {
        val json = serializer.serialize(config.konfig)
        database.execute(
            """
            INSERT INTO configuration_versions
            (version, timestamp, json, author, description)
            VALUES (?, ?, ?, ?, ?)
            """,
            config.version,
            config.timestamp,
            json,
            config.author,
            config.description
        )
    }

    fun loadVersion(version: Int): VersionedConfiguration? {
        return database.queryOne(
            """
            SELECT version, timestamp, json, author, description
            FROM configuration_versions
            WHERE version = ?
            """,
            version
        ) { rs ->
            val json = rs.getString("json")
            when (val result = serializer.deserialize(json)) {
                is ParseResult.Success -> VersionedConfiguration(
                    version = rs.getInt("version"),
                    timestamp = rs.getInstant("timestamp"),
                    konfig = result.value,
                    author = rs.getString("author"),
                    description = rs.getString("description")
                )
                is ParseResult.Failure -> null
            }
        }
    }

    fun rollback(toVersion: Int) {
        loadVersion(toVersion)?.let { config ->
            FlagRegistry.load(config.konfig)
            logInfo("Rolled back to version $toVersion")
        }
    }
}
```

## Best Practices

### Validation

Validate configurations after deserialization:

```kotlin
fun validateConfiguration(konfig: Konfig): List<String> {
    val errors = mutableListOf<String>()

    konfig.flags.forEach { (feature, definition) ->
        // Check for required flags
        if (feature.key in requiredFlags && !definition.isActive) {
            errors.add("Required flag ${feature.key} is inactive")
        }

        // Validate rollout percentages
        definition.values.forEach { conditionalValue ->
            if (conditionalValue.rule.rollout.value !in 0.0..100.0) {
                errors.add("Invalid rollout for ${feature.key}: ${conditionalValue.rule.rollout.value}")
            }
        }
    }

    return errors
}

// Use validation
when (val result = SnapshotSerializer.default.deserialize(json)) {
    is ParseResult.Success -> {
        val errors = validateConfiguration(result.value)
        if (errors.isEmpty()) {
            FlagRegistry.load(result.value)
        } else {
            logError("Configuration validation failed: $errors")
        }
    }
    is ParseResult.Failure -> logError("Parse error: ${result.error}")
}
```

### Backup and Restore

Implement backup mechanism:

```kotlin
class ConfigurationBackup(
    private val backupDir: File,
    private val serializer: SnapshotSerializer = SnapshotSerializer.default
) {
    fun backup(name: String = "backup-${Instant.now().epochSecond}") {
        val konfig = FlagRegistry.konfig()
        val json = serializer.serialize(konfig)
        val backupFile = File(backupDir, "$name.json")
        backupFile.writeText(json)
        logInfo("Configuration backed up to ${backupFile.absolutePath}")
    }

    fun restore(name: String): Boolean {
        val backupFile = File(backupDir, "$name.json")
        if (!backupFile.exists()) {
            logError("Backup file not found: $name")
            return false
        }

        val json = backupFile.readText()
        return when (val result = serializer.deserialize(json)) {
            is ParseResult.Success -> {
                FlagRegistry.load(result.value)
                logInfo("Configuration restored from $name")
                true
            }
            is ParseResult.Failure -> {
                logError("Failed to restore: ${result.error}")
                false
            }
        }
    }

    fun listBackups(): List<String> {
        return backupDir.listFiles { file -> file.extension == "json" }
            ?.map { it.nameWithoutExtension }
            ?.sorted()
            ?: emptyList()
    }
}
```

### Testing Serialization

Test serialization round-trips:

```kotlin
@Test
fun `serialization round-trip preserves configuration`() {
    val original = buildSnapshot {
        MyFeatures.DARK_MODE with {
            default(false)
            rule {
                platforms(Platform.IOS)
                locales(AppLocale.EN_US)
                versions {
                    min(2, 0, 0)
                }
                rollout = Rollout.of(50.0)
            }.implies(true)
        }
    }

    // Serialize
    val json = SnapshotSerializer.default.serialize(original)

    // Deserialize
    val result = SnapshotSerializer.default.deserialize(json)
    assertTrue(result is ParseResult.Success)

    val deserialized = (result as ParseResult.Success).value

    // Compare
    assertEquals(original.flags.size, deserialized.flags.size)
    // ... additional assertions
}
```

## Next Steps

- **[Architecture](Architecture.md)**: Understand how serialization fits into the overall design
- **[Overview](index.md)**: Back to API overview
