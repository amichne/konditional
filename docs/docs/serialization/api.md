---
title: API Reference
description: Complete API documentation for Konditional serialization
---


## Overview

This page documents all public APIs in the Konditional serialization package.

---

## SnapshotSerializer

Main interface for serialization operations.

### Class Definition

```kotlin
class SnapshotSerializer(
    moshi: Moshi = defaultMoshi()
)
```

### Methods

#### serialize()

Serializes a `Flags.Snapshot` to JSON string.

```kotlin
fun serialize(snapshot: Flags.Snapshot): String
```

**Parameters:**
- `snapshot` - The snapshot to serialize

**Returns:** Pretty-printed JSON string

**Example:**
```kotlin
val serializer = SnapshotSerializer.default
val json = serializer.serialize(snapshot)
```

---

#### deserialize()

Deserializes JSON string to `Flags.Snapshot`.

```kotlin
fun deserialize(json: String): Flags.Snapshot
```

**Parameters:**
- `json` - JSON string to deserialize

**Returns:** Deserialized `Flags.Snapshot`

**Throws:**
- `JsonDataException` - If JSON is malformed
- `IllegalArgumentException` - If flag keys are not registered

**Example:**
```kotlin
val snapshot = serializer.deserialize(json)
```

---

#### serializePatch()

Serializes a patch to JSON.

```kotlin
fun serializePatch(patch: SerializablePatch): String
```

**Parameters:**
- `patch` - The patch to serialize

**Returns:** JSON string

**Example:**
```kotlin
val patch = SerializablePatch(...)
val json = serializer.serializePatch(patch)
```

---

#### deserializePatch()

Deserializes JSON to a patch.

```kotlin
fun deserializePatch(json: String): SerializablePatch
```

**Parameters:**
- `json` - JSON string containing patch

**Returns:** Deserialized patch

**Throws:**
- `JsonDataException` - If JSON is malformed

**Example:**
```kotlin
val patch = serializer.deserializePatch(patchJson)
```

---

#### applyPatch()

Applies a patch to an existing snapshot.

```kotlin
fun applyPatch(
    currentSnapshot: Flags.Snapshot,
    patch: SerializablePatch
): Flags.Snapshot
```

**Parameters:**
- `currentSnapshot` - Snapshot to patch
- `patch` - Patch to apply

**Returns:** New snapshot with patch applied

**Example:**
```kotlin
val updated = serializer.applyPatch(current, patch)
```

---

#### applyPatchJson()

Applies a patch from JSON to a snapshot.

```kotlin
fun applyPatchJson(
    currentSnapshot: Flags.Snapshot,
    patchJson: String
): Flags.Snapshot
```

**Parameters:**
- `currentSnapshot` - Snapshot to patch
- `patchJson` - JSON string containing patch

**Returns:** New snapshot with patch applied

**Example:**
```kotlin
val updated = serializer.applyPatchJson(current, patchJson)
```

---

### Companion Object

#### defaultMoshi()

Creates the default Moshi instance.

```kotlin
fun defaultMoshi(): Moshi
```

**Returns:** Configured Moshi instance

---

#### default

Default serializer instance.

```kotlin
val default: SnapshotSerializer
```

**Example:**
```kotlin
val serializer = SnapshotSerializer.default
```

---

## ConditionalRegistry

Registry for mapping flag keys to `Conditional` instances.

### Methods

#### register()

Registers a single conditional.

```kotlin
fun <S : Any, C : Context> register(conditional: Conditional<S, C>)
```

**Parameters:**
- `conditional` - The conditional to register

**Example:**
```kotlin
ConditionalRegistry.register(FeatureFlags.DARK_MODE)
```

---

#### registerEnum()

Registers all conditionals from an enum.

```kotlin
inline fun <reified T> registerEnum()
    where T : Enum<T>, T : Conditional<*, *>
```

**Type Parameters:**
- `T` - Enum type implementing `Conditional`

**Example:**
```kotlin
ConditionalRegistry.registerEnum<FeatureFlags>()
```

