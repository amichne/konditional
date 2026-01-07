# Core Types

Reference for fundamental types in `konditional-core`.

## `Feature<T, C, M>`

A typed feature definition created via namespace property delegation.

```kotlin
sealed interface Feature<T : Any, C : Context, out M : Namespace> : Identifiable {
    val key: String
    val namespace: M
    override val id: FeatureId
}
```

Notes:
- `key` is the in-code key (usually the Kotlin property name).
- `id` is the stable serialized identifier (see `FeatureId`).

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

    data class Core(...) : Context

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

### Extending context

```kotlin
data class EnterpriseContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    val subscriptionTier: SubscriptionTier,
) : Context
```

---

## `Namespace`

Isolation boundary for features and configuration.

```kotlin
open class Namespace(val id: String) : NamespaceRegistry
```

---

## `ConfigurationView` / `ConfigurationMetadataView`

Read-only views over the active configuration snapshot.

```kotlin
interface ConfigurationView {
    val flags: Map<Feature<*, *, *>, FlagDefinition<*, *, *>>
    val metadata: ConfigurationMetadataView
}

interface ConfigurationMetadataView {
    val version: String?
    val generatedAtEpochMillis: Long?
    val source: String?
}
```

Use `Namespace.flag(feature)` to resolve a single feature definition from the active configuration.

**Boundary**: The concrete `Configuration` type lives in `konditional-serialization`.

---

## `StableId`

Stable identifier used for deterministic bucketing.

```kotlin
sealed interface StableId {
    val id: String
    val hexId: HexId

    companion object {
        fun of(input: String): StableId
        fun fromHex(hexId: String): StableId
    }
}
```

---

## `Version`

Semantic version used for version targeting.

```kotlin
data class Version(
    val major: Int,
    val minor: Int,
    val patch: Int,
) : Comparable<Version>
```

---

## `LocaleTag` / `PlatformTag`

Stable identifiers for locale and platform targeting.

```kotlin
interface LocaleTag { val id: String }
interface PlatformTag { val id: String }
```

---

## `AxisValue<T>` / `Axis<T>`

Custom targeting dimensions.

```kotlin
interface AxisValue<T> where T : Enum<T> { val id: String }
abstract class Axis<T>(val id: String, val valueClass: KClass<T>) where T : AxisValue<T>, T : Enum<T>
```

---

## `RampUp`

Percentage rollout configuration.

```kotlin
@JvmInline
value class RampUp private constructor(val value: Double) : Comparable<Number>
```

---

## Next steps

- [Core API reference](/core/reference)
- [Evaluation model](/fundamentals/evaluation-semantics)
- [Serialization module](/serialization/index)
