# Core Types

Reference for fundamental types used throughout the Konditional API.

---

## `Feature<T, C, M>`

A typed configuration value with optional rules.

```kotlin
interface Feature<out T : Any, in C : Context, M : Namespace>
```

### Type Parameters

- `T` — Value type (Boolean, String, Int, Double, Enum, custom)
- `C` — Context type required for evaluation
- `M` — Namespace type this feature belongs to

### Properties

- `key: FeatureKey` — Stable identifier for this feature
- `default: T` — Default value (required, non-null)

### Methods

See [Feature Operations](/api-reference/feature-operations) for evaluation API.

---

## `Context`

Runtime inputs for evaluation.

```kotlin
interface Context {
    val locale: LocaleTag
    val platform: PlatformTag
    val appVersion: Version
    val stableId: StableId
}
```

### Standard Implementation

```kotlin
data class Context(
    override val locale: LocaleTag,
    override val platform: PlatformTag,
    override val appVersion: Version,
    override val stableId: StableId
) : Context
```

### Extending Context

```kotlin
data class EnterpriseContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    val subscriptionTier: SubscriptionTier,
    val employeeCount: Int
) : Context
```

---

## `Namespace`

Isolation boundary for features.

```kotlin
abstract class Namespace(val id: String)
```

### Properties

- `id: String` — Unique namespace identifier
- `registry: NamespaceRegistry` — Registry instance for this namespace

### Example

```kotlin
object AppFeatures : Namespace("app") {
    val darkMode by boolean<Context>(default = false)
}

sealed class TeamDomain(id: String) : Namespace(id) {
    data object Analytics : TeamDomain("analytics")
    data object Payments : TeamDomain("payments")
}
```

---

## `FlagDefinition<T, C, M>`

The compiled/effective definition of a single flag.

```kotlin
data class FlagDefinition<T : Any, C : Context, M : Namespace>(
    val key: FeatureKey,
    val default: T,
    val rules: List<Rule<C>>,
    val salt: String,
    val isActive: Boolean
)
```

### Fields

- `key` — Feature identifier
- `default` — Required default value
- `rules` — List of targeting rules (sorted by specificity)
- `salt` — Per-feature salt for bucketing
- `isActive` — Whether flag is active (inactive flags return default)

---

## `Rule<C>`

A typed mapping from criteria to a value.

```kotlin
data class Rule<C : Context>(
    val value: Any,
    val criteria: RuleCriteria<C>,
    val rampUp: RampUp = RampUp.full,
    val allowlist: Set<StableId> = emptySet(),
    val note: String? = null
)
```

### Fields

- `value` — Return value if rule matches
- `criteria` — Targeting criteria (platform/locale/version/axes/extension)
- `rampUp` — Percentage rollout (default: 100%)
- `allowlist` — StableIds that bypass ramp-up
- `note` — Optional description (for debugging/observability)

---

## `Configuration`

Immutable snapshot of flag state.

```kotlin
data class Configuration(
    val flags: Map<FeatureKey, FlagDefinition<*, *, *>>,
    val metadata: ConfigurationMetadata? = null
)
```

### Fields

- `flags` — Map of feature keys to definitions
- `metadata` — Optional metadata (version, source, timestamp)

---

## `ConfigurationMetadata`

Metadata attached to configuration snapshots.

```kotlin
data class ConfigurationMetadata(
    val version: String? = null,
    val source: String? = null,
    val generatedAtEpochMillis: Long? = null
)
```

### Fields

- `version` — Version identifier (e.g., "rev-123")
- `source` — Source identifier (e.g., "s3://configs/production.json")
- `generatedAtEpochMillis` — Timestamp in epoch milliseconds

---

## `StableId`

Stable identifier for deterministic bucketing.

```kotlin
data class StableId private constructor(val id: String) {
    companion object {
        fun of(input: String): StableId
        fun fromHex(hex: String): StableId
    }
}
```

### Factory Methods

- `StableId.of(input)` — Normalizes input to hex (lowercase via `Locale.ROOT`)
- `StableId.fromHex(hex)` — Uses pre-computed hex identifier

### Example

```kotlin
val id1 = StableId.of("user-123")
val id2 = StableId.fromHex("757365722d313233")
// id1.id == id2.id (true)
```

---

## `Version`

Semantic version for version-based targeting.

```kotlin
data class Version(
    val major: Int,
    val minor: Int,
    val patch: Int
) : Comparable<Version> {
    companion object {
        fun of(major: Int, minor: Int, patch: Int): Version
    }
}
```

### Example

```kotlin
val v = Version.of(2, 1, 0)  // 2.1.0
```

### Comparison

```kotlin
val v1 = Version.of(2, 0, 0)
val v2 = Version.of(2, 1, 0)
println(v1 < v2)  // true
```

---

## `LocaleTag` / `PlatformTag`

Stable identifiers for locale and platform targeting.

```kotlin
interface LocaleTag {
    val id: String
}

interface PlatformTag {
    val id: String
}
```

### Built-In Implementations

```kotlin
enum class AppLocale(override val id: String) : LocaleTag {
    UNITED_STATES("UNITED_STATES"),
    CANADA("CANADA"),
    FRANCE("FRANCE"),
    JAPAN("JAPAN")
}

enum class Platform(override val id: String) : PlatformTag {
    IOS("IOS"),
    ANDROID("ANDROID"),
    WEB("WEB")
}
```

### Custom Implementations

```kotlin
enum class CustomLocale(override val id: String) : LocaleTag {
    US_EAST("us-east"),
    US_WEST("us-west"),
    EU_CENTRAL("eu-central")
}
```

---

## `AxisValue<T>` / `Axis<T>`

Custom targeting dimensions.

```kotlin
interface AxisValue<T : AxisValue<T>> {
    val id: String
}

data class Axis<T : AxisValue<T>>(
    val id: String,
    val type: KClass<T>
)
```

### Example

```kotlin
enum class Environment(override val id: String) : AxisValue<Environment> {
    PROD("prod"),
    STAGE("stage"),
    DEV("dev")
}

object Axes {
    object Environment : Axis<Environment>("environment", Environment::class)
}
```

---

## `RampUp`

Percentage rollout configuration.

```kotlin
data class RampUp private constructor(val percent: Double) {
    companion object {
        val full = RampUp(100.0)
        fun of(percent: Double): RampUp
    }
}
```

### Factory Methods

- `RampUp.of(percent)` — Create ramp-up (0.0 to 100.0)
- `RampUp.full` — 100% ramp-up (constant)

---

## `FeatureKey`

Stable identifier for a feature.

```kotlin
data class FeatureKey(val value: String) {
    override fun toString(): String = value
}
```

Format: `"feature::{namespaceId}::{propertyName}"`

---

## `ParseResult<T>`

Success/failure boundary type for JSON parsing.

```kotlin
sealed interface ParseResult<out T> {
    data class Success<T>(val value: T) : ParseResult<T>
    data class Failure(val error: ParseError) : ParseResult<Nothing>
}
```

See [Serialization](/api-reference/serialization) for details.

---

## Next Steps

- [Feature Operations](/api-reference/feature-operations) — Evaluation API
- [Namespace Operations](/api-reference/namespace-operations) — Lifecycle operations
- [Fundamentals: Core Primitives](/fundamentals/core-primitives) — Conceptual overview
