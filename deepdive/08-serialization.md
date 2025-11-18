# Engineering Deep Dive: Serialization

**Navigate**: [← Previous: Concurrency Model](07-concurrency-model.md) | [Next: Advanced Patterns →](09-advanced-patterns.md)

---

## Type-Safe JSON Conversion

Configurations need to move between systems: server to client, file to memory, remote service to application. Serialization is the boundary where type safety could break down.

Konditional maintains type safety across this boundary using the **parse-don't-validate** pattern and structured error handling.

## The Serialization Challenge

### What Needs to Be Serialized?

A complete feature flag configuration includes:
- **Features**: Keys, types, namespaces
- **Flags**: Default values, active state, salt
- **Rules**: Targeting conditions (locales, platforms, versions)
- **Values**: Typed values for each rule
- **Rollouts**: Percentage-based gradual releases

### The Type Safety Problem

```kotlin
// Runtime: Type-safe configuration
val definition: FlagDefinition<BooleanEncodeable, Boolean, Context, Namespace.Global>

// Serialized: Untyped JSON string
val json: String = """{"key": "FEATURE", "type": "boolean", "default": true}"""

// Deserialized: How to ensure type safety?
val parsed: FlagDefinition<?, ?, ?, ?> = deserialize(json)
```

**Challenge**: JSON is untyped. How do we maintain type guarantees?

---

## Parse-Don't-Validate Pattern

### Traditional Validation

```kotlin
fun loadConfig(json: String): Configuration {
    val parsed = parseJson(json)
    if (parsed.flags.isEmpty()) throw Exception("No flags")
    if (parsed.flags.any { it.key.isBlank() }) throw Exception("Invalid key")
    return parsed  // Assumes validation succeeded
}
```

**Problems**:
1. **Exceptions**: Control flow via exceptions
2. **Partial validation**: Might miss cases
3. **Loss of information**: Error messages may be generic
4. **Multiple passes**: Validate, then use (might re-check)

### Parse-Don't-Validate

```kotlin
fun loadConfig(json: String): ParseResult<Configuration> {
    return when (val result = SnapshotSerializer.fromJson(json)) {
        is ParseResult.Success -> result
        is ParseResult.Failure -> result  // Structured error
    }
}
```

**Benefits**:
1. **Explicit results**: Success or typed error, no exceptions
2. **Single pass**: Parsing validates
3. **Rich errors**: Structured failure information
4. **Type-driven**: If parse succeeds, type guarantees hold

**Principle**: Parse input into validated domain types. If parsing succeeds, the type system ensures correctness.

---

## ParseResult: Type-Safe Error Handling

### The ParseResult Type

From `ParseResult.kt`:

```kotlin
sealed interface ParseResult<out T> {
    data class Success<T>(val value: T) : ParseResult<T>
    data class Failure(val error: ParseError) : ParseResult<Nothing>
}
```

**Two cases**:
1. **Success**: Contains successfully parsed value of type `T`
2. **Failure**: Contains structured error information

**No exceptions**: Errors are values, not control flow.

### ParseError: Structured Failures

```kotlin
sealed interface ParseError {
    val message: String

    data class InvalidHexId(val input: String, override val message: String) : ParseError
    data class InvalidRollout(val value: Double, override val message: String) : ParseError
    data class InvalidVersion(val input: String, override val message: String) : ParseError
    data class FeatureNotFound(val key: String) : ParseError
    data class FlagNotFound(val key: String) : ParseError
    data class InvalidSnapshot(val reason: String) : ParseError
    data class InvalidJson(val reason: String) : ParseError
}
```

**Each error type**:
- Captures relevant context (input value, reason, etc.)
- Provides human-readable message
- Enables precise error handling

