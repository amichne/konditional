# Core Types

Reference for fundamental types used throughout the Konditional runtime API.

---

## `Feature<T, C, M>`

A typed feature definition created via namespace property delegation.

```kotlin
sealed interface Feature<T : Any, C : Context, out M : Namespace> : Identifiable {
    val key: String
    val namespace: M
    override val id: FeatureId
}
```

### Notes

- `key` is the in-code key (typically the Kotlin property name).
- `id` is the stable serialized identifier used in snapshots/patches (see `FeatureId` below).
- Consumers do not implement `Feature`; features are created when you declare `val foo by boolean<Context>(...) { ... }`.

See [Feature Operations](/api-reference/feature-operations) for evaluation APIs.

---

## `FeatureId`

Stable serialized identifier for a feature.

```kotlin
@JvmInline
value class FeatureId private constructor(val plainId: String) {
    companion object {
        fun create(namespaceSeed: String, key: String): FeatureId
        fun parse(plainId: String): FeatureId
    }
}
```

Canonical format:

```
feature::${namespaceSeed}::${key}
```

`namespaceSeed` defaults to the namespace `id`, but test-only namespaces use a unique seed to avoid collisions.

---

## `Context`

Runtime inputs for evaluation.

```kotlin
interface Context {
    val locale: LocaleTag
    val platform: PlatformTag
    val appVersion: Version
    val stableId: StableId

    val axisValues: AxisValues get() = AxisValues.EMPTY

    data class Core(
        override val locale: LocaleTag,
        override val platform: PlatformTag,
        override val appVersion: Version,
        override val stableId: StableId,
    ) : Context

    companion object {
        operator fun invoke(
            locale: LocaleTag,
            platform: PlatformTag,
            appVersion: Version,
            stableId: StableId,
        ): Core
    }
}
```

### Extending Context

```kotlin
data class EnterpriseContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    val subscriptionTier: SubscriptionTier,
    val employeeCount: Int,
) : Context
```

---

## `Namespace`

Isolation boundary for features and configuration.

```kotlin
open class Namespace(val id: String) : NamespaceRegistry
```

`Namespace` implements `NamespaceRegistry`, so lifecycle operations are available directly on the namespace:
`load(...)`, `rollback(...)`, `disableAll()`, `setHooks(...)`, and so on.

Example:

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

## `Configuration`

Immutable snapshot of a namespace’s flag state.

```kotlin
data class Configuration internal constructor(
    val flags: Map<Feature<*, *, *>, FlagDefinition<*, *, *>>,
    val metadata: ConfigurationMetadata = ConfigurationMetadata(),
)
```

Notes:

- `Configuration` is produced at the JSON boundary (`SnapshotSerializer.fromJson(...)`) or read from a namespace via
  `Namespace.configuration`.
- `flags` is keyed by `Feature` instances (not strings); lookups are done via `Namespace.flag(feature)`.

---

## `ConfigurationMetadata`

Operational metadata attached to configuration snapshots.

```kotlin
data class ConfigurationMetadata internal constructor(
    val version: String? = null,
    val generatedAtEpochMillis: Long? = null,
    val source: String? = null,
) {
    companion object {
        fun of(
            version: String? = null,
            generatedAtEpochMillis: Long? = null,
            source: String? = null,
        ): ConfigurationMetadata
    }
}
```

---

## `FlagDefinition<T, C, M>`

Effective definition of a single feature in a loaded configuration.

Consumers typically use this only for introspection/tooling via `Namespace.flag(feature)`.

```kotlin
data class FlagDefinition<T : Any, C : Context, M : Namespace> internal constructor(
    val defaultValue: T,
    val feature: Feature<T, C, M>,
    val isActive: Boolean = true,
    val salt: String = "v1",
)
```

---

## `StableId`

Stable identifier for deterministic bucketing.

```kotlin
sealed interface StableId {
    val id: String      // canonical hex string
    val hexId: HexId

    companion object {
        fun of(input: String): StableId
        fun fromHex(hexId: String): StableId
    }
}
```

Example:

```kotlin
val id1 = StableId.of("user-123")
val id2 = StableId.fromHex(id1.id)
```

---

## `Version`

Semantic version used for version targeting.

```kotlin
@JsonClass(generateAdapter = true)
data class Version(
    val major: Int,
    val minor: Int,
    val patch: Int,
) : Comparable<Version> {
    companion object {
        fun of(major: Int, minor: Int, patch: Int): Version
        fun parse(raw: String): ParseResult<Version>
        val default: Version
    }
}
```

---

## `LocaleTag` / `PlatformTag`

Stable identifiers for locale and platform targeting.

```kotlin
interface LocaleTag { val id: String }
interface PlatformTag { val id: String }
```

The built-in `AppLocale` and `Platform` enums use their enum names as stable ids.

---

## `AxisValue<T>` / `Axis<T>`

Custom targeting dimensions.

```kotlin
interface AxisValue<T> where T : Enum<T> {
    val id: String
}

abstract class Axis<T>(
    val id: String,
    val valueClass: KClass<T>,
) where T : AxisValue<T>, T : Enum<T>
```

Axes auto-register on initialization so rules can infer axes from value types.

---

## `RampUp`

Percentage rollout configuration.

```kotlin
@JvmInline
value class RampUp private constructor(val value: Double) : Comparable<Number> {
    companion object {
        fun of(value: Double): RampUp
        val default: RampUp
    }
}
```

---

## `ParseResult<T>` / `ParseError`

Explicit success/failure boundary (used primarily at the JSON boundary).

```kotlin
sealed interface ParseResult<out T> {
    data class Success<T>(val value: T) : ParseResult<T>
    data class Failure(val error: ParseError) : ParseResult<Nothing>
}
```

See [Serialization](/api-reference/serialization) for parsing/validation behavior and error types.

---

## Next Steps

- [Feature Operations](/api-reference/feature-operations) — Evaluation API
- [Namespace Operations](/api-reference/namespace-operations) — Lifecycle operations
- [Serialization](/api-reference/serialization) — JSON snapshot/patch operations
- [Fundamentals: Core Primitives](/fundamentals/core-primitives) — Conceptual overview
