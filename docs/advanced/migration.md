---
title: Migration
description: Strategies for migrating and evolving feature flag configurations
---

# Migration Strategies

As your application evolves, you'll need to migrate feature flag configurations. This guide covers strategies for:

- Updating flag schemas
- Migrating between versions
- Deprecating old flags
- Evolving configuration structures

## Schema Evolution

### Adding New Flags

New flags can be added without breaking existing configurations:

```kotlin
// Original configuration
val originalFlags = listOf(
    Flag("feature-a", defaultValue = true),
    Flag("feature-b", defaultValue = false)
)

// Add new flag
val updatedFlags = originalFlags + Flag(
    "feature-c",
    defaultValue = true
)
```

When deserializing old configurations, new flags use their default values:

```kotlin
val serializer = SnapshotSerializer()

// Register all flags (including new ones)
updatedFlags.forEach { serializer.register(it) }

// Load old configuration
val oldSnapshot = loadFromStorage()
serializer.deserialize(oldSnapshot) // feature-c uses default value
```

### Removing Flags

To remove a flag safely:

1. **Mark as deprecated** (optional documentation step)
2. **Stop using** in code
3. **Remove after grace period**

```kotlin
// Step 1: Mark as deprecated
@Deprecated("Use feature-c instead", ReplaceWith("feature-c"))
val deprecatedFlag = Flag("old-feature", defaultValue = false)

// Step 2: Migration period - both flags exist
val currentFlags = listOf(
    deprecatedFlag,
    Flag("new-feature", defaultValue = false)
)

// Step 3: Remove old flag (after users have migrated)
val cleanFlags = listOf(
    Flag("new-feature", defaultValue = false)
)
```

### Renaming Flags

Create a migration function to rename flags in stored configurations:

```kotlin
fun migrateRenamedFlag(
    konfig: SerializableSnapshot,
    oldName: String,
    newName: String
): SerializableSnapshot {
    val updatedFlags = konfig.flags.map { flag ->
        if (flag.id == oldName) {
            flag.copy(id = newName)
        } else {
            flag
        }
    }
    return konfig.copy(flags = updatedFlags)
}

// Usage
val oldSnapshot = loadFromStorage()
val migratedSnapshot = migrateRenamedFlag(
    oldSnapshot,
    oldName = "oldFeatureName",
    newName = "newFeatureName"
)
serializer.deserialize(migratedSnapshot)
```

## Type Migrations

### Changing Value Types

When changing a flag's value type, provide a conversion function:

```kotlin
// Original: Boolean flag
val oldFlag = Flag("max-connections", defaultValue = false)

// New: Int flag
val newFlag = Flag("max-connections", defaultValue = 10)

fun migrateBooleanToInt(konfig: SerializableSnapshot): SerializableSnapshot {
    val updatedFlags = konfig.flags.map { flag ->
        if (flag.id == "max-connections" && flag.type == "Boolean") {
            flag.copy(
                type = "Int",
                value = if (flag.value.toBoolean()) "100" else "10"
            )
        } else {
            flag
        }
    }
    return konfig.copy(flags = updatedFlags)
}
```

### Complex Type Evolution

For custom types, handle versioning in your ValueType implementation:

```kotlin
// Version 1
data class ConfigV1(val timeout: Int)

// Version 2 - added retries
data class ConfigV2(val timeout: Int, val retries: Int)

object ConfigType : ValueType<ConfigV2> {
    override val name = "Config"

    override fun deserialize(value: String): ConfigV2 {
        val json = Json.parseToJsonElement(value).jsonObject

        // Check version
        return when (json["version"]?.jsonPrimitive?.int) {
            1 -> {
                // Migrate from V1
                ConfigV2(
                    timeout = json["timeout"]!!.jsonPrimitive.int,
                    retries = 3 // Default value for new field
                )
            }
            2, null -> {
                // V2 or assume current version if no version field
                ConfigV2(
                    timeout = json["timeout"]!!.jsonPrimitive.int,
                    retries = json["retries"]?.jsonPrimitive?.int ?: 3
                )
            }
            else -> throw IllegalArgumentException("Unknown version")
        }
    }

    override fun serialize(value: ConfigV2): String {
        val json = buildJsonObject {
            put("version", 2)
            put("timeout", value.timeout)
            put("retries", value.retries)
        }
        return json.toString()
    }
}
```

## Version-Based Migrations

### Migration Manager

Create a migration manager to handle version transitions:

```kotlin
class MigrationManager {
    private val migrations = mutableMapOf<String, (SerializableSnapshot) -> SerializableSnapshot>()

    fun register(version: String, migration: (SerializableSnapshot) -> SerializableSnapshot) {
        migrations[version] = migration
    }

    fun migrate(konfig: SerializableSnapshot, targetVersion: String): SerializableSnapshot {
        var current = konfig
        val currentVersion = parseVersion(konfig.version.value)
        val target = parseVersion(targetVersion)

        migrations
            .filter { (version, _) ->
                val v = parseVersion(version)
                v > currentVersion && v <= target
            }
            .toSortedMap()
            .forEach { (_, migration) ->
                current = migration(current)
            }

        return current.copy(version = SerializableVersion(targetVersion))
    }

    private fun parseVersion(version: String): Int {
        return version.replace(".", "").toIntOrNull() ?: 0
    }
}

// Usage
val migrationManager = MigrationManager()

migrationManager.register("1.1.0") { konfig ->
    // Migration for 1.1.0
    migrateRenamedFlag(konfig, "old-name", "new-name")
}

migrationManager.register("1.2.0") { konfig ->
    // Migration for 1.2.0
    migrateBooleanToInt(konfig)
}

// Apply migrations
val oldSnapshot = loadFromStorage()
val migratedSnapshot = migrationManager.migrate(oldSnapshot, "1.2.0")
```