**Example**:
```kotlin
when (val result = SnapshotSerializer.fromJson(json)) {
    is ParseResult.Success -> println("Loaded: ${result.value}")
    is ParseResult.Failure -> when (val error = result.error) {
        is ParseError.InvalidJson -> println("JSON syntax error: ${error.reason}")
        is ParseError.InvalidVersion -> println("Invalid version '${error.input}': ${error.message}")
        is ParseError.FeatureNotFound -> println("Unknown feature: ${error.key}")
        else -> println("Parse error: ${error.message}")
    }
}
```

**Benefits**:
- Type-safe error handling (exhaustive when)
- Actionable error information
- No exception catching

---

## SnapshotSerializer: The Main Interface

### Public API

```kotlin
object SnapshotSerializer {
    fun serialize(configuration: Configuration): String
    fun fromJson(json: String): ParseResult<Configuration>
}
```

**Two operations**:
1. **Serialize**: Configuration → JSON string (always succeeds)
2. **Deserialize**: JSON string → ParseResult<Configuration> (may fail)

### Asymmetry: Why Serialize Always Succeeds

**Serialize** (Configuration → JSON):
```kotlin
fun serialize(configuration: Configuration): String {
    val serializable = configuration.toSerializable()
    return snapshotAdapter.toJson(serializable)
}
```

**Guaranteed to succeed** because:
- Input is already validated (Configuration is a domain type)
- All fields are present and correct
- Type safety enforced at compile time

**Deserialize** (JSON → Configuration):
```kotlin
fun fromJson(json: String): ParseResult<Configuration> {
    return try {
        val serializable = snapshotAdapter.fromJson(json)
            ?: return ParseResult.Failure(ParseError.InvalidJson("null result"))
        serializable.toSnapshot()  // Validation happens here
    } catch (e: Exception) {
        ParseResult.Failure(ParseError.InvalidJson(e.message ?: "Unknown error"))
    }
}
```

**May fail** because:
- JSON may be malformed
- Required fields may be missing
- Values may have wrong types
- Domain constraints may be violated

**Principle**: Domain types are always valid. Parsing from external representation may fail.

---

## JSON Structure

### Top-Level: SerializableSnapshot

```kotlin
@JsonClass(generateAdapter = true)
data class SerializableSnapshot(
    val flags: List<SerializableFlag>
)
```

**JSON format**:
```json
{
  "flags": [...]
}
```

Simple wrapper around a list of flags.

### Flag-Level: SerializableFlag

```kotlin
@JsonClass(generateAdapter = true)
data class SerializableFlag(
    val key: String,
    val type: String,
    val default: FlagValue,
    val active: Boolean = true,
    val salt: String = "v1",
    val values: List<SerializableRule> = emptyList()
)
```

**JSON example**:
```json
{
  "key": "DARK_MODE",
  "type": "boolean",
  "default": {"value": false},
  "active": true,
  "salt": "v1",
  "values": [...]
}
```

**Fields**:
- `key`: Feature identifier
- `type`: Value type (`"boolean"`, `"string"`, `"int"`, `"double"`)
- `default`: Default value with type wrapper
- `active`: Kill switch state
- `salt`: Bucketing version
- `values`: List of conditional rules

### Rule-Level: SerializableRule

```kotlin
@JsonClass(generateAdapter = true)
data class SerializableRule(
    val note: String? = null,
    val locales: Set<String> = emptySet(),
    val platforms: Set<String> = emptySet(),
    val versionRange: VersionRange = Unbounded(),
    val rollout: Double = 100.0,
    val value: FlagValue
)
```

**JSON example**:
```json
{
  "note": "iOS users on v2+",
  "locales": ["en_US"],
  "platforms": ["IOS"],
  "versionRange": {
    "type": "MIN_BOUND",
    "min": {"major": 2, "minor": 0, "patch": 0}
  },
  "rollout": 50.0,
  "value": {"value": true}
}
```

**Fields**:
- `note`: Optional description
- `locales`: Set of locale codes (e.g., `"en_US"`)
- `platforms`: Set of platform names (e.g., `"IOS"`)
- `versionRange`: Version constraint (polymorphic)
- `rollout`: Percentage (0.0-100.0)
- `value`: Target value with type wrapper

