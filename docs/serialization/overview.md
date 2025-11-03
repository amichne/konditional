---
title: Serialization Overview
description: Complete overview of the Konditional serialization system
---


## What is Serialization?

The Konditional serialization system enables you to externalize your feature flag configurations as JSON files. This allows you to:

- **Separate configuration from code** - Update flags without recompiling
- **Version control configurations** - Track flag changes over time
- **Environment-specific settings** - Different configs for dev/staging/prod
- **Remote configuration** - Load flags from a server or CDN
- **Dynamic updates** - Apply changes without app restarts

  
### Type-Safe

    All values are wrapped with their type information, preventing runtime type mismatches
  
  
### Round-Trip Guaranteed

    Serialized configurations behave identically to programmatic ones
  
  
### Patch Updates

    Apply incremental changes without replacing entire configurations
  
  
### Production Ready

    Comprehensive test coverage with real-world examples
  

## Architecture

The serialization system consists of three main components:

### 1. Serializable Models (`SerializablePatch.kt`)

Type-safe DTOs that represent your configuration in JSON:

```kotlin
@JsonClass(generateAdapter = true)
data class SerializableSnapshot(
    val flags: List<SerializableFlag>
)

@JsonClass(generateAdapter = true)
data class SerializableFlag(
    val key: String,
    val type: ValueType,
    val defaultValue: Any,
    val default: SerializableRule.SerializableValue,
    val salt: String = "v1",
    val isActive: Boolean = true,
    val rules: List<SerializableRule> = emptyList()
)
```

### 2. Conversion Utilities (`ConversionUtils.kt`)

Bidirectional conversion between runtime types and JSON:

- **ConditionalRegistry** - Maps flag keys to runtime instances
- **Extension functions** - Convert between `Flags.Snapshot` and `SerializableSnapshot`
- **Type casting** - Safe conversion with validation

### 3. Serialization API (`SnapshotSerializer.kt`)

Main interface for serialization operations:

```kotlin
val serializer = SnapshotSerializer.default

// Serialize
val json = serializer.serialize(snapshot)

// Deserialize
val loaded = serializer.deserialize(json)

// Apply patches
val updated = serializer.applyPatch(current, patch)
```

## Key Concepts

### Type Safety with SerializableValue

All values are wrapped with their type information to guarantee type safety:

```json
{
  "value": {
    "value": true,
    "type": "BOOLEAN"
  }
}
```

This prevents type mismatches and makes the JSON self-describing.

### The Registry Pattern

Before deserialization, you must register all your `Conditional` instances:

```kotlin
// Register an enum of flags
ConditionalRegistry.registerEnum<MyFeatureFlags>()

// Or register individual flags
ConditionalRegistry.register(MyFlags.SOME_FLAG)
```

This allows the deserializer to map string keys back to typed `Conditional` instances.

### Round-Trip Equality

A core guarantee of the system is that serialized configurations behave identically to the originals:

```kotlin
val original = createConfiguration()
val json = serializer.serialize(original)
val restored = serializer.deserialize(json)

// These produce identical results
original.evaluate(context) == restored.evaluate(context) // true
```

## JSON Structure

### Complete Example

```json
{
  "flags": [
    {
      "key": "enable_compact_cards",
      "type": "BOOLEAN",
      "defaultValue": false,
      "default": {
        "value": false,
        "type": "BOOLEAN"
      },
      "salt": "v1",
      "isActive": true,
      "rules": [
        {
          "value": {
            "value": true,
            "type": "BOOLEAN"
          },
          "rollout": 50.0,
          "note": "US iOS 50% rollout",
          "locales": ["EN_US"],
          "platforms": ["IOS"],
          "versionRange": {
            "type": "MIN_BOUND",
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

### Field Reference

#### Flag Level

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `key` | String | Yes | Unique identifier matching `Conditional.key` |
| `type` | ValueType | Yes | Value type: BOOLEAN, STRING, INT, LONG, DOUBLE |
| `defaultValue` | Any | Yes | Default value when no rules match |
| `default` | SerializableValue | Yes | Typed wrapper for default value |
| `salt` | String | No | Salt for bucketing (default: "v1") |
| `isActive` | Boolean | No | Whether flag is active (default: true) |
| `rules` | Array | No | List of targeting rules |

#### Rule Level

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `value` | SerializableValue | Yes | Typed value to return if rule matches |
| `rollout` | Number | No | Percentage 0-100 (default: 100) |
| `note` | String | No | Description of the rule |
| `locales` | Array | No | Set of locales (empty = all) |
| `platforms` | Array | No | Set of platforms (empty = all) |
| `versionRange` | Object | No | Version constraints |

#### Version Range Types

- **UNBOUNDED** - No version constraints
- **MIN_BOUND** - `version >= min`
- **MAX_BOUND** - `version <= max`
- **MIN_AND_MAX_BOUND** - `min <= version <= max`

## Use Cases

### 1. Development Workflow

```
Developer → ConfigBuilder → Snapshot → Serialize → JSON file
                                                        ↓
                                                   Git commit
```

### 2. CI/CD Pipeline

```
Git → Build → Validate JSON → Deploy to S3/CDN → App downloads → Deserialize
```

### 3. Remote Configuration

```
Server API → Patch JSON → Client downloads → Apply patch → Updated flags
```

### 4. A/B Testing

```
JSON config → Multiple rules with rollout → Stable bucketing → Consistent experience
```

## Next Steps

  
### Integration Guide

    Learn how to integrate serialization into your existing codebase

    [Read the guide →](/serialization/integration/)
  
  
### Step-by-Step Tutorial

    Follow detailed steps to implement serialization from scratch

    [Start tutorial →](/serialization/steps/step-01-dependencies/)
  
  
### Full Runthrough

    See the complete integration in one place

    [View runthrough →](/serialization/runthrough/)
  

::: tip
The serialization system is production-ready with comprehensive test coverage. All 11 serialization tests pass, verifying round-trip equality and patch functionality.
:::