## Backward Compatibility

### Maintain Old Flag IDs

Keep a mapping of old to new flag IDs:

```kotlin
class FlagRegistry {
    private val flags = mutableMapOf<String, Flag<*>>()
    private val aliases = mutableMapOf<String, String>()

    fun register(flag: Flag<*>, vararg aliasIds: String) {
        flags[flag.id] = flag
        aliasIds.forEach { alias ->
            aliases[alias] = flag.id
        }
    }

    fun get(id: String): Flag<*>? {
        val actualId = aliases[id] ?: id
        return flags[actualId]
    }
}

// Usage
val registry = FlagRegistry()
registry.register(
    Flag("new-feature-name", defaultValue = true),
    "old-feature-name", // Alias for backward compatibility
    "legacy-name"       // Another alias
)
```

### Graceful Degradation

Handle missing or invalid flags gracefully:

```kotlin
fun <T> safeEvaluate(
    flagId: String,
    defaultValue: T,
    context: EvaluationContext = EvaluationContext()
): T {
    return try {
        val flag = registry.get(flagId) as? Flag<T>
        flag?.evaluate(context) ?: defaultValue
    } catch (e: Exception) {
        logger.warn("Failed to evaluate flag $flagId: ${e.message}")
        defaultValue
    }
}
```

## Rolling Migrations

For large deployments, use rolling migrations:

### Phase 1: Dual Write

Write to both old and new flags:

```kotlin
fun updateFeatureFlag(enabled: Boolean) {
    // Update old flag
    oldFlag.setValue(enabled)

    // Update new flag
    newFlag.setValue(enabled)

    // Persist both
    persistFlags()
}
```

### Phase 2: Dual Read

Read from new flag, fall back to old:

```kotlin
fun isFeatureEnabled(): Boolean {
    return try {
        newFlag.evaluate()
    } catch (e: Exception) {
        logger.warn("Failed to read new flag, falling back to old")
        oldFlag.evaluate()
    }
}
```

### Phase 3: Deprecate Old

After all instances are updated:

```kotlin
fun isFeatureEnabled(): Boolean {
    return newFlag.evaluate()
}
```

### Phase 4: Remove Old

Clean up old flag completely.

## Testing Migrations

### Unit Tests

```kotlin
@Test
fun `test flag rename migration`() {
    val oldSnapshot = SerializableSnapshot(
        version = SerializableVersion("1.0.0"),
        flags = listOf(
            SerializableFlag(
                id = "old-name",
                type = "Boolean",
                value = "true"
            )
        )
    )

    val migrated = migrateRenamedFlag(oldSnapshot, "old-name", "new-name")

    assertEquals("new-name", migrated.flags.first().id)
    assertEquals("true", migrated.flags.first().value)
}
```

### Integration Tests

```kotlin
@Test
fun `test full migration pipeline`() {
    val serializer = SnapshotSerializer()
    val migrationManager = MigrationManager()

    // Setup migrations
    setupMigrations(migrationManager)

    // Load old config
    val oldConfig = loadTestConfig("v1.0.0")

    // Migrate
    val migrated = migrationManager.migrate(oldConfig, "2.0.0")

    // Deserialize and verify
    serializer.deserialize(migrated)

    // Verify all flags work correctly
    assertTrue(featureFlag.evaluate())
}
```

## Best Practices

### Document Migrations

```kotlin
/**
 * Migration 1.5.0 -> 1.6.0
 *
 * Changes:
 * - Renamed `maxConnections` to `connectionLimit`
 * - Changed `enableCache` from Boolean to CacheConfig
 * - Removed deprecated `oldFeature` flag
 *
 * Backward compatibility: Maintains aliases for old flag names
 */
fun migrateToV1_6_0(konfig: SerializableSnapshot): SerializableSnapshot {
    // Migration implementation
}
```

### Version Everything

```kotlin
data class SerializableSnapshot(
    val version: SerializableVersion,
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val flags: List<SerializableFlag>
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 3
    }
}
```

### Test Migrations Thoroughly

- Test migration from every previous version
- Test with missing fields
- Test with invalid data
- Test rollback scenarios

### Provide Migration Tools

```kotlin
// CLI tool for offline migration
fun main(args: Array<String>) {
    val inputFile = args[0]
    val outputFile = args[1]
    val targetVersion = args[2]

    val konfig = loadSnapshot(inputFile)
    val migrated = migrationManager.migrate(konfig, targetVersion)
    saveSnapshot(outputFile, migrated)

    println("Migrated from ${konfig.version} to $targetVersion")
}
```

## Next Steps

- Review [Patch Updates](patch-updates.md) for incremental configuration changes
- Learn about [Custom Types](custom-types.md) for complex flag values
- Explore the [Serialization API](../serialization/api.md) for complete details
