# OpenAPI Generator for Konditional

This package provides tools to generate OpenAPI 3.0 specifications from your JSONSchema DSL definitions.

## Overview

The OpenAPI generator creates a one-to-one mapping between your Kotlin `JsonSchema` definitions and OpenAPI schema specifications. This enables:

- **Automatic API documentation** from your feature flag schemas
- **Client code generation** using tools like OpenAPI Generator
- **Schema validation** for API requests/responses
- **Type-safe contracts** between frontend and backend

## Quick Start

### 1. Register Your Schemas

Create a file to register your schemas (e.g., `AppSchemas.kt`):

```kotlin
package com.yourapp.schemas

import io.amichne.konditional.core.dsl.jsonObject
import io.amichne.konditional.openapi.SchemaRegistry

object AppSchemas {

    val userSchema = jsonObject {
        requiredField("id") { int() }
        requiredField("username") { string() }
        requiredField("email") { string() }
        optionalField("firstName") { string() }
        optionalField("lastName") { string() }
        optionalField("active") { boolean() }
    }

    val productSchema = jsonObject {
        requiredField("sku") { string() }
        requiredField("price") { double() }
        requiredField("inStock") { boolean() }
        optionalField("description") { string() }
        optionalField("tags") { array { string() } }
    }

    val orderSchema = jsonObject {
        requiredField("orderId") { string() }
        requiredField("userId") { int() }
        requiredField("items") {
            array {
                jsonObject {
                    requiredField("productSku") { string() }
                    requiredField("quantity") { int() }
                    requiredField("price") { double() }
                }
            }
        }
        optionalField("total") { double() }
        optionalField("status") { enum<OrderStatus>() }
    }

    // Register all schemas
    init {
        SchemaRegistry.register("User", userSchema)
        SchemaRegistry.register("Product", productSchema)
        SchemaRegistry.register("Order", orderSchema)
    }
}
```

### 2. Generate OpenAPI Spec

Run the Gradle task:

```bash
./gradlew generateOpenApiSpec
```

Or with a custom output path:

```bash
./gradlew generateOpenApiSpec -PoutputPath=docs/api-spec.json
```

### 3. View the Generated Spec

The generated file will look like:

```json
{
  "openapi": "3.0.0",
  "info": {
    "title": "Konditional Feature Flags API",
    "version": "1.0.0",
    "description": "API specification auto-generated from Konditional feature flag schemas"
  },
  "servers": [
    {
      "url": "https://api.example.com/v1",
      "description": "Feature Flags API Server"
    }
  ],
  "paths": {
    "/user": {
      "get": {
        "summary": "Get User",
        "operationId": "getUser",
        "responses": {
          "200": {
            "description": "Successful response",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/components/schemas/User"
                }
              }
            }
          }
        }
      }
    }
  },
  "components": {
    "schemas": {
      "User": {
        "type": "object",
        "properties": {
          "id": { "type": "integer", "format": "int32" },
          "username": { "type": "string" },
          "email": { "type": "string" },
          "firstName": { "type": "string" },
          "lastName": { "type": "string" },
          "active": { "type": "boolean" }
        },
        "required": ["id", "username", "email"]
      }
    }
  }
}
```

## Programmatic Usage

You can also generate specs programmatically:

```kotlin
import io.amichne.konditional.openapi.OpenApiGenerator
import java.io.File

fun main() {
    val generator = OpenApiGenerator(
        title = "My API",
        version = "2.0.0",
        description = "My custom API",
        baseUrl = "https://my-api.com/v2"
    )

    // Register schemas
    generator.registerSchema("User", AppSchemas.userSchema)
    generator.registerSchema("Product", AppSchemas.productSchema)

    // Generate and write
    generator.writeToFile(File("build/my-api-spec.json"))
}
```

Or use the DSL helper:

```kotlin
val spec = buildOpenApiSpec(
    title = "My API",
    version = "1.0.0"
) {
    registerSchema("User", userSchema)
    registerSchema("Product", productSchema)
}

println(spec)  // JSON string
```

## Schema Mapping

The converter maps JsonSchema types to OpenAPI as follows:

| JsonSchema Type | OpenAPI Type | Notes |
|-----------------|--------------|-------|
| `BooleanSchema` | `{"type": "boolean"}` | - |
| `StringSchema` | `{"type": "string"}` | - |
| `IntSchema` | `{"type": "integer", "format": "int32"}` | - |
| `DoubleSchema` | `{"type": "number", "format": "double"}` | - |
| `NullSchema` | `{"type": "null"}` | - |
| `EnumSchema<E>` | `{"type": "string", "enum": [...]}` | Extracts enum values |
| `ObjectSchema` | `{"type": "object", "properties": {...}}` | Includes required fields |
| `ArraySchema` | `{"type": "array", "items": {...}}` | Recursive schema |

## Advanced Features

### Nested Objects

Nested objects are fully supported:

```kotlin
val addressSchema = jsonObject {
    optionalField("street") { string() }
    optionalField("city") { string() }
    optionalField("country") { string() }
}

val userWithAddressSchema = jsonObject {
    requiredField("id") { int() }
    requiredField("name") { string() }
    optionalField("address") { jsonObject {
        optionalField("street") { string() }
        optionalField("city") { string() }
    }}
}
```

### Arrays of Objects

Arrays with object elements work seamlessly:

```kotlin
val orderSchema = jsonObject {
    requiredField("items") {
        array {
            jsonObject {
                requiredField("sku") { string() }
                requiredField("quantity") { int() }
            }
        }
    }
}
```

### Enum Types

Enums are converted to string types with enum constraints:

```kotlin
enum class Status { ACTIVE, INACTIVE, PENDING }

val schema = jsonObject {
    requiredField("status") { enum<Status>() }
}

// Becomes:
// {
//   "type": "string",
//   "enum": ["ACTIVE", "INACTIVE", "PENDING"],
//   "description": "Enum: Status"
// }
```

## Integration with Feature Containers

While the current implementation requires manual schema registration, you can easily integrate with FeatureContainers:

```kotlin
object MyFeatures : FeatureContainer<Namespace.Users>(Namespace.Users) {

    val userSchema = jsonObject {
        requiredField("id") { int() }
        requiredField("name") { string() }
    }

    val USER_CONFIG by jsonObject(default = buildJsonObject(schema = userSchema) {
        "id" to 1
        "name" to "Default"
    }) {
        // rules...
    }

    init {
        // Register schema for OpenAPI generation
        SchemaRegistry.register("UserConfig", userSchema)
    }
}
```

## Use Cases

1. **API Documentation**: Generate OpenAPI specs for Swagger UI or Redoc
2. **Client Generation**: Use OpenAPI Generator to create TypeScript/Python/Java clients
3. **Contract Testing**: Validate API responses against schemas
4. **Developer Experience**: Provide type-safe API contracts to frontend teams
5. **Version Control**: Track API changes through schema diffs

## Future Enhancements

Potential improvements for production use:

- **Annotation processor**: Auto-register schemas at compile-time
- **Reflection-based discovery**: Automatically find all schemas in classpath
- **Schema references**: Support `$ref` for reusable components
- **OpenAPI extensions**: Add custom extensions like `x-feature-flag`
- **Multiple output formats**: Support YAML, JSON Schema, etc.

## Tools & Ecosystem

Generated specs work with:

- **Swagger UI**: Interactive API documentation
- **Redoc**: Beautiful API documentation
- **OpenAPI Generator**: Client/server code generation
- **Postman**: Import for API testing
- **IntelliJ IDEA**: HTTP Client support
