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

Serializes a `Snapshot` to JSON string.

```kotlin
fun serialize(snapshot: Snapshot): String
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

Deserializes JSON string to `Snapshot`.

```kotlin
fun deserialize(json: String): Snapshot
```

**Parameters:**
- `json` - JSON string to deserialize

**Returns:** Deserialized `Snapshot`

**Throws:**
- `JsonDataException` - If JSON is malformed
- `IllegalArgumentException` - If flag keys are not registered

**Example:**
```kotlin
val snapshot = serializer.deserialize(json)
```

---

#### serializePatch()

Serializes a core `SnapshotPatch` to JSON.

```kotlin
fun serializePatch(patch: SnapshotPatch): String
```

**Parameters:**
- `patch` - The SnapshotPatch to serialize

**Returns:** JSON string

**Example:**
```kotlin
val patch = SnapshotPatch.from(snapshot) {
    add(MY_FLAG to flagDefinition)
}
val json = serializer.serializePatch(patch)
```

---

#### deserializePatch()

Deserializes JSON to a serializable patch.

```kotlin
fun deserializePatch(json: String): SerializablePatch
```

**Parameters:**
- `json` - JSON string containing patch

**Returns:** Deserialized SerializablePatch

**Throws:**
- `JsonDataException` - If JSON is malformed

**Example:**
```kotlin
val patch = serializer.deserializePatch(patchJson)
```

---

#### deserializePatchToCore()

Deserializes JSON to a core `SnapshotPatch`.

```kotlin
fun deserializePatchToCore(json: String): SnapshotPatch
```

**Parameters:**
- `json` - JSON string containing patch

**Returns:** Deserialized SnapshotPatch

**Throws:**
- `JsonDataException` - If JSON is malformed
- `IllegalArgumentException` - If flag keys are not registered

**Example:**
```kotlin
val patch = serializer.deserializePatchToCore(patchJson)
```

---

#### applyPatch()

Applies a serializable patch to an existing snapshot.

```kotlin
fun applyPatch(
    currentSnapshot: Snapshot,
    patch: SerializablePatch
): Snapshot
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
    currentSnapshot: Snapshot,
    patchJson: String
): Snapshot
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

## FlagRegistry

Interface for managing feature flag configurations and evaluations.

### Interface Definition

```kotlin
interface FlagRegistry {
    fun load(config: Snapshot)
    fun applyPatch(patch: SnapshotPatch)
    fun <S : Any, C : Context> update(definition: FlagDefinition<S, C>)
    fun getCurrentSnapshot(): Snapshot
    fun <S : Any, C : Context> getFlag(key: Conditional<S, C>): ContextualFeatureFlag<S, C>?
    fun getAllFlags(): Map<Conditional<*, *>, ContextualFeatureFlag<*, *>>
}
```

### Methods

#### load()

Loads a complete flag configuration from the provided snapshot.

```kotlin
fun load(config: Snapshot)
```

**Parameters:**
- `config` - The Snapshot containing the flag configuration to load

**Example:**
```kotlin
val snapshot = ConfigBuilder.buildSnapshot {
    MY_FLAG with { default(true) }
}
SingletonFlagRegistry.load(snapshot)
```

---

#### applyPatch()

Applies an incremental patch to the current configuration.

```kotlin
fun applyPatch(patch: SnapshotPatch)
```

**Parameters:**
- `patch` - The SnapshotPatch to apply

**Example:**
```kotlin
val patch = SnapshotPatch.from(SingletonFlagRegistry.getCurrentSnapshot()) {
    add(MY_FLAG to newDefinition)
    remove(OLD_FLAG)
}
SingletonFlagRegistry.applyPatch(patch)
```

---

#### update()

Updates a single flag definition in the current configuration.

```kotlin
fun <S : Any, C : Context> update(definition: FlagDefinition<S, C>)
```

**Parameters:**
- `definition` - The FlagDefinition to update

**Example:**
```kotlin
SingletonFlagRegistry.update(flagDefinition)
```

---

#### getCurrentSnapshot()

Retrieves the current snapshot of all flag configurations.

```kotlin
fun getCurrentSnapshot(): Snapshot
```

**Returns:** The current Snapshot

**Example:**
```kotlin
val snapshot = SingletonFlagRegistry.getCurrentSnapshot()
```

---

#### getFlag()

Retrieves a specific flag definition from the registry.

```kotlin
fun <S : Any, C : Context> getFlag(key: Conditional<S, C>): ContextualFeatureFlag<S, C>?
```

**Parameters:**
- `key` - The Conditional key for the flag

**Returns:** The ContextualFeatureFlag if found, null otherwise

**Example:**
```kotlin
val flag = SingletonFlagRegistry.getFlag(MY_FLAG)
```

---

#### getAllFlags()

Retrieves all flags from the registry.