---

## Moshi: The JSON Library

### Why Moshi?

**Moshi** is a modern JSON library for Kotlin/JVM.

**Benefits**:
1. **Kotlin-first**: Native Kotlin support, not Java retrofit
2. **Lightweight**: Minimal dependencies
3. **Type-safe**: Compile-time adapter generation
4. **Extensible**: Custom adapters for domain types
5. **Null-safe**: Respects Kotlin nullability

**Alternative**: kotlinx.serialization
- More Kotlin-specific but requires compiler plugin
- Moshi works with plain Kotlin, no plugin needed

### Moshi Setup

```kotlin
fun defaultMoshi(): Moshi {
    return Moshi.Builder()
        .add(FlagValueAdapter.FACTORY)
        .add(VersionRangeAdapter(...))
        .add(
            PolymorphicJsonAdapterFactory.of(VersionRange::class.java, "type")
                .withSubtype(FullyBound::class.java, "MIN_AND_MAX_BOUND")
                .withSubtype(Unbounded::class.java, "UNBOUNDED")
                .withSubtype(LeftBound::class.java, "MIN_BOUND")
                .withSubtype(RightBound::class.java, "MAX_BOUND")
        )
        .add(KotlinJsonAdapterFactory())
        .build()
}
```

**Order matters**: Custom adapters must be added **before** `KotlinJsonAdapterFactory`.

**Why**: Moshi uses first matching adapter. Custom adapters override default reflection-based serialization.

---

## Custom Adapters

### FlagValueAdapter: Type-Safe Value Wrapper

**Challenge**: JSON has limited types (string, number, boolean, null, array, object). We need to preserve our type information.

**Solution**: Wrap values with type discriminator.

**JSON format**:
```json
{"value": true}          // Boolean
{"value": "hello"}       // String
{"value": 42}            // Int
{"value": 3.14}          // Double
```

**Implementation**:
```kotlin
object FlagValueAdapter {
    val FACTORY: JsonAdapter.Factory = object : JsonAdapter.Factory {
        override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
            if (type != FlagValue::class.java) return null

            return object : JsonAdapter<FlagValue>() {
                override fun fromJson(reader: JsonReader): FlagValue {
                    reader.beginObject()
                    var value: Any? = null
                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "value" -> value = readValue(reader)
                        }
                    }
                    reader.endObject()
                    return createFlagValue(value)
                }

                override fun toJson(writer: JsonWriter, value: FlagValue?) {
                    writer.beginObject()
                    writer.name("value")
                    writeValue(writer, value?.value)
                    writer.endObject()
                }
            }
        }
    }
}
```

**Benefits**:
- Type preserved in runtime value
- JSON format simple and readable
- Type checking during deserialization

### VersionRangeAdapter: Polymorphic Types

**Challenge**: `VersionRange` is a sealed interface with 4 subtypes:
- `Unbounded`: No constraints
- `LeftBound`: Minimum version only
- `RightBound`: Maximum version only
- `FullyBound`: Both minimum and maximum

**Solution**: Polymorphic JSON with type discriminator.

**JSON examples**:
```json
// Unbounded
{"type": "UNBOUNDED"}

// LeftBound (>= 2.0.0)
{
  "type": "MIN_BOUND",
  "min": {"major": 2, "minor": 0, "patch": 0}
}

// RightBound (< 3.0.0)
{
  "type": "MAX_BOUND",
  "max": {"major": 3, "minor": 0, "patch": 0}
}

// FullyBound (>= 2.0.0 and < 3.0.0)
{
  "type": "MIN_AND_MAX_BOUND",
  "min": {"major": 2, "minor": 0, "patch": 0},
  "max": {"major": 3, "minor": 0, "patch": 0}
}
```

