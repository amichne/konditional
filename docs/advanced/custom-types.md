---
title: Custom Types
description: Use custom value types with Konditional feature flags
---

# Custom Types

While Konditional provides built-in support for common types (Boolean, String, Int, Double), you can also use custom types for your feature flags. This enables:

- **Complex configurations**: Use rich domain objects as flag values
- **Type safety**: Leverage Kotlin's type system for your custom types
- **Serialization**: Integrate custom types with Konditional's serialization system

## Defining Custom Types

### Simple Custom Type

```kotlin
data class ThemeConfig(
    val primaryColor: String,
    val secondaryColor: String,
    val darkMode: Boolean
)

val themeFlag = Flag(
    id = "app-theme",
    defaultValue = ThemeConfig(
        primaryColor = "#007AFF",
        secondaryColor = "#5856D6",
        darkMode = false
    )
)
```

### Using the Flag

```kotlin
val theme = themeFlag.evaluate(context)
println("Primary color: ${theme.primaryColor}")
```

## Serialization Support

To use custom types with serialization, register a `ValueType` converter:

### Step 1: Define Your Type

```kotlin
data class FeatureConfig(
    val maxUsers: Int,
    val allowedRegions: List<String>,
    val experimentalFeatures: Set<String>
)
```

### Step 2: Create a ValueType

```kotlin
import io.amichne.konditional.core.ValueType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SerializableFeatureConfig(
    val maxUsers: Int,
    val allowedRegions: List<String>,
    val experimentalFeatures: Set<String>
)

object FeatureConfigType : ValueType<FeatureConfig> {
    override val name = "FeatureConfig"

    override fun serialize(value: FeatureConfig): String {
        val serializable = SerializableFeatureConfig(
            maxUsers = value.maxUsers,
            allowedRegions = value.allowedRegions,
            experimentalFeatures = value.experimentalFeatures
        )
        return Json.encodeToString(
            SerializableFeatureConfig.serializer(),
            serializable
        )
    }

    override fun deserialize(value: String): FeatureConfig {
        val serializable = Json.decodeFromString(
            SerializableFeatureConfig.serializer(),
            value
        )
        return FeatureConfig(
            maxUsers = serializable.maxUsers,
            allowedRegions = serializable.allowedRegions,
            experimentalFeatures = serializable.experimentalFeatures
        )
    }
}
```

### Step 3: Register the Type

```kotlin
import io.amichne.konditional.serialization.SnapshotSerializer

val serializer = SnapshotSerializer(
    customTypes = mapOf(
        "FeatureConfig" to FeatureConfigType
    )
)
```

### Step 4: Use with Flags

```kotlin
val featureFlag = Flag(
    id = "feature-config",
    defaultValue = FeatureConfig(
        maxUsers = 1000,
        allowedRegions = listOf("US", "EU"),
        experimentalFeatures = setOf()
    )
)

// Register flag
serializer.register(featureFlag)

// Serialize
val snapshot = serializer.serialize()

// Deserialize
val loaded = serializer.deserialize(snapshot)
```

## Enum Types

Enums work naturally with Konditional:

```kotlin
enum class LogLevel {
    DEBUG, INFO, WARN, ERROR
}

val logLevelFlag = Flag(
    id = "log-level",
    defaultValue = LogLevel.INFO
)

// With rules
val logLevelWithRules = Flag(
    id = "log-level",
    defaultValue = LogLevel.INFO,
    rules = listOf(
        Rule(
            condition = EnvironmentCondition("development"),
            value = LogLevel.DEBUG
        ),
        Rule(
            condition = EnvironmentCondition("production"),
            value = LogLevel.WARN
        )
    )
)
```

### Enum Serialization

```kotlin
object LogLevelType : ValueType<LogLevel> {
    override val name = "LogLevel"

    override fun serialize(value: LogLevel): String = value.name

    override fun deserialize(value: String): LogLevel =
        LogLevel.valueOf(value)
}
```

## Sealed Classes

Sealed classes provide type-safe polymorphism:

```kotlin
sealed class PaymentMethod {
    data class CreditCard(val last4: String) : PaymentMethod()
    data class PayPal(val email: String) : PaymentMethod()
    object Cash : PaymentMethod()
}

val paymentFlag = Flag(
    id = "default-payment-method",
    defaultValue = PaymentMethod.Cash as PaymentMethod
)
```