```kotlin
fun getAllFlags(): Map<Conditional<*, *>, ContextualFeatureFlag<*, *>>
```

**Returns:** Map of all Conditional keys to their ContextualFeatureFlag definitions

**Example:**
```kotlin
val allFlags = SingletonFlagRegistry.getAllFlags()
```

---

## SingletonFlagRegistry

Default singleton implementation of `FlagRegistry`.

### Object Definition

```kotlin
object SingletonFlagRegistry : FlagRegistry
```

### Description

Thread-safe, in-memory registry for managing feature flags. Uses `AtomicReference` to ensure atomic updates and lock-free reads.

**Example:**
```kotlin
// Load configuration
SingletonFlagRegistry.load(snapshot)

// Evaluate flags (uses SingletonFlagRegistry by default)
val value = context.evaluate(MY_FLAG)

// Use custom registry
val customRegistry: FlagRegistry = MyCustomRegistry()
val value = context.evaluate(MY_FLAG, customRegistry)
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
    val rollout: Double = 100.0,
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
| `rollout` | `Double` | No | `100.0` | Percentage 0-100 |
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
| `min` | `SerializableVersion?` | Conditional | Min version (required for MIN_BOUND, MIN_AND_MAX_BOUND) |
| `max` | `SerializableVersion?` | Conditional | Max version (required for MAX_BOUND, MIN_AND_MAX_BOUND) |

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
    MIN_BOUND,
    MAX_BOUND,
    MIN_AND_MAX_BOUND
}
```

---

## SnapshotPatch

Represents an incremental update to a Snapshot.

### Class Definition

```kotlin
data class SnapshotPatch(
    val flags: Map<Conditional<*, *>, ContextualFeatureFlag<*, *>>,
    val removeKeys: Set<Conditional<*, *>>
)
```

### Methods

#### applyTo()

Applies the patch to a snapshot, creating a new snapshot with the changes.

```kotlin
fun applyTo(snapshot: Snapshot): Snapshot
```

**Parameters:**
- `snapshot` - The snapshot to apply the patch to

**Returns:** A new Snapshot with the patch applied

**Example:**
```kotlin
val updatedSnapshot = patch.applyTo(currentSnapshot)
```

---

### Companion Object Methods

#### from()

Creates a new SnapshotPatch using a builder DSL.

```kotlin
fun from(current: Snapshot, builder: PatchBuilder.() -> Unit): SnapshotPatch
```

**Parameters:**
- `current` - The current snapshot to base the patch on
- `builder` - A builder function to configure the patch

**Returns:** A new SnapshotPatch

**Example:**
```kotlin
val patch = SnapshotPatch.from(currentSnapshot) {
    add(MY_FLAG to myFlagDefinition)
    remove(OLD_FLAG)
}
```

---

#### empty()

Creates an empty patch with no changes.

```kotlin
fun empty(): SnapshotPatch
```

**Returns:** An empty SnapshotPatch

**Example:**
```kotlin
val patch = SnapshotPatch.empty()
```

---

#### toJson()

Serializes a SnapshotPatch to JSON.

```kotlin
fun SnapshotPatch.toJson(
    serializer: SnapshotSerializer = SnapshotSerializer.default
): String
```

**Parameters:**
- `serializer` - The serializer to use

**Returns:** JSON string representation of the patch

**Example:**
```kotlin
val json = patch.toJson()
```

---

#### fromJson()

Deserializes a patch from JSON.

```kotlin
fun fromJson(
    json: String,
    serializer: SnapshotSerializer = SnapshotSerializer.default
): SnapshotPatch
```

**Parameters:**
- `json` - The JSON string to deserialize
- `serializer` - The serializer to use

**Returns:** A SnapshotPatch

**Example:**
```kotlin
val patch = SnapshotPatch.fromJson(patchJson)
```

---

### PatchBuilder

Builder for creating patches with a DSL-style API.

#### add()

Adds or updates a flag in the patch.

```kotlin
fun <S : Any, C : Context> add(entry: Pair<Conditional<S, C>, ContextualFeatureFlag<S, C>>)
```

**Parameters:**
- `entry` - Pair of Conditional key and its flag definition

**Example:**
```kotlin
SnapshotPatch.from(snapshot) {
    add(MY_FLAG to flagDefinition)
}
```

---

#### remove()

Marks a flag for removal in the patch.

```kotlin
fun remove(key: Conditional<*, *>)
```

**Parameters:**
- `key` - The conditional key to remove

**Example:**
```kotlin
SnapshotPatch.from(snapshot) {
    remove(OLD_FLAG)
}
```

---

## Extension Functions

### Snapshot.toJson()

Convenience method to serialize a snapshot directly.

```kotlin
fun Snapshot.toJson(
    serializer: SnapshotSerializer = SnapshotSerializer.default
): String
```