---

#### get()

Retrieves a conditional by key.

```kotlin
fun <S : Any, C : Context> get(key: String): Conditional<S, C>
```

**Parameters:**
- `key` - String key to look up

**Returns:** Conditional instance

**Throws:**
- `IllegalArgumentException` - If key not found

**Example:**
```kotlin
val flag = ConditionalRegistry.get<Boolean, Context>("dark_mode")
```

---

#### contains()

Checks if a key is registered.

```kotlin
fun contains(key: String): Boolean
```

**Parameters:**
- `key` - String key to check

**Returns:** `true` if registered, `false` otherwise

**Example:**
```kotlin
if (ConditionalRegistry.contains("dark_mode")) {
    // Key is registered
}
```

---

#### clear()

Clears all registrations. **For testing only!**

```kotlin
fun clear()
```

**Example:**
```kotlin
@AfterEach
fun tearDown() {
    ConditionalRegistry.clear()
}
```

---

## SerializableSnapshot

Top-level serializable representation.

### Data Class

```kotlin
@JsonClass(generateAdapter = true)
data class SerializableSnapshot(
    val flags: List<SerializableFlag>
)
```

### Fields

| Field | Type | Description |
|-------|------|-------------|
| `flags` | `List<SerializableFlag>` | List of flag configurations |

---

## SerializableFlag

Serializable flag configuration.

### Data Class

```kotlin
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

### Fields

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `key` | `String` | Yes | - | Unique identifier |
| `type` | `ValueType` | Yes | - | Value type |
| `defaultValue` | `Any` | Yes | - | Default value (primitive) |
| `default` | `SerializableValue` | Yes | - | Typed default wrapper |
| `salt` | `String` | No | `"v1"` | Bucketing salt |
| `isActive` | `Boolean` | No | `true` | Active flag |
| `rules` | `List<SerializableRule>` | No | `[]` | Targeting rules |

---

## SerializableRule

Serializable targeting rule.

### Data Class

```kotlin
@JsonClass(generateAdapter = true)
data class SerializableRule(
    val value: SerializableValue,
    val rampUp: Double = 100.0,
    val note: String? = null,
    val locales: Set<String> = emptySet(),
    val platforms: Set<String> = emptySet(),
    val versionRange: SerializableVersionRange? = null
)
```

### Fields

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `value` | `SerializableValue` | Yes | - | Typed value wrapper |
| `rampUp` | `Double` | No | `100.0` | Percentage 0-100 |
| `note` | `String?` | No | `null` | Description |
| `locales` | `Set<String>` | No | `[]` | Locale set (empty = all) |
| `platforms` | `Set<String>` | No | `[]` | Platform set (empty = all) |
| `versionRange` | `SerializableVersionRange?` | No | `null` | Version constraints |

### Nested Class: SerializableValue

```kotlin
data class SerializableValue(
    val value: Any,
    val type: ValueType
)
```

Wraps a value with its type for type safety.

---

## SerializableVersionRange

Serializable version range.

### Data Class

```kotlin
@JsonClass(generateAdapter = true)
data class SerializableVersionRange(
    val type: VersionRangeType,
    val min: SerializableVersion? = null,
    val max: SerializableVersion? = null
)
```

### Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `type` | `VersionRangeType` | Yes | Range type |
| `min` | `SerializableVersion?` | Conditional | Min version (required for LEFT_BOUND, FULLY_BOUND) |
| `max` | `SerializableVersion?` | Conditional | Max version (required for RIGHT_BOUND, FULLY_BOUND) |

---

## SerializableVersion

Serializable version.

### Data Class

```kotlin
@JsonClass(generateAdapter = true)
data class SerializableVersion(
    val major: Int,
    val minor: Int,
    val patch: Int
)
```

### Fields

| Field | Type | Description |
|-------|------|-------------|
| `major` | `Int` | Major version |
| `minor` | `Int` | Minor version |
| `patch` | `Int` | Patch version |

---

## SerializablePatch

Patch update configuration.

### Data Class

```kotlin
@JsonClass(generateAdapter = true)
data class SerializablePatch(
    val flags: List<SerializableFlag>,
    val removeKeys: List<String> = emptyList()
)
```

### Fields

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `flags` | `List<SerializableFlag>` | - | Flags to add/update |
| `removeKeys` | `List<String>` | `[]` | Flag keys to remove |

---

## Enums

### ValueType

Supported value types.

```kotlin
enum class ValueType {
    BOOLEAN,
    STRING,
    INT,
    LONG,
    DOUBLE
}
```

### VersionRangeType

Version range types.

```kotlin
enum class VersionRangeType {
    UNBOUNDED,
    LEFT_BOUND,
    RIGHT_BOUND,
    FULLY_BOUND
}
```

---

## Extension Functions

### Flags.Snapshot.toJson()

Convenience method to serialize directly.

```kotlin
fun Flags.Snapshot.toJson(
    serializer: SnapshotSerializer = SnapshotSerializer.default
): String
```

**Example:**
```kotlin
val json = snapshot.toJson()
```

---

### SnapshotJsonParser.fromJson()

Convenience method to deserialize.

```kotlin
object SnapshotJsonParser {
    fun fromJson(
        json: String,
        serializer: SnapshotSerializer = SnapshotSerializer.default
    ): Flags.Snapshot
}
```

**Example:**
```kotlin
val snapshot = SnapshotJsonParser.fromJson(json)
```

---

## Conversion Utilities

### Flags.Snapshot.toSerializable()

Converts snapshot to serializable form.

```kotlin
fun Flags.Snapshot.toSerializable(): SerializableSnapshot
```

**Internal use** - Called by `SnapshotSerializer.serialize()`

---

### SerializableSnapshot.toSnapshot()

Converts serializable form to snapshot.

```kotlin
fun SerializableSnapshot.toSnapshot(): Flags.Snapshot
```

**Internal use** - Called by `SnapshotSerializer.deserialize()`

---

## Usage Examples

### Complete Example

```kotlin
import io.amichne.konditional.serialization.*

