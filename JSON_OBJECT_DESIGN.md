# JSON Object Support - Design Document

## Overview

Adding strongly-typed JSON object and array support to Konditional's type system. This will allow feature flags to return complex structured data while maintaining type safety.

## Design Goals

1. **Type Safety**: Strongly typed schemas defined at compile time
2. **Composability**: JSON objects can contain primitives, enums, other objects, and arrays
3. **Homogeneous Arrays**: Arrays contain only one type to simplify parsing
4. **Validation**: Runtime validation against schemas
5. **Serialization**: Full JSON serialization/deserialization support

## Architecture

### 1. Type Hierarchy

```
JsonValue (sealed class)
├── JsonPrimitive (sealed class)
│   ├── JsonBoolean(value: Boolean)
│   ├── JsonString(value: String)
│   ├── JsonNumber(value: Double)
│   └── JsonNull
├── JsonObject(fields: Map<String, JsonValue>, schema: JsonObjectSchema)
└── JsonArray(elements: List<JsonValue>, elementType: JsonSchema)
```

### 2. Schema System

```kotlin
sealed class JsonSchema {
    // Primitive schemas
    object BooleanSchema : JsonSchema()
    object StringSchema : JsonSchema()
    object IntSchema : JsonSchema()
    object DoubleSchema : JsonSchema()
    object EnumSchema<E : Enum<E>>(val enumClass: KClass<E>) : JsonSchema()

    // Complex schemas
    data class ObjectSchema(
        val fields: Map<String, FieldSchema>,
        val required: Set<String> = emptySet()
    ) : JsonSchema()

    data class ArraySchema(
        val elementSchema: JsonSchema
    ) : JsonSchema()

    data class FieldSchema(
        val schema: JsonSchema,
        val required: Boolean = false,
        val default: JsonValue? = null
    )
}
```

### 3. DSL for Schema Definition

```kotlin
// Example usage
val userSchema = jsonObject {
    field("id", required = true) { int() }
    field("name", required = true) { string() }
    field("email") { string() }
    field("role") { enum<UserRole>() }
    field("settings") {
        jsonObject {
            field("theme") { enum<Theme>() }
            field("notifications") { boolean() }
        }
    }
    field("tags") { array { string() } }
}

// In a feature container
object MyFeatures : FeatureContainer<Namespace.Users>(Namespace.Users) {
    val userConfig by jsonObject(default = defaultUser, schema = userSchema) {
        rule<Context> {
            platforms(Platform.WEB)
        } returns webUser
    }
}
```

### 4. Type-Safe Accessors

```kotlin
// Schema-aware accessors
class TypedJsonObject<S : JsonObjectSchema>(
    val value: JsonObject,
    val schema: S
) {
    inline fun <reified T> get(key: String): T? {
        val fieldSchema = schema.fields[key] ?: return null
        val jsonValue = value.fields[key] ?: return null
        return when (fieldSchema.schema) {
            is JsonSchema.BooleanSchema -> (jsonValue as JsonBoolean).value as T
            is JsonSchema.StringSchema -> (jsonValue as JsonString).value as T
            is JsonSchema.IntSchema -> (jsonValue as JsonNumber).value.toInt() as T
            // ... etc
        }
    }

    inline fun <reified T> getOrDefault(key: String, default: T): T {
        return get(key) ?: default
    }
}
```

### 5. Encodeable Types

```kotlin
// JsonObjectEncodeable wraps a JsonObject with its schema
data class JsonObjectEncodeable(
    override val value: JsonObject,
    val schema: JsonObjectSchema
) : EncodableValue<JsonObject> {
    override val encoding: Encoding = Encoding.JSON_OBJECT

    fun toTyped(): TypedJsonObject<JsonObjectSchema> =
        TypedJsonObject(value, schema)
}

// JsonArrayEncodeable wraps a JsonArray with element type
data class JsonArrayEncodeable<T : Any>(
    override val value: JsonArray,
    val elementSchema: JsonSchema
) : EncodableValue<JsonArray> {
    override val encoding: Encoding = Encoding.JSON_ARRAY
}
```

## Implementation Plan

### Phase 1: Core Types (This PR)

1. **ValueType enum**: Add `JSON_OBJECT` and `JSON_ARRAY`
2. **JsonValue sealed class**: Runtime representation of JSON values
3. **JsonSchema sealed class**: Compile-time schema definitions
4. **JsonObjectEncodeable/JsonArrayEncodeable**: Integration with EncodableValue

### Phase 2: Schema DSL

5. **JsonSchemaBuilder**: DSL for defining schemas
6. **Validation**: Runtime validation of JsonValue against JsonSchema
7. **Type-safe accessors**: TypedJsonObject for schema-aware access

### Phase 3: Feature Integration

8. **JsonObjectFeature/JsonArrayFeature**: Feature types for JSON
9. **FeatureContainer support**: Add `jsonObject()` and `jsonArray()` methods
10. **Evidence support**: EncodableEvidence for JSON types

