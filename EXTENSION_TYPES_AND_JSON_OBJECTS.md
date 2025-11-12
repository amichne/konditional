# Extension Types and JSON Object Support

This document describes the newly added support for extension types and JSON object representations in Konditional.

## Overview

The type-safe primitive DSL has been extended to support:

1. **Extension Types** (Custom Wrapper Types): "0-depth primitive-like values" that wrap JSON primitives
2. **JSON Objects**: Complex data structures for HSON-object type representation

Both features maintain compile-time type safety while enabling flexible value representations.

## Extension Types (Custom Wrapper Types)

Extension types are wrapper types that encode to JSON primitives, providing type safety and domain semantics while
remaining serializable.

### Supported Primitive Encodings

- **String**: DateTime, UUID, Email, etc.
- **Int**: Enums (by ordinal), Unix timestamps, etc.
- **Double**: Durations, measurements, etc.
- **Boolean**: Custom flags (less common)

### Example: DateTime Wrapper

```kotlin
data class DateTime(val instant: Instant) {
    fun toIso8601(): String = DateTimeFormatter.ISO_INSTANT.format(instant)

    companion object {
        fun parse(iso8601: String): DateTime = DateTime(Instant.parse(iso8601))
    }
}

// Declare conditional
val CREATED_AT: Conditional.OfCustom<DateTime, String, Context> =
    Conditional.custom("created_at")

// Create encodable value
fun DateTime.toEncodable(): EncodableValue.CustomEncodeable<DateTime, String> =
    EncodableValue.CustomEncodeable.asString(
        value = this,
        encoder = { it.toIso8601() },
        decoder = { DateTime.parse(it) }
    )

// Configure
config {
    CREATED_AT with {
        default(DateTime.now().toEncodable().value)

        rule {
            platforms(Platform.IOS)
        } implies DateTime.parse("2025-01-01T00:00:00Z").toEncodable().value
    }
}
```

### Example: UUID Wrapper

```kotlin
data class UUID(val value: String) {
    companion object {
        fun random(): UUID = UUID(java.util.UUID.randomUUID().toString())
    }
}

val REQUEST_ID: Conditional.OfCustom<UUID, String, Context> =
    Conditional.custom("request_id")

fun UUID.toEncodable(): EncodableValue.CustomEncodeable<UUID, String> =
    EncodableValue.CustomEncodeable.asString(
        value = this,
        encoder = { it.value },
        decoder = { UUID(it) }
    )
```

### Example: Duration (Double encoding)

```kotlin
data class Duration(val millis: Long)

val TIMEOUT: Conditional.OfCustom<Duration, Double, Context> =
    Conditional.custom("timeout")

fun Duration.toEncodable(): EncodableValue.CustomEncodeable<Duration, Double> =
    EncodableValue.CustomEncodeable.asDouble(
        value = this,
        encoder = { it.millis.toDouble() },
        decoder = { Duration(it.toLong()) }
    )
```

## JSON Objects (HSON-Object Type Representation)

JSON objects enable complex data structures as conditional values, providing a "distinct super type of object nodes"
that represent different values based on conditions.

### Use Cases

- **API Configurations**: Different endpoints, timeouts, headers per environment
- **Theme Configurations**: Complete theme objects varying by platform
- **Feature Sets**: Complex feature bundles with limits and metadata
- **Multi-field Configs**: Any scenario requiring structured data as conditional values

### Example: API Configuration

```kotlin
data class ApiConfig(
    val baseUrl: String,
    val timeout: Int,
    val retries: Int,
    val useHttps: Boolean,
    val headers: Map<String, String> = emptyMap()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "baseUrl" to baseUrl,
        "timeout" to timeout,
        "retries" to retries,
        "useHttps" to useHttps,
        "headers" to headers
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): ApiConfig = ApiConfig(
            baseUrl = map["baseUrl"] as String,
            timeout = (map["timeout"] as Number).toInt(),
            retries = (map["retries"] as Number).toInt(),
            useHttps = map["useHttps"] as Boolean,
            headers = (map["headers"] as? Map<String, String>) ?: emptyMap()
        )
    }
}

// Declare conditional
val API_CONFIG: Conditional.OfJsonObject<ApiConfig, Context> =
    Conditional.jsonObject("api_config")

// Create encodable value
fun ApiConfig.toEncodable(): EncodableValue.JsonObjectEncodeable<ApiConfig> =
    EncodableValue.JsonObjectEncodeable.of(
        value = this,
        encoder = { it.toMap() },
        decoder = { ApiConfig.fromMap(it) }
    )

// Configure with HSON-object type representation
config {
    API_CONFIG with {
        default(ApiConfig(
            baseUrl = "https://api.prod.example.com",
            timeout = 30,
            retries = 3,
            useHttps = true
        ).toEncodable().value)

        // Different object node for WEB platform
        rule {
            platforms(Platform.WEB)
        } implies ApiConfig(
            baseUrl = "https://api.dev.example.com",
            timeout = 60,
            retries = 1,
            useHttps = true
        ).toEncodable().value
    }
}
```

### Example: Theme Configuration

