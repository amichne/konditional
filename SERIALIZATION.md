# Konditional Serialization Guide

This document describes how to use the serialization system for Konditional flag configurations.

## Overview

The Konditional serialization system allows you to:

- **Serialize** `Flags.Snapshot` configurations to JSON
- **Deserialize** JSON back to `Flags.Snapshot`
- **Apply patch updates** to existing configurations
- **Guarantee round-trip equality** - serialized configurations behave identically to the originals

## Quick Start

### 1. Register Your Conditionals

Before deserializing, you must register all your feature flag enums:

```kotlin
// Register a single enum
ConditionalRegistry.registerEnum<SampleFeatureEnum>()

// Or register individual flags
ConditionalRegistry.register(MyFlags.SOME_FLAG)
```

### 2. Create a Configuration

```kotlin
val snapshot = ConfigBuilder.buildSnapshot {
    SampleFeatureEnum.ENABLE_COMPACT_CARDS with {
        default(false)
        boundary {
            rampUp = RampUp.of(50.0)
            locales(AppLocale.EN_US)
            platforms(Platform.IOS)
        }.implies(true)
    }
}
```

### 3. Serialize to JSON

```kotlin
val serializer = SnapshotSerializer.default
val json = serializer.serialize(snapshot)

// Save to file
File("config.json").writeText(json)
```

### 4. Deserialize from JSON

```kotlin
// Load from file
val json = File("config.json").readText()
val snapshot = serializer.deserialize(json)

// Load into Flags
Flags.load(snapshot)
```

## JSON Schema

The serialization system uses the following JSON structure:

```json
{
  "flags": [
    {
      "key": "enable_compact_cards",
      "valueType": "BOOLEAN",
      "defaultValue": false,
      "salt": "v1",
      "isActive": true,
      "rules": [
        {
          "value": true,
          "rampUp": 50.0,
          "note": "US iOS 50% rollout",
          "locales": ["EN_US"],
          "platforms": ["IOS"],
          "versionRange": {
            "type": "LEFT_BOUND",
            "min": {
              "major": 7,
              "minor": 10,
              "patch": 0
            }
          }
        }
      ]
    }
  ]
}
```

### Field Descriptions

#### Flag Level
- **key**: Unique identifier for the flag (matches `Conditional.key`)
- **valueType**: Type of the flag value (`BOOLEAN`, `STRING`, `INT`, `LONG`, `DOUBLE`)
- **defaultValue**: Default value when no rules match
- **salt**: Salt value for stable bucketing (default: `"v1"`)
- **isActive**: Whether the flag is active (default: `true`)
- **rules**: Array of rule configurations

#### Rule Level
- **value**: The value to return if this rule matches
- **rampUp**: Percentage of users to include (0-100, default: 100)
- **note**: Optional description of the rule
- **locales**: Set of matching locales (empty = all)
- **platforms**: Set of matching platforms (empty = all)
- **versionRange**: Version constraint (optional)

#### Version Range Types
- **UNBOUNDED**: No version constraints
- **LEFT_BOUND**: `version >= min`
- **RIGHT_BOUND**: `version <= max`
- **FULLY_BOUND**: `min <= version <= max`

## Patch Updates

Apply incremental updates to existing configurations:

```kotlin
val patch = SerializablePatch(
    flags = listOf(
        SerializableFlag(
            key = "enable_compact_cards",
            valueType = ValueType.BOOLEAN,
            defaultValue = true,
            rules = emptyList()
        )
    ),
    removeKeys = listOf("old_flag_to_remove")
)

val updatedSnapshot = serializer.applyPatch(currentSnapshot, patch)
```

Or from JSON:

```kotlin
val patchJson = """
{
  "flags": [
    {
      "key": "enable_compact_cards",
      "valueType": "BOOLEAN",
      "defaultValue": true,
      "salt": "v2",
      "isActive": true,
      "rules": []
    }
  ],
  "removeKeys": ["old_flag"]
}
"""

val updatedSnapshot = serializer.applyPatchJson(currentSnapshot, patchJson)
```

## Supported Value Types

The serialization system supports the following value types:

- **BOOLEAN**: `true`, `false`
- **STRING**: Any string value
- **INT**: 32-bit integers
- **LONG**: 64-bit integers
- **DOUBLE**: Double-precision floating-point numbers

## Testing

The serialization system includes comprehensive tests that verify:

1. **Simple flag serialization** - Basic flag with default value
2. **Flags with rules** - Complex targeting rules with rampUp
3. **Version ranges** - All version range types
4. **Multiple flags** - Configurations with many flags
5. **Patch updates** - Adding, updating, and removing flags
6. **Round-trip equality** - Serialized configs behave identically

Run the tests:

```bash
./gradlew test --tests "io.amichne.konditional.serialization.*"
```

## Complete Example

```kotlin
// 1. Register flags
ConditionalRegistry.registerEnum<SampleFeatureEnum>()

// 2. Create configuration
val snapshot = ConfigBuilder.buildSnapshot {
    SampleFeatureEnum.ENABLE_COMPACT_CARDS with {
        default(false)
        boundary {
            rampUp = RampUp.of(50.0)
            locales(AppLocale.EN_US)
            platforms(Platform.IOS)
        }.implies(true)
    }
}

// 3. Serialize to JSON
val serializer = SnapshotSerializer.default
val json = serializer.serialize(snapshot)
File("config.json").writeText(json)

// 4. Load from JSON (e.g., in another part of the app)
val loadedJson = File("config.json").readText()
val loadedSnapshot = serializer.deserialize(loadedJson)
Flags.load(loadedSnapshot)

// 5. Evaluate flags
val context = Context(
    locale = AppLocale.EN_US,
    platform = Platform.IOS,
    appVersion = Version.of(1, 0, 0),
    stableId = StableId.of("a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6")
)

with(Flags) {
    val enabled = context.evaluate(SampleFeatureEnum.ENABLE_COMPACT_CARDS)
    println("Compact cards enabled: $enabled")
}
```

## API Reference

### SnapshotSerializer

```kotlin
class SnapshotSerializer {
    companion object {
        val default: SnapshotSerializer
    }

    // Serialize a snapshot to JSON
    fun serialize(snapshot: Flags.Snapshot): String

    // Deserialize JSON to a snapshot
    fun deserialize(json: String): Flags.Snapshot

    // Serialize a patch to JSON
    fun serializePatch(patch: SerializablePatch): String

    // Deserialize JSON to a patch
    fun deserializePatch(json: String): SerializablePatch

    // Apply a patch to a snapshot
    fun applyPatch(currentSnapshot: Flags.Snapshot, patch: SerializablePatch): Flags.Snapshot

    // Apply a patch from JSON
    fun applyPatchJson(currentSnapshot: Flags.Snapshot, patchJson: String): Flags.Snapshot
}
```

### ConditionalRegistry

```kotlin
object ConditionalRegistry {
    // Register a single conditional
    fun <S : Any, C : Context> register(conditional: Conditional<S, C>)

    // Register all conditionals from an enum
    inline fun <reified T> registerEnum() where T : Enum<T>, T : Conditional<*, *>

    // Check if a key is registered
    fun contains(key: String): Boolean

    // Clear all registrations (for testing)
    fun clear()
}
```

### Extension Functions

```kotlin
// Serialize a snapshot directly
fun Flags.Snapshot.toJson(serializer: SnapshotSerializer = SnapshotSerializer.default): String

// Parse JSON to a snapshot
object SnapshotJsonParser {
    fun fromJson(json: String, serializer: SnapshotSerializer = SnapshotSerializer.default): Flags.Snapshot
}
```

## Best Practices

1. **Register flags early** - Call `ConditionalRegistry.registerEnum<>()` during app initialization
2. **Version your salt values** - Change the salt when you want to rebucket users (e.g., `"v1"`, `"v2"`)
3. **Use patch updates** - For incremental changes, use patches instead of replacing entire configs
4. **Test round-trips** - Always verify that deserialized configs behave identically to originals
5. **Document rules** - Use the `note` field to explain why a rule exists

## Limitations

1. **Custom Rule Types**: Only the base `Rule<C>` class is supported. Custom rule subclasses require additional serialization logic.
2. **Value Types**: Only primitive types and strings are supported. Custom objects require additional adapters.
3. **Registry Requirement**: All `Conditional` instances must be registered before deserialization.

## Architecture

The serialization system consists of several components:

- **SerializableModels.kt**: DTOs for JSON serialization
- **ConversionUtils.kt**: Bidirectional conversion between runtime types and DTOs
- **SnapshotSerializer.kt**: Main serialization API
- **ConditionalRegistry.kt**: Runtime registry for flag lookups

This design ensures:
- Type safety through generic preservation
- Clean separation between runtime and serializable types
- Easy extensibility for new features
