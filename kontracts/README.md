# Kontracts

Type-safe JSON Schema DSL for Kotlin with compile-time contract specification.

## Overview

Kontracts provides a powerful, type-safe DSL for defining JSON schemas in Kotlin. It leverages Kotlin's type system and context receivers to create schemas that are validated at compile-time, preventing runtime errors and ensuring type safety.

## Features

- **Type-Inferred DSL**: Automatic schema type detection from Kotlin property types
- **Compile-Time Safety**: Catch schema errors at compile time, not runtime
- **Custom Type Mapping**: Map custom domain types to primitive schemas with conversion logic
- **Runtime Validation**: Validate JSON values against schemas with detailed error messages
- **Zero Dependencies**: Only depends on Kotlin stdlib
- **OpenAPI Compatible**: JSON Schema definitions compatible with OpenAPI 3.1 specification

## Installation

```kotlin
// build.gradle.kts
dependencies {
    implementation(project(":kontracts"))  // Currently a submodule
    // Will be published to Maven Central in future releases
}
```

## Usage

### Type-Inferred DSL (Recommended)

The modern type-inferred DSL automatically detects schema types from Kotlin property types:

```kotlin
import io.amichne.kontracts.dsl.*
import io.amichne.kontracts.schema.JsonSchema

data class UserSettings(
    val theme: String = "light",
    val notificationsEnabled: Boolean = true,
    val maxRetries: Int = 3,
    val timeout: Double = 30.0,
) {
    companion object {
        val schema = schemaRoot {
            // Type-inferred: property type determines schema type
            ::theme of {
                minLength = 1
                maxLength = 50
                description = "UI theme preference"
                enum = listOf("light", "dark", "auto")
            }

            ::notificationsEnabled of {
                description = "Enable push notifications"
                default = true
            }

            ::maxRetries of {
                minimum = 0
                maximum = 10
                description = "Maximum retry attempts"
            }

            ::timeout of {
                minimum = 0.0
                maximum = 300.0
                description = "Request timeout in seconds"
            }
        }
    }
}
```

### Custom Type Mapping

Map custom domain types to primitive schemas:

```kotlin
data class UserId(val value: String)
data class Email(val value: String)

data class UserConfig(
    val userId: UserId,
    val email: Email,
) {
    companion object {
        val schema = schemaRoot {
            // Map UserId to String schema with validation
            ::userId asString {
                represent = { this.value }
                pattern = "[A-Z0-9]{8}"
                minLength = 8
                maxLength = 8
                description = "Unique 8-character user identifier"
            }

            // Map Email to String schema with format
            ::email asString {
                represent = { this.value }
                format = "email"
                pattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
                description = "User email address"
            }
        }
    }
}
```

### Legacy Field-Based DSL

Also supports an older field-based DSL:

```kotlin
val schema = jsonObject {
    field("id", required = true) { int() }
    field("name", required = true) { string() }
    field("email") { string() }
    field("settings") {
        jsonObject {
            field("theme") { string() }
            field("notifications") { boolean() }
        }
    }
}
```

### Runtime Validation

```kotlin
// Create JSON values
val obj = buildJsonObject {
    "id" to 123
    "name" to "John Doe"
    "email" to "john@example.com"
}

// Validate against schema
val result = obj.validate(schema)
if (result.isValid) {
    println("Valid!")
} else {
    println("Validation error: ${result.getErrorMessage()}")
}
```

## Architecture

Kontracts consists of three main components:

### 1. Schema Types (`io.amichne.kontracts.schema.JsonSchema`)

Sealed class hierarchy representing JSON Schema types:
- `BooleanSchema`, `StringSchema`, `IntSchema`, `DoubleSchema`
- `EnumSchema<E>` for enum types
- `ObjectSchema` for structured objects
- `ArraySchema` for arrays
- `NullSchema` for nullable values

### 2. Runtime Values (`io.amichne.kontracts.value.JsonValue`)

Sealed class hierarchy for runtime JSON values:
- `JsonBoolean`, `JsonString`, `JsonNumber`, `JsonNull`
- `JsonObject` with typed fields
- `JsonArray` with homogeneous elements

### 3. DSL Builders (`io.amichne.kontracts.dsl`)

Type-safe DSL for schema construction:
- `schemaRoot { }` - Entry point for type-inferred DSL
- `::property of { }` - Type-inferred property schema
- `asString`, `asInt`, `asDouble`, `asBoolean` - Custom type mapping
- `jsonObject { }` - Legacy field-based DSL

