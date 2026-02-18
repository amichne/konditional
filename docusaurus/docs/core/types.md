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

## `Konstrained<S>`

Use `Konstrained` when a custom feature value needs schema validation at the
JSON boundary.

```kotlin
interface Konstrained<out S : JsonSchema<*>> {
  val schema: S
}
```

`Namespace.custom<T, C>(...)` accepts any `T : Konstrained<*>`.

### Supported schema shapes

`Konstrained` supports object, primitive, and array schemas.

- **Object schemas (data classes)**: Use this for multi-field structures.
- **Primitive schemas (value classes, recommended)**: Wrap one `String`,
  `Boolean`, `Int`, or `Double` with constraints.
- **Array schemas (value classes, recommended)**: Wrap one list and validate
  each element with an element schema.

```kotlin
data class UserSettings(
    val theme: String = "light",
    val notificationsEnabled: Boolean = true,
) : Konstrained<ObjectSchema> {
    override val schema = schema {
        ::theme of { minLength = 1 }
        ::notificationsEnabled of { default = true }
    }
}

@JvmInline
value class Email(val raw: String) : Konstrained<StringSchema> {
    override val schema = stringSchema { pattern = "^[^@]+@[^@]+$" }
}

@JvmInline
value class Tags(val values: List<String>) : Konstrained<ArraySchema<String>> {
    override val schema = arraySchema { elementSchema(stringSchema { minLength = 1 }) }
}
```

### Invariants

`Konstrained` keeps type safety by enforcing these rules:

- Primitive and array schema wrappers must expose exactly one property whose
  type matches the schema backing type. Encode/decode violations fail with
  `IllegalArgumentException`.
- `schema` must be deterministic for an instance. The same instance must return
  the same schema on every call.
- External values stay untrusted until schema decoding succeeds. App code
  models values as Kotlin types instead of constructing JSON literals directly.

---

<details>
<summary>Advanced Types</summary>

## `FeatureId`

Stable serialized identifier for a feature.

```kotlin
@JvmInline
value class FeatureId private constructor(val plainId: String) {
  companion object {
    fun create(
        namespaceSeed: String,
        key: String
    ): FeatureId
    fun parse(plainId: String): FeatureId
  }
}
```

Canonical format:

```
feature::${namespaceSeed}::${key}
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

- **Boundary**: The concrete `Configuration` type lives in `konditional-serialization`.

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
interface LocaleTag {
  val id: String
}
interface PlatformTag {
  val id: String
}
```

---

## `AxisValue<T>` / `Axis<T>`

Custom targeting dimensions.
Axis ids are explicit and stable by contract; inferred/implicit axis registration is not supported.

```kotlin
interface AxisValue<T> where T : Enum<T> {
  val id: String
}
class Axis<T> private constructor(
    val id: String,
    val valueClass: KClass<out T>,
    autoRegister: Boolean = true,
) where T : AxisValue<T>, T : Enum<T>

object Axis {
  fun <T> of(id: String, valueClass: KClass<out T>): Axis<T>
}
```

---

## `RampUp`

Percentage rollout configuration.

```kotlin
@JvmInline
value class RampUp private constructor(val value: Double) : Comparable<Number>
```

</details>

---

## Next steps

- [Core DSL best practices](/core/best-practices)
- [Core API reference](/core/reference)
- [Evaluation model](/learn/evaluation-model)
- [Serialization module](/serialization/)