### Sealed Class Serialization

```kotlin
import kotlinx.serialization.json.*

object PaymentMethodType : ValueType<PaymentMethod> {
    override val name = "PaymentMethod"

    override fun serialize(value: PaymentMethod): String {
        val json = when (value) {
            is PaymentMethod.CreditCard -> buildJsonObject {
                put("type", "CreditCard")
                put("last4", value.last4)
            }
            is PaymentMethod.PayPal -> buildJsonObject {
                put("type", "PayPal")
                put("email", value.email)
            }
            PaymentMethod.Cash -> buildJsonObject {
                put("type", "Cash")
            }
        }
        return json.toString()
    }

    override fun deserialize(value: String): PaymentMethod {
        val json = Json.parseToJsonElement(value).jsonObject
        return when (json["type"]?.jsonPrimitive?.content) {
            "CreditCard" -> PaymentMethod.CreditCard(
                json["last4"]!!.jsonPrimitive.content
            )
            "PayPal" -> PaymentMethod.PayPal(
                json["email"]!!.jsonPrimitive.content
            )
            "Cash" -> PaymentMethod.Cash
            else -> throw IllegalArgumentException("Unknown payment method")
        }
    }
}
```

## Collections

Use collections as flag values:

```kotlin
// List of strings
val allowedCountriesFlag = Flag(
    id = "allowed-countries",
    defaultValue = listOf("US", "CA", "UK")
)

// Map
val featureLimitsFlag = Flag(
    id = "feature-limits",
    defaultValue = mapOf(
        "free" to 10,
        "premium" to 100,
        "enterprise" to -1
    )
)

// Set
val enabledFeaturesFlag = Flag(
    id = "enabled-features",
    defaultValue = setOf("feature-a", "feature-b")
)
```

## Best Practices

### Keep Types Simple

Prefer simple, serializable types:

```kotlin
// Good
data class Config(
    val enabled: Boolean,
    val timeout: Int,
    val endpoints: List<String>
)

// Avoid complex hierarchies
data class OverlyComplex(
    val nested: Level1,
    val callbacks: List<() -> Unit>, // Not serializable
    val state: MutableState<Int> // Mutable state
)
```

### Use Immutable Types

```kotlin
// Good - immutable
data class FeatureSettings(
    val maxRetries: Int,
    val timeout: Duration
)

// Avoid - mutable
data class MutableSettings(
    var maxRetries: Int,
    var timeout: Duration
)
```

### Document Type Contracts

```kotlin
/**
 * Configuration for the recommendation engine.
 *
 * @property algorithm The algorithm to use ("collaborative", "content", "hybrid")
 * @property maxResults Maximum number of results to return (1-100)
 * @property minScore Minimum confidence score (0.0-1.0)
 */
data class RecommendationConfig(
    val algorithm: String,
    val maxResults: Int,
    val minScore: Double
) {
    init {
        require(algorithm in setOf("collaborative", "content", "hybrid"))
        require(maxResults in 1..100)
        require(minScore in 0.0..1.0)
    }
}
```

### Version Your Types

When types evolve, handle versioning:

```kotlin
sealed class ConfigVersion {
    data class V1(val timeout: Int) : ConfigVersion()
    data class V2(
        val timeout: Int,
        val retries: Int
    ) : ConfigVersion()
}

object ConfigVersionType : ValueType<ConfigVersion> {
    override fun deserialize(value: String): ConfigVersion {
        val json = Json.parseToJsonElement(value).jsonObject
        return when (json["version"]?.jsonPrimitive?.int) {
            1 -> ConfigVersion.V1(
                timeout = json["timeout"]!!.jsonPrimitive.int
            )
            2 -> ConfigVersion.V2(
                timeout = json["timeout"]!!.jsonPrimitive.int,
                retries = json["retries"]!!.jsonPrimitive.int
            )
            else -> throw IllegalArgumentException("Unknown version")
        }
    }
}
```

## Next Steps

- Learn about [Migration](/migration/) strategies for evolving flag configurations
- Explore [Patch Updates](/patch-updates/) for dynamic configuration changes
- Review the [Serialization API](../serialization/api/) for complete details