## Future Enhancements

### Version 0.2.0
- OpenAPI 3.1 schema generation
- Enhanced validation with JSON path errors
- Schema documentation generator (Markdown/HTML)

### Version 1.0.0
- Remove deprecated legacy DSL
- Publish to Maven Central
- Pluggable serialization adapters (Moshi, kotlinx.serialization)

### Standalone Repository
- Split kontracts to separate repository
- Independent release cycle from Konditional
- Konditional depends on published artifact

## Contributing

This library is currently part of the Konditional project but is designed to be extracted as a standalone library. Contributions are welcome!


```mermaid
classDiagram
    direction LR
    class JsonSchema {
        String description
        Object default
        Boolean deprecated
        String title
        Boolean nullable
        Object example
    }
    class ObjectTraits {
        <<Interface>>
        + validateRequiredFields(Set<String>) ValidationResult
        + validateRequiredFields(Set<String>) ValidationResult
        Map<String, FieldSchema>> fields
        Set<String> required
    }
    class OpenApiProps {
        <<Interface>>
        String description
        Object default
        Boolean deprecated
        String title
        Boolean nullable
        Object example
    }
    class Valid {
        Boolean valid
    }

    class ValidationResult {
        Boolean valid
        Boolean invalid
        String errorMessage
    }

    class data arraySchema {
String description
Int maxItems
Object default
Boolean deprecated
Boolean uniqueItems
String title
Object example
JsonSchema elementSchema
Int minItems
Boolean nullable
 }

class data  BooleanSchema {
String description
Object default
Boolean deprecated
String title
Boolean nullable
Object example
}
class data  DoubleSchema {
String description
Object default
Boolean deprecated
String format
String title
Object example
Double maximum
Double minimum
List<Double> enum
Boolean nullable
}
class data  EnumSchema<E> {
String description
Object default
Boolean deprecated
KClass<E> enumClass
String title
Object example
Boolean nullable
List<E> values
 }
class data  FieldSchema {
String description
Boolean deprecated
Boolean required
JsonSchema schema
Object defaultValue
}
class data  IntSchema {
String description
Object default
Boolean deprecated
String title
Int maximum
Object example
List<Int> enum
Int minimum
Boolean nullable
 }
class data  Invalid {
String message
}
class data  NullSchema {
String description
Object default
Boolean deprecated
String title
Boolean nullable
Object example
}
class data  ObjectSchema {
String description
Object default
Boolean deprecated
String title
Set<String> required
Object example
Map<String, FieldSchema> fields
Boolean nullable
}
class data  RootObjectSchema {
String description
Object default
Boolean deprecated
String title
Set<String> required
Object example
Map<String, FieldSchema> fields
Boolean nullable
}
class data  StringSchema {
String description
Int minLength
Object default
Boolean deprecated
String format
String title
Int maxLength
Object example
List<String> enum
Boolean nullable
String pattern
}

JsonSchema  ..>  OpenApiProps
JsonSchema  -->  ObjectTraits
ValidationResult  -->  Valid
Valid  -->  ValidationResult
JsonSchema  -->  ValidationResult
data  ArraySchema  -->  JsonSchema
JsonSchema  -->  data  ArraySchema
data  BooleanSchema  -->  JsonSchema
JsonSchema  -->  data  BooleanSchema
JsonSchema  -->  data  DoubleSchema
data  DoubleSchema  -->  JsonSchema
JsonSchema  -->  data  EnumSchema<E>
data  EnumSchema<E>  -->  JsonSchema
JsonSchema  -->  data  FieldSchema
JsonSchema  -->  data  IntSchema
data  IntSchema  -->  JsonSchema
data  Invalid  -->  ValidationResult
ValidationResult  -->  data  Invalid
JsonSchema  -->  data  NullSchema
data  NullSchema  -->  JsonSchema
JsonSchema  -->  data  ObjectSchema
data  ObjectSchema  -->  JsonSchema
data  ObjectSchema  ..>  ObjectTraits
JsonSchema  -->  data  RootObjectSchema
data  RootObjectSchema  -->  JsonSchema
data  RootObjectSchema  ..>  ObjectTraits
JsonSchema  -->  data  StringSchema
data  StringSchema  -->  JsonSchema


```

## License

MIT License - Same as parent Konditional project
