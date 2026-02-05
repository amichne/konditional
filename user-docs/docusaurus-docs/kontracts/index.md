# kontracts

:::caution Work in progress
This page is a draft and may change without notice. Treat it as lightly reliable until stabilized.
:::

Type-safe JSON Schema DSL for Kotlin used by Konditional for structured value validation.

## When to Use This Module

You should use `kontracts` when you need to:

- Define custom structured values in feature flags with type-safe validation
- Express JSON Schema constraints using Kotlin's type system instead of raw JSON
- Validate configuration payloads at the JSON boundary with explicit schemas
- Ensure custom value types have compile-time schema definitions

## What You Get

- **Type-inferred DSL**: Define schemas with `schemaRoot { ::property of { ... } }`
- **Compile-time safety**: Schema structure follows data class structure
- **Runtime validation**: Validate JSON against schemas at the configuration boundary
- **Explicit constraints**: Minimum, maximum, patterns, and custom validation rules

## Alternatives

Without this module, you would need to:

- Write untyped JSON Schema definitions as raw JSON or YAML strings
- Manually ensure schema definitions stay in sync with Kotlin data classes
- Build custom validation logic for every structured value type

## Installation

```kotlin
dependencies {
  implementation(project(":kontracts"))
}
```

## Schema DSL



```kotlin
data class UserId(val value: String)

data class UserSettings(
    val userId: UserId,
    val theme: String = "light",
    val notificationsEnabled: Boolean = true,
    val maxRetries: Int = 3
) : Konstrained<ObjectSchema> {
    override val schema = schemaRoot {
        ::userId asString {
            represent = { this.value }
            pattern = "[A-Z0-9]{8}"
        }
        ::theme of { minLength = 1 }
        ::notificationsEnabled of { default = true }
        ::maxRetries of { minimum = 0 }
    }
}
```

## Runtime validation

```kotlin
val result = jsonValue.validate(schema)
if (!result.isValid) {
    println(result.getErrorMessage())
}
```


## Next steps

- [Schema DSL](/kontracts/schema-dsl)