**Moshi configuration**:
```kotlin
PolymorphicJsonAdapterFactory.of(VersionRange::class.java, "type")
    .withSubtype(FullyBound::class.java, "MIN_AND_MAX_BOUND")
    .withSubtype(Unbounded::class.java, "UNBOUNDED")
    .withSubtype(LeftBound::class.java, "MIN_BOUND")
    .withSubtype(RightBound::class.java, "MAX_BOUND")
```

**How it works**:
1. Read `"type"` field to determine subtype
2. Deserialize remaining fields according to subtype
3. Return instance of specific subtype

**Benefits**:
- Type-safe polymorphism
- Human-readable JSON
- Compile-time exhaustiveness checking

---

## Conversion: Serializable ↔ Domain

### Domain to Serializable

```kotlin
fun Configuration.toSerializable(): SerializableSnapshot {
    val serializableFlags = flags.map { (feature, definition) ->
        definition.toSerializableFlag()
    }
    return SerializableSnapshot(serializableFlags)
}
```

**Process**:
1. Iterate through flags in Configuration
2. Convert each FlagDefinition to SerializableFlag
3. Wrap in SerializableSnapshot

**Always succeeds**: Domain types are already validated.

### Serializable to Domain

```kotlin
fun SerializableSnapshot.toSnapshot(): ParseResult<Configuration> {
    val flagMap = mutableMapOf<Feature<*, *, *, *>, FlagDefinition<*, *, *, *>>()

    for (serializableFlag in flags) {
        when (val result = serializableFlag.toFlagDefinition()) {
            is ParseResult.Success -> {
                val definition = result.value
                flagMap[definition.feature] = definition
            }
            is ParseResult.Failure -> return result  // Propagate error
        }
    }

    return ParseResult.Success(Configuration(flagMap))
}
```

**Process**:
1. Iterate through serializable flags
2. Convert each to FlagDefinition
3. If any conversion fails, return failure immediately
4. If all succeed, create Configuration

**May fail**: External JSON may violate domain constraints.

### Error Propagation

```kotlin
when (val result = serializableFlag.toFlagDefinition()) {
    is ParseResult.Success -> { /* continue */ }
    is ParseResult.Failure -> return result  // Early return
}
```

**Fail-fast**: First error stops processing and returns immediately.

**Alternative**: Accumulate all errors
- More informative (all errors at once)
- More complex (need to aggregate)
- Konditional chooses fail-fast for simplicity

---

## Patch Operations

### What Are Patches?

**Patch**: Incremental configuration update.

Instead of replacing entire configuration:
```kotlin
registry.load(entireNewConfiguration)
```

Apply only changes:
```kotlin
registry.applyPatch(patch)
```

### SerializablePatch Structure

```kotlin
@JsonClass(generateAdapter = true)
data class SerializablePatch(
    val flags: List<SerializableFlag> = emptyList(),
    val removeKeys: Set<String> = emptySet()
)
```

**Two operations**:
1. **Update/Add**: Flags in `flags` list are added or updated
2. **Remove**: Keys in `removeKeys` are deleted

**JSON example**:
```json
{
  "flags": [
    {"key": "NEW_FEATURE", "type": "boolean", "default": {"value": true}}
  ],
  "removeKeys": ["OLD_FEATURE"]
}
```

### Applying Patches

```kotlin
internal fun applyPatch(
    currentConfiguration: Configuration,
    patch: SerializablePatch
): ParseResult<Configuration> {
    val currentSerializable = currentConfiguration.toSerializable()
    val flagMap = currentSerializable.flags.associateBy { it.key }.toMutableMap()

    // Remove flags
    patch.removeKeys.forEach { key ->
        flagMap.remove(key)
    }

    // Add or update flags
    patch.flags.forEach { patchFlag ->
        flagMap[patchFlag.key] = patchFlag
    }

    // Convert back to domain
    val patchedSerializable = SerializableSnapshot(flagMap.values.toList())
    return patchedSerializable.toSnapshot()
}
```

**Process**:
1. Convert current config to mutable map
2. Apply removals
3. Apply updates/additions
4. Convert back to domain type