### Phase 4: Serialization

11. **FlagValue support**: Add JsonObjectValue and JsonArrayValue
12. **JSON serialization**: Moshi adapters for JsonValue
13. **Schema serialization**: Serialize schemas for validation

### Phase 5: Testing

14. **Unit tests**: Schema validation, type safety, serialization
15. **Integration tests**: Feature evaluation with JSON objects
16. **Edge cases**: Nested objects, arrays of objects, null handling

## Example Use Cases

### Use Case 1: User Configuration

```kotlin
enum class Theme { LIGHT, DARK }
enum class NotificationFrequency { REALTIME, HOURLY, DAILY }

val userConfigSchema = jsonObject {
    field("userId", required = true) { string() }
    field("theme") { enum<Theme>() }
    field("notifications") {
        jsonObject {
            field("enabled") { boolean() }
            field("frequency") { enum<NotificationFrequency>() }
        }
    }
    field("favoriteCategories") { array { string() } }
}

object UserFeatures : FeatureContainer<Namespace.Users>(Namespace.Users) {
    val config by jsonObject(
        default = defaultUserConfig,
        schema = userConfigSchema
    ) {
        rule<Context> {
            locales(AppLocale.JAPAN)
        } returns japaneseUserConfig
    }
}

// Usage
val config = context.evaluate(UserFeatures.config)
val theme = config.get<Theme>("theme")
val notificationsEnabled = config.getNested("notifications", "enabled") as? Boolean
```

### Use Case 2: Feature Rollout Configuration

```kotlin
val rolloutConfigSchema = jsonObject {
    field("enabled") { boolean() }
    field("percentage") { int() }
    field("targetRegions") { array { string() } }
    field("overrides") {
        jsonObject {
            field("vipUsers") { boolean() }
            field("betaTesters") { boolean() }
        }
    }
}

object RolloutFeatures : FeatureContainer<Namespace.Rollout>(Namespace.Rollout) {
    val newCheckout by jsonObject(
        default = disabledRollout,
        schema = rolloutConfigSchema
    ) {
        rule<Context> {
            platforms(Platform.WEB)
        } returns webRolloutConfig
    }
}
```

### Use Case 3: API Configuration

```kotlin
val apiConfigSchema = jsonObject {
    field("baseUrl", required = true) { string() }
    field("timeout") { int() }
    field("retryAttempts") { int() }
    field("headers") {
        array {
            jsonObject {
                field("key") { string() }
                field("value") { string() }
            }
        }
    }
}
```

## Serialization Format

### JSON Object

```json
{
  "type": "JSON_OBJECT",
  "schema": {
    "fields": {
      "userId": {"type": "STRING", "required": true},
      "theme": {"type": "ENUM", "enumClass": "com.example.Theme"},
      "notifications": {
        "type": "JSON_OBJECT",
        "fields": {
          "enabled": {"type": "BOOLEAN"},
          "frequency": {"type": "ENUM", "enumClass": "com.example.NotificationFrequency"}
        }
      }
    }
  },
  "value": {
    "userId": "user123",
    "theme": "DARK",
    "notifications": {
      "enabled": true,
      "frequency": "HOURLY"
    }
  }
}
```

### JSON Array

```json
{
  "type": "JSON_ARRAY",
  "elementType": {"type": "STRING"},
  "value": ["tag1", "tag2", "tag3"]
}
```

## Type Safety Guarantees

1. **Compile-time schema validation**: Schemas defined using type-safe DSL
2. **Runtime value validation**: Values validated against schemas
3. **Type-safe accessors**: TypedJsonObject provides type-checked access
4. **Homogeneous arrays**: Array element type enforced at runtime
5. **Required field validation**: Required fields checked during construction

## Migration Path

Existing code using primitive types continues to work. JSON objects are opt-in:

```kotlin
// Before
val config by string(default = """{"key": "value"}""")

// After
val config by jsonObject(default = configObject, schema = configSchema)
```

## Performance Considerations

1. **Schema validation**: Only performed once during construction
2. **Lazy parsing**: JSON strings parsed on-demand
3. **Caching**: Validated objects cached to avoid re-validation
4. **Immutability**: JsonValue types are immutable for thread safety

## Limitations

1. **Homogeneous arrays only**: No mixed-type arrays (by design)
2. **No recursive schemas**: Schemas cannot reference themselves (for now)
3. **No schema evolution**: Changing schemas requires migration
4. **No union types**: Fields can't have multiple possible types (for now)

## Future Enhancements

1. **Recursive schemas**: Support for self-referential types
2. **Schema versioning**: Handle schema evolution
3. **Union types**: Allow fields with multiple possible types
4. **Validation rules**: Custom validators (e.g., string length, number ranges)
5. **Schema inheritance**: Extend schemas
6. **Null safety**: Better handling of optional vs nullable fields