```kotlin
data class ThemeConfig(
    val primaryColor: String,
    val secondaryColor: String,
    val fontFamily: String,
    val fontSize: Int,
    val darkModeEnabled: Boolean
)

val THEME: Conditional.OfJsonObject<ThemeConfig, Context> =
    Conditional.jsonObject("app_theme")

config {
    THEME with {
        // Light theme by default
        default(ThemeConfig("#FFFFFF", "#F0F0F0", "Roboto", 14, false).toEncodable().value)

        // Dark theme for specific locales - distinct object node
        rule {
            locales(AppLocale.EN_US)
        } implies ThemeConfig("#1E1E1E", "#2D2D2D", "Roboto", 14, true).toEncodable().value
    }
}
```

## Serialization Format

### Extension Types (Custom Wrappers)

Extension types serialize as their primitive encoding:

```json
{
  "type": "STRING",
  "value": "2025-01-01T00:00:00Z"
}
```

The encoder/decoder functions handle conversion to/from the wrapper type.

### JSON Objects

JSON objects serialize with the `JSON` type:

```json
{
  "type": "JSON",
  "value": {
    "baseUrl": "https://api.prod.example.com",
    "timeout": 30,
    "retries": 3,
    "useHttps": true,
    "headers": {
      "X-Environment": "production"
    }
  }
}
```

Nested structures (maps, lists, nested objects) are fully supported.

## Implementation Details

### Type Hierarchy

```kotlin
sealed interface EncodableValue<T : Any> {
    // Primitives
    data class BooleanEncodeable(value: Boolean)
    data class StringEncodeable(value: String)
    data class IntegerEncodeable(value: Int)
    data class DecimalEncodeable(value: Double)

    // JSON Object
    data class JsonObjectEncodeable<T>(
        value: T,
        encoder: (T) -> Map<String, Any?>,
        decoder: (Map<String, Any?>) -> T
    )

    // Custom Wrapper
    data class CustomEncodeable<T, P>(
        value: T,
        primitiveEncoding: Encoding,
        encoder: (T) -> P,
        decoder: (P) -> T
    )
}
```

### Conditional Interfaces

```kotlin
sealed interface Conditional<S : EncodableValue<T>, T : Any, C : Context, M : Module> {
    // Primitives
    interface OfBoolean<C : Context>
    interface OfString<C : Context>
    interface OfInt<C : Context>
    interface OfDouble<C : Context>

    // JSON Object
    interface OfJsonObject<T : Any, C : Context>

    // Custom Wrapper
    interface OfCustom<T : Any, P : Any, C : Context>
}
```

### Factory Methods

```kotlin
// Primitives
Conditional.boolean(key)
Conditional.string(key)
Conditional.int(key)
Conditional.double(key)

// JSON Object
Conditional.jsonObject<ApiConfig, Context>(key)

// Custom Wrapper
Conditional.custom<DateTime, String, Context>(key)
```

## Type Safety Guarantees

1. **Compile-time enforcement**: Only defined types can be used
2. **No runtime type errors**: Sealed interfaces prevent invalid types
3. **Parse, don't validate**: Types are validated at serialization boundaries
4. **No unchecked casts**: Type parameters ensure correctness (except for unavoidable Map conversions)

## Migration Guide

### From Primitives to Extension Types

```kotlin
// Before: Plain String
val TIMESTAMP: Conditional.OfString<Context> = Conditional.string("timestamp")

// After: DateTime wrapper
data class DateTime(val instant: Instant)
val TIMESTAMP: Conditional.OfCustom<DateTime, String, Context> =
    Conditional.custom("timestamp")
```

### From Any to JSON Objects

```kotlin
// Before: Not possible with type-safe witness pattern

// After: Full JSON object support
data class Config(val url: String, val timeout: Int)
val CONFIG: Conditional.OfJsonObject<Config, Context> =
    Conditional.jsonObject("config")
```

## Best Practices

### Extension Types

1. **Keep encoders/decoders simple**: Pure functions without side effects
2. **Validate in constructors**: Ensure wrapper types maintain invariants
3. **Use companion objects**: Centralize encoding/decoding logic
4. **Prefer existing types**: String for textual data, Int for counts, Double for measurements

### JSON Objects

1. **Implement toMap/fromMap**: Explicit encoding is clearer than reflection
2. **Handle nulls carefully**: Use nullable types in Map representation
3. **Validate on decode**: Throw clear exceptions for malformed data
4. **Keep objects flat when possible**: Nested structures increase complexity
5. **Document expected structure**: Make serialization format explicit

## Examples

See complete working examples:

- [`ExtensionTypesExample.kt`](src/main/kotlin/io/amichne/konditional/example/ExtensionTypesExample.kt)
- [`JsonObjectExample.kt`](src/main/kotlin/io/amichne/konditional/example/JsonObjectExample.kt)

## Limitations

1. **No automatic serialization**: Must provide encoder/decoder functions
2. **Map-based representation**: JSON objects use `Map<String, Any?>` internally
3. **Type erasure at boundaries**: Generic type info lost during serialization
4. **Manual evidence creation**: No automatic derivation of encodable evidence

## Future Enhancements

Potential improvements:

- Automatic encoder/decoder generation via KSP/annotation processing
- First-class support for common types (Instant, UUID, Duration)
- Schema validation for JSON objects
- Better error messages for malformed data