**Atomicity**: Entire patch succeeds or fails as unit.

### When to Use Patches

**Use patches**:
- Frequent small updates (add one flag)
- Bandwidth-constrained (mobile, IoT)
- Need to track changes explicitly

**Use full snapshots**:
- Initial load
- Periodic refresh (ensure consistency)
- After many incremental patches (to reset state)

---

## Complete Serialization Example

### Domain Configuration

```kotlin
object Features : FeatureContainer<Namespace.Global>(Namespace.Global) {
    val DARK_MODE by boolean(default = false) {
        rule {
            note = "iOS users"
            platforms(Platform.IOS)
            rollout { 50.0 }
        } returns true
    }
}
```

### Serialization

```kotlin
val configuration = Namespace.Global.configuration()
val json = SnapshotSerializer.serialize(configuration)
```

**Resulting JSON**:
```json
{
  "flags": [
    {
      "key": "DARK_MODE",
      "type": "boolean",
      "default": {"value": false},
      "active": true,
      "salt": "v1",
      "values": [
        {
          "note": "iOS users",
          "locales": [],
          "platforms": ["IOS"],
          "versionRange": {"type": "UNBOUNDED"},
          "rollout": 50.0,
          "value": {"value": true}
        }
      ]
    }
  ]
}
```

### Deserialization

```kotlin
when (val result = SnapshotSerializer.fromJson(json)) {
    is ParseResult.Success -> {
        val config = result.value
        Namespace.Global.load(config)
        println("Configuration loaded successfully")
    }
    is ParseResult.Failure -> {
        println("Failed to parse: ${result.error.message}")
    }
}
```

**Type safety maintained**: If deserialization succeeds, configuration is valid.

---

## Error Handling Patterns

### Pattern 1: Log and Skip

```kotlin
when (val result = SnapshotSerializer.fromJson(json)) {
    is ParseResult.Success -> registry.load(result.value)
    is ParseResult.Failure -> {
        logger.error("Config parse failed: ${result.error}")
        // Keep using existing configuration
    }
}
```

**Use case**: Non-critical configuration updates. Better to keep old config than crash.

### Pattern 2: Retry with Backoff

```kotlin
suspend fun loadConfigWithRetry(maxAttempts: Int = 3) {
    repeat(maxAttempts) { attempt ->
        val json = fetchConfigFromServer()
        when (val result = SnapshotSerializer.fromJson(json)) {
            is ParseResult.Success -> {
                registry.load(result.value)
                return
            }
            is ParseResult.Failure -> {
                logger.warn("Attempt ${attempt + 1} failed: ${result.error}")
                delay(2.0.pow(attempt).seconds)  // Exponential backoff
            }
        }
    }
    logger.error("Failed to load config after $maxAttempts attempts")
}
```

**Use case**: Transient network or server issues. Retry may succeed.

### Pattern 3: Fallback to Default

```kotlin
val config = when (val result = SnapshotSerializer.fromJson(json)) {
    is ParseResult.Success -> result.value
    is ParseResult.Failure -> {
        logger.error("Parse failed, using default config: ${result.error}")
        createDefaultConfiguration()
    }
}
```

**Use case**: Application must start even if remote config unavailable.

### Pattern 4: Fail Fast

```kotlin
val config = SnapshotSerializer.fromJson(json).getOrThrow()
registry.load(config)
```

**Use case**: Tests or initialization where invalid config is unacceptable.

---

## Validation During Deserialization

### Domain Constraints Enforced

**StableId validation**:
```kotlin
if (hexId.length < 32) {
    return ParseResult.Failure(
        ParseError.InvalidHexId(hexId, "Hex ID must be at least 32 characters")
    )
}
```

**Rollout validation**:
```kotlin
if (percentage < 0.0 || percentage > 100.0) {
    return ParseResult.Failure(
        ParseError.InvalidRollout(percentage, "Rollout must be 0.0-100.0")
    )
}
```

