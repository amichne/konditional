# Kontracts Schema DSL

## Type-inferred DSL

```kotlin
import io.amichne.kontracts.dsl.*

val schema = schemaRoot {
    ::theme of {
        minLength = 1
        maxLength = 50
        description = "UI theme preference"
        enum = listOf("light", "dark", "auto")
    }

    ::notificationsEnabled of {
        default = true
        description = "Enable push notifications"
    }

    ::maxRetries of {
        minimum = 0
        maximum = 10
    }
}
```

## Custom type mapping

```kotlin
data class UserId(val value: String)

data class UserConfig(
    val userId: UserId,
) {
    companion object {
        val schema = schemaRoot {
            ::userId asString {
                represent = { this.value }
                pattern = "[A-Z0-9]{8}"
            }
        }
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

- [Konditional serialization](/serialization/index)
