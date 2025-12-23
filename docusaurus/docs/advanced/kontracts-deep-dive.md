# Kontracts Deep Dive

Using Kontracts (Konditional's JSON Schema DSL) for structured flag values with compile-time safety and runtime validation.

---

## What is Kontracts

Kontracts is a standalone, zero-dependency JSON Schema DSL used by Konditional for:
1. **Custom data class validation** — Ensure JSON payloads match Kotlin type expectations
2. **Parse-time safety** — Invalid JSON is rejected before becoming active config
3. **Type-safe schema definitions** — Schemas are defined in Kotlin, not separate JSON files

---

## Basic Usage: Custom Data Class Flags

### Define a Custom Type

```kotlin
import io.amichne.kontracts.dsl.schemaRoot
import io.amichne.kontracts.schema.ObjectSchema
import io.amichne.konditional.core.types.KotlinEncodeable

data class RetryPolicy(
    val maxAttempts: Int = 3,
    val backoffMs: Double = 1000.0,
    val enabled: Boolean = true,
    val strategy: RetryStrategy = RetryStrategy.EXPONENTIAL
) : KotlinEncodeable<ObjectSchema> {
    override val schema = schemaRoot {
        ::maxAttempts of {
            minimum = 1
            maximum = 10
        }
        ::backoffMs of {
            minimum = 0.0
            maximum = 60000.0
        }
        ::enabled of {
            default = true
        }
        ::strategy of {
            enum = RetryStrategy.values().map { it.name }
        }
    }
}

enum class RetryStrategy { CONSTANT, LINEAR, EXPONENTIAL }
```

### Use in Feature Definition

```kotlin
object NetworkConfig : Namespace("network") {
    val RETRY_POLICY by custom<RetryPolicy, Context>(
        default = RetryPolicy()
    ) {
        rule(RetryPolicy(maxAttempts = 5, backoffMs = 2000.0)) {
            platforms(Platform.ANDROID)
        }
    }
}

// Evaluation
val policy: RetryPolicy = NetworkConfig.RETRY_POLICY.evaluate(context)
retryWithPolicy(policy)
```

---

## Schema DSL

### Primitive Types

```kotlin
data class Config(
    val stringField: String,
    val numberField: Double,
    val intField: Int,
    val boolField: Boolean
) : KotlinEncodeable<ObjectSchema> {
    override val schema = schemaRoot {
        ::stringField of {
            minLength = 1
            maxLength = 100
            pattern = "^[a-zA-Z0-9_-]+$"
        }
        ::numberField of {
            minimum = 0.0
            maximum = 100.0
            multipleOf = 0.5
        }
        ::intField of {
            minimum = 1
            maximum = 1000
        }
        ::boolField of {
            default = false
        }
    }
}
```

### Nested Objects

```kotlin
data class DatabaseConfig(
    val host: String,
    val port: Int,
    val credentials: Credentials
) : KotlinEncodeable<ObjectSchema> {
    override val schema = schemaRoot {
        ::host of { minLength = 1 }
        ::port of { minimum = 1; maximum = 65535 }
        ::credentials of {
            // Nested schema
            properties {
                "username" to { type = "string"; minLength = 1 }
                "password" to { type = "string"; minLength = 8 }
            }
        }
    }
}

data class Credentials(
    val username: String,
    val password: String
)
```

### Collections

```kotlin
data class AllowlistConfig(
    val userIds: List<String>,
    val regions: Set<String>,
    val metadata: Map<String, String>
) : KotlinEncodeable<ObjectSchema> {
    override val schema = schemaRoot {
        ::userIds of {
            type = "array"
            items = jsonObject { type = "string" }
            minItems = 0
            maxItems = 1000
            uniqueItems = false
        }
        ::regions of {
            type = "array"
            items = jsonObject { type = "string" }
            uniqueItems = true
        }
        ::metadata of {
            type = "object"
            additionalProperties = jsonObject { type = "string" }
        }
    }
}
```

### Enums

```kotlin
enum class LogLevel { DEBUG, INFO, WARN, ERROR }

data class LogConfig(
    val level: LogLevel,
    val format: String
) : KotlinEncodeable<ObjectSchema> {
    override val schema = schemaRoot {
        ::level of {
            enum = LogLevel.values().map { it.name }
        }
        ::format of {
            enum = listOf("json", "text", "structured")
        }
    }
}
```

---

## Validation at Parse Boundary

When JSON is loaded, Kontracts validates the payload:

### Valid JSON (Passes)

```json
{
  "flags": [
    {
      "key": "feature::network::RETRY_POLICY",
      "defaultValue": {
        "type": "DATA_CLASS",
        "dataClassName": "com.example.RetryPolicy",
        "value": {
          "maxAttempts": 5,
          "backoffMs": 2000.0,
          "enabled": true,
          "strategy": "EXPONENTIAL"
        }
      }
    }
  ]
}
```

```kotlin
when (val result = SnapshotSerializer.fromJson(json)) {
    is ParseResult.Success -> {
        // JSON is valid, schema checks passed
        NetworkConfig.load(result.value)
    }
}
```

### Invalid JSON (Rejected)

```json
{
  "flags": [
    {
      "key": "feature::network::RETRY_POLICY",
      "defaultValue": {
        "type": "DATA_CLASS",
        "value": {
          "maxAttempts": 20,  // Exceeds maximum (10)
          "backoffMs": -500.0,  // Below minimum (0.0)
          "strategy": "INVALID"  // Not in enum
        }
      }
    }
  ]
}
```

```kotlin
when (val result = SnapshotSerializer.fromJson(json)) {
    is ParseResult.Failure -> {
        // Schema validation failed
        logger.error("Parse failed: ${result.error}")
        // Last-known-good config remains active
    }
}
```

---

## Advanced Patterns

### Optional Fields with Defaults

```kotlin
data class FeatureConfig(
    val enabled: Boolean = true,
    val timeout: Long = 5000L,
    val retries: Int = 3
) : KotlinEncodeable<ObjectSchema> {
    override val schema = schemaRoot {
        ::enabled of { default = true }
        ::timeout of { default = 5000L; minimum = 0 }
        ::retries of { default = 3; minimum = 0; maximum = 10 }
    }
}
```

**JSON can omit fields:**

```json
{
  "enabled": false
  // timeout and retries use defaults
}
```

### Conditional Validation

```kotlin
data class PaymentConfig(
    val provider: String,
    val apiKey: String?
) : KotlinEncodeable<ObjectSchema> {
    override val schema = schemaRoot {
        ::provider of {
            enum = listOf("stripe", "paypal", "square")
        }
        ::apiKey of {
            // Required if provider is "stripe"
            if (provider == "stripe") {
                minLength = 32
                maxLength = 64
            }
        }
    }
}
```

### Runtime Type Checking

```kotlin
data class DynamicConfig(
    val value: Any
) : KotlinEncodeable<ObjectSchema> {
    override val schema = schemaRoot {
        ::value of {
            oneOf = listOf(
                jsonObject { type = "string" },
                jsonObject { type = "number" },
                jsonObject { type = "boolean" }
            )
        }
    }
}
```

---

## Schema Reuse

### Extract Common Schemas

```kotlin
object CommonSchemas {
    val EmailSchema = jsonObject {
        type = "string"
        pattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    }

    val UrlSchema = jsonObject {
        type = "string"
        pattern = "^https?://.*"
    }
}

data class ContactConfig(
    val email: String,
    val website: String
) : KotlinEncodeable<ObjectSchema> {
    override val schema = schemaRoot {
        ::email of CommonSchemas.EmailSchema
        ::website of CommonSchemas.UrlSchema
    }
}
```

---

## Testing Custom Types

### Unit Test: Schema Validation

```kotlin
@Test
fun `retry policy validates min attempts`() {
    val invalidJson = """
    {
      "maxAttempts": 0,
      "backoffMs": 1000.0,
      "enabled": true,
      "strategy": "EXPONENTIAL"
    }
    """

    val result = Json.decodeFromString<RetryPolicy>(invalidJson)
    // Expect validation error: maxAttempts below minimum
}

@Test
fun `retry policy accepts valid config`() {
    val validJson = """
    {
      "maxAttempts": 5,
      "backoffMs": 2000.0,
      "enabled": true,
      "strategy": "EXPONENTIAL"
    }
    """

    val config = Json.decodeFromString<RetryPolicy>(validJson)
    assertEquals(5, config.maxAttempts)
    assertEquals(2000.0, config.backoffMs)
}
```

---

## Best Practices

### 1. Keep Schemas Close to Types

```kotlin
// ✓ Good: Schema defined with type
data class Config(...) : KotlinEncodeable<ObjectSchema> {
    override val schema = schemaRoot { ... }
}

// ✗ Bad: Schema separate from type
data class Config(...)
object ConfigSchema { val schema = ... }
```

### 2. Use Strict Bounds

```kotlin
// ✓ Good: Explicit bounds
::maxAttempts of {
    minimum = 1
    maximum = 10
}

// ✗ Bad: No bounds (accepts any value)
::maxAttempts of { }
```

### 3. Provide Sensible Defaults

```kotlin
// ✓ Good: Defaults for optional fields
data class Config(
    val timeout: Long = 5000L,
    val retries: Int = 3
)

// ✗ Bad: No defaults (requires all fields in JSON)
data class Config(
    val timeout: Long,
    val retries: Int
)
```

### 4. Document Complex Schemas

```kotlin
/**
 * Retry policy configuration.
 * - maxAttempts: [1, 10] (default: 3)
 * - backoffMs: [0, 60000] (default: 1000.0)
 * - strategy: CONSTANT | LINEAR | EXPONENTIAL (default: EXPONENTIAL)
 */
data class RetryPolicy(...) : KotlinEncodeable<ObjectSchema> {
    override val schema = schemaRoot { ... }
}
```

---

## Next Steps

- [Fundamentals: Core Primitives](/fundamentals/core-primitives) — Custom data class features
- [API Reference: Core Types](/api-reference/core-types) — `KotlinEncodeable` interface
- [Kontracts Documentation](https://github.com/amichne/kontracts) — Full schema DSL reference