**Version validation**:
```kotlin
if (!versionString.matches(versionRegex)) {
    return ParseResult.Failure(
        ParseError.InvalidVersion(versionString, "Expected format: x.y.z")
    )
}
```

**Benefit**: Domain types are always valid. If deserialization succeeds, constraints hold.

---

## Testing Serialization

### Round-Trip Test

```kotlin
@Test
fun `serialization round-trip preserves configuration`() {
    val original = createTestConfiguration()

    // Serialize
    val json = SnapshotSerializer.serialize(original)

    // Deserialize
    val parsed = SnapshotSerializer.fromJson(json).getOrThrow()

    // Assert equal
    assertEquals(original, parsed)
}
```

**Verifies**: Serialization preserves all information.

### Invalid Input Test

```kotlin
@Test
fun `deserialization fails gracefully on invalid JSON`() {
    val invalidJson = """{"flags": [{"key": "X", "type": "invalid_type"}]}"""

    when (val result = SnapshotSerializer.fromJson(invalidJson)) {
        is ParseResult.Success -> fail("Should have failed")
        is ParseResult.Failure -> {
            assertContains(result.error.message, "invalid")
        }
    }
}
```

**Verifies**: Invalid input returns structured error, not exception.

### Version Constraint Test

```kotlin
@Test
fun `version range serializes correctly`() {
    val range = LeftBound(Version(2, 0, 0))
    val json = """{"type":"MIN_BOUND","min":{"major":2,"minor":0,"patch":0}}"""

    val serialized = versionRangeAdapter.toJson(range)
    assertEquals(json, serialized)

    val deserialized = versionRangeAdapter.fromJson(json)
    assertEquals(range, deserialized)
}
```

**Verifies**: Complex types serialize correctly.

---

## Performance Considerations

### Serialization Cost

**JSON generation**: ~1-10 milliseconds for typical configurations
- Dominated by string concatenation and encoding
- Proportional to number of flags and rules

**Optimization**: Cache serialized JSON if configuration rarely changes.

### Deserialization Cost

**JSON parsing**: ~5-50 milliseconds for typical configurations
- Parsing text to tokens: ~50%
- Object construction: ~30%
- Validation: ~20%

**Optimization**: Deserialize on background thread to avoid blocking UI.

### Memory Usage

**Temporary objects**: Serialization creates intermediate SerializableSnapshot
- Short-lived (garbage collected quickly)
- ~2x memory of final Configuration during conversion

**Mitigation**: Process large configurations in streaming fashion (not implemented in Konditional, but possible).

---

## Review: Serialization

### Parse-Don't-Validate

**Principle**: Parse into validated domain types. If successful, type guarantees hold.

**Implementation**: `ParseResult<T>` with `Success<T>` or `Failure(ParseError)`.

### JSON Structure

```
SerializableSnapshot
  └─ List<SerializableFlag>
       ├─ key, type, default, active, salt
       └─ List<SerializableRule>
            ├─ locales, platforms, versionRange
            ├─ rollout, note
            └─ value
```

### Custom Adapters

- **FlagValueAdapter**: Type-safe value wrappers
- **VersionRangeAdapter**: Polymorphic version constraints

### Error Handling

Structured errors via `ParseError` enable precise handling:
- `InvalidJson`: Syntax errors
- `InvalidVersion`: Malformed version strings
- `InvalidRollout`: Out-of-range percentages
- etc.

---

## Next Steps

Now that you understand serialization, we can explore advanced usage patterns and real-world architectures.

**Next chapter**: [Advanced Patterns](09-advanced-patterns.md)
- Custom context patterns
- Reusable evaluables
- Multi-namespace architectures
- Remote configuration patterns
- Testing strategies
- Migration patterns

These patterns show how to apply Konditional in complex real-world scenarios.

---

**Navigate**: [← Previous: Concurrency Model](07-concurrency-model.md) | [Next: Advanced Patterns →](09-advanced-patterns.md)