// Register flags
ConditionalRegistry.registerEnum<FeatureFlags>()

// Create config
val snapshot = ConfigBuilder.buildSnapshot {
    FeatureFlags.DARK_MODE with { default(true) }
}

// Serialize
val serializer = SnapshotSerializer.default
val json = serializer.serialize(snapshot)

// Save to file
File("flags.json").writeText(json)

// Load from file
val loadedJson = File("flags.json").readText()

// Deserialize
val loadedSnapshot = serializer.deserialize(loadedJson)

// Load into runtime
Flags.load(loadedSnapshot)

// Use flags
val context = createContext()
with(Flags) {
    val darkMode = context.evaluate(FeatureFlags.DARK_MODE)
}
```

---

## Best Practices

### 1. Always Register Before Deserializing

```kotlin
// ✅ Correct
ConditionalRegistry.registerEnum<FeatureFlags>()
val snapshot = serializer.deserialize(json)

// ❌ Wrong
val snapshot = serializer.deserialize(json)
ConditionalRegistry.registerEnum<FeatureFlags>() // Too late!
```

### 2. Use Default Serializer

```kotlin
// ✅ Preferred
val serializer = SnapshotSerializer.default

// ⚠️ Only if you need custom Moshi config
val customMoshi = Moshi.Builder()...build()
val serializer = SnapshotSerializer(customMoshi)
```

### 3. Handle Errors

```kotlin
try {
    val snapshot = serializer.deserialize(json)
    Flags.load(snapshot)
} catch (e: JsonDataException) {
    logger.error("Invalid JSON", e)
    // Load fallback
} catch (e: IllegalArgumentException) {
    logger.error("Unregistered flag", e)
    // Load fallback
}
```

---

## See Also

- [Integration Guide](/serialization/integration/)
- [Step-by-Step Tutorial](/serialization/steps/step-01-dependencies/)
- [Full Runthrough](/serialization/runthrough/)