**Example:**
```kotlin
val json = snapshot.toJson()
```

---

### Snapshot.fromJson()

Convenience method to deserialize a snapshot.

```kotlin
fun Snapshot.Companion.fromJson(
    json: String,
    serializer: SnapshotSerializer = SnapshotSerializer.default
): Snapshot
```

**Example:**
```kotlin
val snapshot = Snapshot.fromJson(json)
```

---

### Context.evaluate()

Evaluates a specific feature flag in the context.

```kotlin
fun <S : Any, C : Context> C.evaluate(
    key: Conditional<S, C>,
    registry: FlagRegistry = SingletonFlagRegistry
): S
```

**Parameters:**
- `key` - The feature flag to evaluate
- `registry` - The FlagRegistry to use (defaults to SingletonFlagRegistry)

**Returns:** The evaluated value

**Throws:**
- `IllegalStateException` - If the flag is not found in the registry

**Example:**
```kotlin
// Uses SingletonFlagRegistry by default
val value = context.evaluate(MY_FLAG)

// With custom registry
val value = context.evaluate(MY_FLAG, customRegistry)
```

---

### Context.evaluate() - All Flags

Evaluates all feature flags in the context.

```kotlin
fun <C : Context> C.evaluate(
    registry: FlagRegistry = SingletonFlagRegistry
): Map<Conditional<*, *>, Any?>
```

**Parameters:**
- `registry` - The FlagRegistry to use (defaults to SingletonFlagRegistry)

**Returns:** A map of Conditional keys to their evaluated values

**Example:**
```kotlin
val allValues = context.evaluate()
```

---

## Conversion Utilities

### Snapshot.toSerializable()

Converts snapshot to serializable form.

```kotlin
fun Snapshot.toSerializable(): SerializableSnapshot
```

**Internal use** - Called by `SnapshotSerializer.serialize()`

---

### SerializableSnapshot.toSnapshot()

Converts serializable form to snapshot.

```kotlin
fun SerializableSnapshot.toSnapshot(): Snapshot
```

**Internal use** - Called by `SnapshotSerializer.deserialize()`

---

### SnapshotPatch.toSerializable()

Converts core patch to serializable form.

```kotlin
fun SnapshotPatch.toSerializable(): SerializablePatch
```

**Internal use** - Called by `SnapshotSerializer.serializePatch()`

---

### SerializablePatch.toPatch()

Converts serializable form to core patch.

```kotlin
fun SerializablePatch.toPatch(): SnapshotPatch
```

**Internal use** - Called by `SnapshotSerializer.deserializePatchToCore()`

---

## Usage Examples

### Complete Example

```kotlin
import io.amichne.konditional.serialization.*
import io.amichne.konditional.core.SingletonFlagRegistry
import io.amichne.konditional.core.snapshot.Snapshot
import io.amichne.konditional.core.snapshot.SnapshotPatch

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
SingletonFlagRegistry.load(loadedSnapshot)

// Use flags (SingletonFlagRegistry is the default)
val context = createContext()
val darkMode = context.evaluate(FeatureFlags.DARK_MODE)
```

---

### Patch Update Example

```kotlin
// Get current snapshot
val current = SingletonFlagRegistry.getCurrentSnapshot()

// Create a patch
val patch = SnapshotPatch.from(current) {
    add(NEW_FLAG to newFlagDefinition)
    remove(OLD_FLAG)
}

// Serialize patch
val patchJson = patch.toJson()

// Save patch
File("update.json").writeText(patchJson)

// Later: load and apply patch
val loadedPatch = SnapshotPatch.fromJson(File("update.json").readText())
SingletonFlagRegistry.applyPatch(loadedPatch)

// Or apply directly to a snapshot
val updatedSnapshot = loadedPatch.applyTo(current)
SingletonFlagRegistry.load(updatedSnapshot)
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
    SingletonFlagRegistry.load(snapshot)
} catch (e: JsonDataException) {
    logger.error("Invalid JSON", e)
    // Load fallback
} catch (e: IllegalArgumentException) {
    logger.error("Unregistered flag", e)
    // Load fallback
}
```

### 4. Use Patches for Incremental Updates

```kotlin
// ✅ Efficient - only update what changed
val patch = SnapshotPatch.from(SingletonFlagRegistry.getCurrentSnapshot()) {
    add(NEW_FLAG to definition)
}
SingletonFlagRegistry.applyPatch(patch)

// ⚠️ Less efficient - replaces entire snapshot
val newSnapshot = ConfigBuilder.buildSnapshot { /* ... */ }
SingletonFlagRegistry.load(newSnapshot)
```

---

## See Also

- [Integration Guide](/serialization/integration/)
- [Step-by-Step Tutorial](/serialization/steps/step-01-dependencies/)
- [Full Runthrough](/serialization/runthrough/)
